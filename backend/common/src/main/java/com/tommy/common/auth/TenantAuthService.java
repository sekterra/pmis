package com.tommy.common.auth;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tommy.common.code.CodeRules;
import com.tommy.common.global.exception.ApiException;
import com.tommy.common.global.multitenancy.TenantContext;
import com.tommy.common.global.response.FieldErrorItem;
import com.tommy.common.global.security.AccountStatusCache;
import com.tommy.common.global.security.JwtTokenService;
import com.tommy.common.global.security.MdmSecurityProperties;
import com.tommy.common.global.security.PasswordHasher;
import com.tommy.common.global.time.TenantClock;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Service
public class TenantAuthService {

	private final EntityManager entityManager;

	private final PasswordHasher passwordHasher;

	private final JwtTokenService tokens;

	private final MdmSecurityProperties properties;

	private final AccountStatusCache accountStatusCache;

	private final TenantClock clock;

	private final String dummyHash;

	public TenantAuthService(EntityManager entityManager, PasswordHasher passwordHasher, JwtTokenService tokens,
			MdmSecurityProperties properties, AccountStatusCache accountStatusCache, TenantClock clock) {
		this.entityManager = entityManager;
		this.passwordHasher = passwordHasher;
		this.tokens = tokens;
		this.properties = properties;
		this.accountStatusCache = accountStatusCache;
		this.clock = clock;
		this.dummyHash = passwordHasher.hash(UUID.randomUUID().toString());
	}

	@Transactional(noRollbackFor = ApiException.class)
	public AuthSession login(String loginId, String password, HttpServletRequest request, HttpServletResponse response) {
		String id = loginId == null ? "" : loginId.trim().toLowerCase(Locale.ROOT);
		Policy policy = policy();
		String ip = ClientAddresses.ip(request, properties.isTrustForwardedHeaders());
		String agent = ClientAddresses.userAgent(request);
		Object[] row = one("""
				SELECT user_pk, password_hash, user_status_code, use_yn, login_fail_count, locked_datetime,
				       must_change_password_yn, password_expire_datetime
				FROM user_account WHERE login_id = ?
				""", id);
		if (row == null) {
			rejectUnknown(password);
			history(null, id, "FAIL_NOT_FOUND", ip, agent);
			throw invalid();
		}
		long userPk = longValue(row[0]);
		String hash = text(row[1]);
		String status = text(row[2]);
		String useYn = text(row[3]);
		int failCount = intValue(row[4]);
		Instant lockedAt = instant(row[5]);
		boolean active = "ACTIVE".equals(status) || "LOCKED".equals(status);
		if (!"Y".equals(useYn) || !active) {
			if (hash == null) {
				passwordHasher.matches(password, dummyHash);
			}
			else {
				passwordHasher.matches(password, hash);
			}
			history(userPk, id, "FAIL_INACTIVE", ip, agent);
			throw invalid();
		}
		boolean passwordOk = hash != null && passwordHasher.matches(password, hash);
		if (hash == null) {
			passwordHasher.matches(password, dummyHash);
		}
		boolean locked = "LOCKED".equals(status);
		boolean lockExpired = LoginLockRules.expired(lockedAt, policy.lockMinutes(), Instant.now());
		LoginLockRules.Action action = LoginLockRules.decide(locked, lockExpired, passwordOk);
		if (action == LoginLockRules.Action.FAIL_LOCKED) {
			history(userPk, id, "FAIL_LOCKED", ip, agent);
			throw invalid();
		}
		if (action == LoginLockRules.Action.FAIL_INCREMENT || action == LoginLockRules.Action.FAIL_RESTART) {
			boolean becameLocked = applyFailure(userPk, action, policy.maxFail());
			if (becameLocked) {
				revokeUser(userPk, "ADMIN");
				accountStatusCache.evict(TenantContext.schema(), userPk);
			}
			history(userPk, id, becameLocked ? "FAIL_LOCKED" : "FAIL_PASSWORD", ip, agent);
			throw invalid();
		}
		entityManager.createNativeQuery("""
				UPDATE user_account
				SET login_fail_count = 0, last_login_datetime = CURRENT_TIMESTAMP,
				    user_status_code = 'ACTIVE', locked_datetime = NULL
				WHERE user_pk = ?
				""").setParameter(1, userPk).executeUpdate();
		accountStatusCache.evict(TenantContext.schema(), userPk);
		history(userPk, id, "SUCCESS", ip, agent);
		boolean changeRequired = changeRequired(text(row[6]), instant(row[7]));
		return issue(userPk, id, changeRequired, policy, request, response, true);
	}

	@Transactional(noRollbackFor = ApiException.class)
	public AuthSession refresh(HttpServletRequest request, HttpServletResponse response) {
		requireCsrf(request);
		String raw = cookie(request);
		if (raw == null || raw.isBlank()) {
			throw unauthorized();
		}
		Policy policy = policy();
		Object[] token = one("""
				SELECT refresh_token_pk, user_pk, token_family_id, expire_datetime, revoked_datetime, revoke_reason_code
				FROM user_refresh_token WHERE token_hash = ? FOR UPDATE
				""", RefreshTokenValues.hash(raw));
		if (token == null) {
			throw unauthorized();
		}
		Instant expire = instant(token[3]);
		if (expire == null || !expire.isAfter(Instant.now())) {
			throw new ApiException(401, "AUTH.TOKEN_EXPIRED", "토큰이 만료되었습니다.", 2);
		}
		Instant revokedAt = instant(token[4]);
		if (revokedAt != null) {
			RefreshReuseRules.Decision decision = RefreshReuseRules.decide(text(token[5]), revokedAt, Instant.now(),
					properties.getRefreshReuseGraceSeconds());
			if (decision == RefreshReuseRules.Decision.IN_PROGRESS) {
				throw new ApiException(409, "AUTH.REFRESH_IN_PROGRESS", "토큰 갱신이 진행 중입니다.", 2);
			}
			revokeFamily(text(token[2]), "REUSE_DETECTED");
			throw new ApiException(401, "AUTH.TOKEN_REUSED", "다시 사용된 토큰입니다.", 2);
		}
		long userPk = longValue(token[1]);
		Object[] account = requireAccount(userPk);
		if (!usable(account)) {
			revokeFamily(text(token[2]), "ADMIN");
			accountStatusCache.evict(TenantContext.schema(), userPk);
			throw new ApiException(401, "AUTH.ACCOUNT_DISABLED", "사용할 수 없는 계정입니다.", 2);
		}
		entityManager.createNativeQuery("""
				UPDATE user_refresh_token
				SET revoked_datetime = CURRENT_TIMESTAMP, revoke_reason_code = 'ROTATED'
				WHERE refresh_token_pk = ?
				""").setParameter(1, longValue(token[0])).executeUpdate();
		boolean changeRequired = changeRequired(text(account[1]), instant(account[2]));
		return issue(userPk, text(account[3]), changeRequired, policy, request, response, false, text(token[2]));
	}

	@Transactional(noRollbackFor = ApiException.class)
	public void logout(HttpServletRequest request, HttpServletResponse response) {
		requireCsrf(request);
		String raw = cookie(request);
		if (raw != null && !raw.isBlank()) {
			Object[] token = one("SELECT user_pk, revoked_datetime FROM user_refresh_token WHERE token_hash = ?",
					RefreshTokenValues.hash(raw));
			if (token != null && instant(token[1]) == null && longValue(token[0]) == currentUserPk()) {
				entityManager.createNativeQuery("""
						UPDATE user_refresh_token
						SET revoked_datetime = CURRENT_TIMESTAMP, revoke_reason_code = 'LOGOUT'
						WHERE token_hash = ? AND revoked_datetime IS NULL
						""").setParameter(1, RefreshTokenValues.hash(raw)).executeUpdate();
			}
		}
		RefreshCookies.clear(response, properties.getCookie().isSecure());
	}

	@Transactional(noRollbackFor = ApiException.class)
	public AuthSession changePassword(String currentPassword, String newPassword, HttpServletRequest request,
			HttpServletResponse response) {
		long userPk = currentUserPk();
		Policy policy = policy();
		Object[] account = requireAccount(userPk);
		String hash = text(account[4]);
		String loginId = text(account[3]);
		if (hash == null || !passwordHasher.matches(currentPassword, hash)) {
			throw new ApiException(400, "AUTH.PASSWORD_CURRENT", "현재 비밀번호가 올바르지 않습니다.", 2);
		}
		if (passwordHasher.matches(newPassword, hash)) {
			throw new ApiException(400, "AUTH.PASSWORD_SAME", "새 비밀번호는 현재 비밀번호와 달라야 합니다.", 2);
		}
		List<String> violations = PasswordPolicy.violations(newPassword, loginId, policy.minLength(), policy.minClasses());
		if (!violations.isEmpty()) {
			throw new ApiException(400, "AUTH.PASSWORD_POLICY", "비밀번호 정책에 맞지 않습니다.", 2,
					violations.stream().map(reason -> new FieldErrorItem("newPassword", reason)).toList());
		}
		if (reused(userPk, newPassword, policy.historyCount())) {
			throw new ApiException(400, "AUTH.PASSWORD_REUSED", "최근에 사용한 비밀번호입니다.", 2);
		}
		String nextHash = passwordHasher.hash(newPassword);
		boolean expired = changeRequired("N", instant(account[2]));
		entityManager.createNativeQuery("""
				UPDATE user_account
				SET password_hash = ?, must_change_password_yn = 'N',
				    password_changed_datetime = CURRENT_TIMESTAMP,
				    password_expire_datetime = CURRENT_TIMESTAMP + (? * INTERVAL '1 day')
				WHERE user_pk = ?
				""").setParameter(1, nextHash).setParameter(2, policy.expireDays()).setParameter(3, userPk).executeUpdate();
		entityManager.createNativeQuery("""
				INSERT INTO user_password_history (user_pk, password_hash, change_type_code, changed_datetime, changed_user_pk)
				VALUES (?, ?, ?, CURRENT_TIMESTAMP, ?)
				""").setParameter(1, userPk).setParameter(2, nextHash).setParameter(3, expired ? "EXPIRED_CHANGE" : "CHANGE")
				.setParameter(4, userPk).executeUpdate();
		revokeUser(userPk, "PASSWORD_CHANGED");
		accountStatusCache.evict(TenantContext.schema(), userPk);
		return issue(userPk, loginId, false, policy, request, response, true);
	}

	@Transactional(readOnly = true)
	public Me me() {
		long userPk = currentUserPk();
		Object[] account = one("""
				SELECT a.login_id, a.support_language_code, a.timezone_id, a.must_change_password_yn,
				       a.password_expire_datetime, p.person_name
				FROM user_account a
				LEFT JOIN person p ON p.person_pk = a.person_pk
				WHERE a.user_pk = ?
				""", userPk);
		if (account == null) {
			throw unauthorized();
		}
		@SuppressWarnings("unchecked")
		List<String> roles = entityManager.createNativeQuery("""
				SELECT r.role_code
				FROM user_role ur
				JOIN role r ON r.role_pk = ur.role_pk AND r.use_yn = 'Y'
				WHERE ur.user_pk = ? AND ur.use_yn = 'Y'
				  AND (ur.valid_from_date IS NULL OR ur.valid_from_date <= ?)
				  AND (ur.valid_to_date IS NULL OR ur.valid_to_date >= ?)
				ORDER BY r.role_code
				""").setParameter(1, userPk).setParameter(2, clock.today()).setParameter(3, clock.today()).getResultList();
		String loginId = text(account[0]);
		String name = text(account[5]);
		return new Me(userPk, loginId, name == null ? loginId : name, text(account[1]), text(account[2]),
				roles.stream().map(TenantAuthService::text).toList(), changeRequired(text(account[3]), instant(account[4])));
	}

	private AuthSession issue(long userPk, String loginId, boolean changeRequired, Policy policy, HttpServletRequest request,
			HttpServletResponse response, boolean newFamily) {
		return issue(userPk, loginId, changeRequired, policy, request, response, newFamily, null);
	}

	private AuthSession issue(long userPk, String loginId, boolean changeRequired, Policy policy, HttpServletRequest request,
			HttpServletResponse response, boolean newFamily, String familyId) {
		String tenant = tenantCode();
		String raw = RefreshTokenValues.raw();
		String family = newFamily || familyId == null ? UUID.randomUUID().toString() : familyId;
		String ip = ClientAddresses.ip(request, properties.isTrustForwardedHeaders());
		String agent = ClientAddresses.userAgent(request);
		entityManager.createNativeQuery("""
				INSERT INTO user_refresh_token (
				    user_pk, token_hash, token_family_id, issued_datetime, expire_datetime, ip_address, user_agent)
				VALUES (?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + (? * INTERVAL '1 day'), ?, ?)
				""").setParameter(1, userPk).setParameter(2, RefreshTokenValues.hash(raw)).setParameter(3, family)
				.setParameter(4, policy.refreshDays()).setParameter(5, ip == null ? "" : ip).setParameter(6, agent == null ? "" : agent)
				.executeUpdate();
		RefreshCookies.set(response, raw, Duration.ofDays(policy.refreshDays()), properties.getCookie().isSecure());
		String access = tokens.issue(Long.toString(userPk), JwtTokenService.TENANT_AUDIENCE, JwtTokenService.TENANT_TYPE, tenant,
				changeRequired, Duration.ofMinutes(policy.accessMinutes()));
		return new AuthSession(access, policy.accessMinutes() * 60L, changeRequired);
	}

	private boolean applyFailure(long userPk, LoginLockRules.Action action, int maxFail) {
		if (action == LoginLockRules.Action.FAIL_RESTART) {
			entityManager.createNativeQuery("""
					UPDATE user_account
					SET login_fail_count = 1,
					    user_status_code = CASE WHEN ? <= 1 THEN 'LOCKED' ELSE 'ACTIVE' END,
					    locked_datetime = CASE WHEN ? <= 1 THEN CURRENT_TIMESTAMP ELSE NULL END
					WHERE user_pk = ?
					""").setParameter(1, maxFail).setParameter(2, maxFail).setParameter(3, userPk).executeUpdate();
			return maxFail <= 1;
		}
		entityManager.createNativeQuery("""
				UPDATE user_account
				SET login_fail_count = login_fail_count + 1,
				    user_status_code = CASE WHEN login_fail_count + 1 >= ? THEN 'LOCKED' ELSE user_status_code END,
				    locked_datetime = CASE WHEN login_fail_count + 1 >= ? THEN CURRENT_TIMESTAMP ELSE locked_datetime END
				WHERE user_pk = ?
				""").setParameter(1, maxFail).setParameter(2, maxFail).setParameter(3, userPk).executeUpdate();
		return "LOCKED".equals(text(one("SELECT user_status_code FROM user_account WHERE user_pk = ?", userPk)[0]));
	}

	private boolean reused(long userPk, String password, int count) {
		@SuppressWarnings("unchecked")
		List<String> hashes = entityManager.createNativeQuery("""
				SELECT password_hash FROM user_password_history
				WHERE user_pk = ?
				ORDER BY changed_datetime DESC, password_history_pk DESC
				LIMIT ?
				""").setParameter(1, userPk).setParameter(2, count).getResultList();
		for (String hash : hashes) {
			if (passwordHasher.matches(password, hash)) {
				return true;
			}
		}
		return false;
	}

	private void revokeUser(long userPk, String reason) {
		entityManager.createNativeQuery("""
				UPDATE user_refresh_token
				SET revoked_datetime = CURRENT_TIMESTAMP, revoke_reason_code = ?
				WHERE user_pk = ? AND revoked_datetime IS NULL
				""").setParameter(1, reason).setParameter(2, userPk).executeUpdate();
	}

	private void revokeFamily(String familyId, String reason) {
		entityManager.createNativeQuery("""
				UPDATE user_refresh_token
				SET revoked_datetime = COALESCE(revoked_datetime, CURRENT_TIMESTAMP), revoke_reason_code = ?
				WHERE token_family_id = ?
				""").setParameter(1, reason).setParameter(2, familyId).executeUpdate();
	}

	private Policy policy() {
		@SuppressWarnings("unchecked")
		List<Object[]> rows = entityManager.createNativeQuery("""
				SELECT c.code_value, c.ext_attr1
				FROM common_code c
				JOIN code_group g ON g.code_group_pk = c.code_group_pk
				WHERE g.code_group_code = 'SECURITY_POLICY' AND c.use_yn = 'Y'
				""").getResultList();
		java.util.Map<String, String> values = new java.util.HashMap<>();
		for (Object[] row : rows) {
			values.put(text(row[0]), text(row[1]));
		}
		return new Policy(number("PASSWORD_EXPIRE_DAYS", values), number("PASSWORD_MIN_LENGTH", values),
				number("PASSWORD_MIN_CLASSES", values), number("PASSWORD_HISTORY_COUNT", values),
				number("MAX_LOGIN_FAIL_COUNT", values), number("LOCK_MINUTES", values),
				number("ACCESS_TOKEN_MINUTES", values), number("REFRESH_TOKEN_DAYS", values));
	}

	private static int number(String code, java.util.Map<String, String> values) {
		String raw = values.get(code);
		if (!CodeRules.validPolicy(code, raw)) {
			throw new ApiException(500, "TENANT.SECURITY_POLICY_MISSING", "보안 정책이 없습니다.", 2);
		}
		return Integer.parseInt(raw.trim());
	}

	private Object[] requireAccount(long userPk) {
		Object[] account = one("""
				SELECT user_status_code, must_change_password_yn, password_expire_datetime, login_id, password_hash, use_yn
				FROM user_account WHERE user_pk = ?
				""", userPk);
		if (account == null) {
			throw unauthorized();
		}
		return account;
	}

	private static boolean usable(Object[] account) {
		return "ACTIVE".equals(text(account[0])) && "Y".equals(text(account[5]));
	}

	private static boolean changeRequired(String mustChange, Instant expire) {
		return "Y".equals(mustChange) || (expire != null && !expire.isAfter(Instant.now()));
	}

	private void history(Long userPk, String loginId, String result, String ip, String agent) {
		var query = entityManager.createNativeQuery("""
				INSERT INTO user_login_history (user_pk, login_id_input, login_result_code, login_datetime, ip_address, user_agent)
				VALUES (?, ?, ?, CURRENT_TIMESTAMP, ?, ?)
				""");
		query.setParameter(1, userPk);
		query.setParameter(2, loginId);
		query.setParameter(3, result);
		query.setParameter(4, ip == null ? "" : ip);
		query.setParameter(5, agent == null ? "" : agent);
		query.executeUpdate();
	}

	private Object[] one(String sql, Object... parameters) {
		try {
			var query = entityManager.createNativeQuery(sql);
			for (int index = 0; index < parameters.length; index++) {
				query.setParameter(index + 1, parameters[index]);
			}
			Object value = query.getSingleResult();
			if (value instanceof Object[] row) {
				return row;
			}
			return new Object[] { value };
		}
		catch (NoResultException ex) {
			return null;
		}
	}

	private void requireCsrf(HttpServletRequest request) {
		if (!"1".equals(request.getHeader("X-MDM-Request"))) {
			throw new ApiException(403, "AUTH.CSRF", "요청을 확인할 수 없습니다.", 2);
		}
	}

	private static String cookie(HttpServletRequest request) {
		Cookie[] cookies = request.getCookies();
		if (cookies == null) {
			return null;
		}
		for (Cookie cookie : cookies) {
			if (RefreshCookies.NAME.equals(cookie.getName())) {
				return cookie.getValue();
			}
		}
		return null;
	}

	private String tenantCode() {
		String schema = TenantContext.schema();
		Object[] value = one("SELECT tenant_code FROM pmis.tenant WHERE schema_name = ?", schema);
		if (value == null || value[0] == null) {
			throw new ApiException(400, "TENANT.UNRESOLVED", "테넌트를 식별할 수 없습니다.", 2);
		}
		return text(value[0]);
	}

	private static long currentUserPk() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
			throw unauthorized();
		}
		return Long.parseLong(jwt.getSubject());
	}

	void rejectUnknown(String password) {
		passwordHasher.matches(password, dummyHash);
	}

	private static ApiException invalid() {
		return new ApiException(401, "AUTH.INVALID_CREDENTIALS", "로그인에 실패했습니다.", 2);
	}

	private static ApiException unauthorized() {
		return new ApiException(401, "AUTH.UNAUTHORIZED", "인증이 필요합니다.", 2);
	}

	private static String text(Object value) {
		if (value == null) {
			return null;
		}
		String text = String.valueOf(value).trim();
		return text.isEmpty() ? null : text;
	}

	private static long longValue(Object value) {
		return ((Number) value).longValue();
	}

	private static int intValue(Object value) {
		return value == null ? 0 : ((Number) value).intValue();
	}

	private static Instant instant(Object value) {
		if (value == null) {
			return null;
		}
		if (value instanceof Instant instant) {
			return instant;
		}
		if (value instanceof java.time.LocalDateTime local) {
			return local.toInstant(java.time.ZoneOffset.UTC);
		}
		if (value instanceof java.sql.Timestamp timestamp) {
			return timestamp.toInstant();
		}
		if (value instanceof java.util.Date date) {
			return date.toInstant();
		}
		if (value instanceof java.time.OffsetDateTime offset) {
			return offset.toInstant();
		}
		return null;
	}

	private record Policy(int expireDays, int minLength, int minClasses, int historyCount, int maxFail, int lockMinutes,
			int accessMinutes, int refreshDays) {
	}

	public record AuthSession(String accessToken, long expiresIn, boolean passwordChangeRequired) {
	}

	public record Me(long userPk, String loginId, String displayName, String supportLanguageCode, String timezoneId,
			List<String> roleCodes, boolean passwordChangeRequired) {
	}

}
