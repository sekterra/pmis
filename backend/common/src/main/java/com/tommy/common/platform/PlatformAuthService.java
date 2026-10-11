package com.tommy.common.platform;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tommy.common.auth.ClientAddresses;
import com.tommy.common.auth.LoginLockRules;
import com.tommy.common.auth.PasswordPolicy;
import com.tommy.common.auth.RefreshCookies;
import com.tommy.common.auth.RefreshReuseRules;
import com.tommy.common.auth.RefreshTokenValues;
import com.tommy.common.global.exception.ApiException;
import com.tommy.common.global.response.FieldErrorItem;
import com.tommy.common.global.security.JwtTokenService;
import com.tommy.common.global.security.MdmSecurityProperties;
import com.tommy.common.global.security.PasswordHasher;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Service
public class PlatformAuthService {

	private final JdbcTemplate jdbcTemplate;

	private final PasswordHasher passwordHasher;

	private final JwtTokenService tokens;

	private final MdmSecurityProperties properties;

	private final PlatformAccountStatus accountStatus;

	private final String dummyHash;

	public PlatformAuthService(JdbcTemplate jdbcTemplate, PasswordHasher passwordHasher, JwtTokenService tokens,
			MdmSecurityProperties properties, PlatformAccountStatus accountStatus) {
		this.jdbcTemplate = jdbcTemplate;
		this.passwordHasher = passwordHasher;
		this.tokens = tokens;
		this.properties = properties;
		this.accountStatus = accountStatus;
		this.dummyHash = passwordHasher.hash(UUID.randomUUID().toString());
	}

	@Transactional(noRollbackFor = ApiException.class)
	public AuthSession login(String loginId, String password, HttpServletRequest request, HttpServletResponse response) {
		String id = loginId == null ? "" : loginId.trim().toLowerCase(Locale.ROOT);
		MdmSecurityProperties.Platform policy = properties.getPlatform();
		String ip = ClientAddresses.ip(request, properties.isTrustForwardedHeaders());
		String agent = ClientAddresses.userAgent(request);
		List<AccountRow> rows = jdbcTemplate.query("""
				SELECT platform_admin_pk, password_hash, admin_status_code, use_yn, login_fail_count, locked_datetime,
				       must_change_password_yn, password_expire_datetime
				FROM pmis.platform_admin WHERE login_id = ?
				""", (resultSet, row) -> new AccountRow(resultSet.getLong(1), resultSet.getString(2), resultSet.getString(3),
				trim(resultSet.getString(4)), resultSet.getInt(5), instant(resultSet.getObject(6)), trim(resultSet.getString(7)),
				instant(resultSet.getObject(8))), id);
		if (rows.isEmpty()) {
			passwordHasher.matches(password, dummyHash);
			history(null, id, "FAIL_NOT_FOUND", ip, agent);
			throw invalid(request);
		}
		AccountRow account = rows.get(0);
		boolean usableStatus = "ACTIVE".equals(account.status()) || "LOCKED".equals(account.status());
		if (!"Y".equals(account.useYn()) || !usableStatus) {
			passwordHasher.matches(password, account.hash() == null ? dummyHash : account.hash());
			history(account.pk(), id, "FAIL_INACTIVE", ip, agent);
			throw invalid(request);
		}
		boolean passwordOk = account.hash() != null && passwordHasher.matches(password, account.hash());
		if (account.hash() == null) {
			passwordHasher.matches(password, dummyHash);
		}
		boolean locked = "LOCKED".equals(account.status());
		boolean lockExpired = LoginLockRules.expired(account.lockedAt(), policy.getLockMinutes(), Instant.now());
		LoginLockRules.Action action = LoginLockRules.decide(locked, lockExpired, passwordOk);
		if (action == LoginLockRules.Action.FAIL_LOCKED) {
			history(account.pk(), id, "FAIL_LOCKED", ip, agent);
			throw invalid(request);
		}
		if (action == LoginLockRules.Action.FAIL_INCREMENT || action == LoginLockRules.Action.FAIL_RESTART) {
			boolean becameLocked = applyFailure(account.pk(), action, policy.getMaxLoginFailCount());
			if (becameLocked) {
				revokeUser(account.pk(), "ADMIN");
				accountStatus.evict(account.pk());
			}
			history(account.pk(), id, becameLocked ? "FAIL_LOCKED" : "FAIL_PASSWORD", ip, agent);
			throw invalid(request);
		}
		jdbcTemplate.update("""
				UPDATE pmis.platform_admin
				SET login_fail_count = 0, last_login_datetime = CURRENT_TIMESTAMP,
				    admin_status_code = 'ACTIVE', locked_datetime = NULL
				WHERE platform_admin_pk = ?
				""", account.pk());
		accountStatus.evict(account.pk());
		history(account.pk(), id, "SUCCESS", ip, agent);
		boolean changeRequired = changeRequired(account.mustChange(), account.expireAt());
		return issue(account.pk(), changeRequired, request, response, true, null);
	}

	@Transactional(noRollbackFor = ApiException.class)
	public AuthSession refresh(HttpServletRequest request, HttpServletResponse response) {
		requireCsrf(request);
		String raw = cookie(request);
		if (raw == null || raw.isBlank()) {
			throw unauthorized(request);
		}
		List<TokenRow> tokensFound = jdbcTemplate.query("""
				SELECT refresh_token_pk, platform_admin_pk, token_family_id, expire_datetime, revoked_datetime, revoke_reason_code
				FROM pmis.platform_admin_refresh_token WHERE token_hash = ? FOR UPDATE
				""", (resultSet, row) -> new TokenRow(resultSet.getLong(1), resultSet.getLong(2), resultSet.getString(3),
				instant(resultSet.getObject(4)), instant(resultSet.getObject(5)), resultSet.getString(6)), RefreshTokenValues.hash(raw));
		if (tokensFound.isEmpty()) {
			throw unauthorized(request);
		}
		TokenRow token = tokensFound.get(0);
		if (token.expireAt() == null || !token.expireAt().isAfter(Instant.now())) {
			throw fail(request, 401, "AUTH.TOKEN_EXPIRED", "토큰이 만료되었습니다.");
		}
		if (token.revokedAt() != null) {
			RefreshReuseRules.Decision decision = RefreshReuseRules.decide(token.reason(), token.revokedAt(), Instant.now(),
					properties.getRefreshReuseGraceSeconds());
			if (decision == RefreshReuseRules.Decision.IN_PROGRESS) {
				throw fail(request, 409, "AUTH.REFRESH_IN_PROGRESS", "토큰 갱신이 진행 중입니다.");
			}
			revokeFamily(token.familyId(), "REUSE_DETECTED");
			throw fail(request, 401, "AUTH.TOKEN_REUSED", "다시 사용된 토큰입니다.");
		}
		if (!accountStatus.active(token.adminPk()) || !usable(token.adminPk())) {
			revokeFamily(token.familyId(), "ADMIN");
			accountStatus.evict(token.adminPk());
			throw fail(request, 401, "AUTH.ACCOUNT_DISABLED", "사용할 수 없는 계정입니다.");
		}
		jdbcTemplate.update("""
				UPDATE pmis.platform_admin_refresh_token
				SET revoked_datetime = CURRENT_TIMESTAMP, revoke_reason_code = 'ROTATED'
				WHERE refresh_token_pk = ?
				""", token.pk());
		AccountRow account = requireAccount(token.adminPk(), request);
		return issue(token.adminPk(), changeRequired(account.mustChange(), account.expireAt()), request, response, false, token.familyId());
	}

	@Transactional(noRollbackFor = ApiException.class)
	public void logout(HttpServletRequest request, HttpServletResponse response) {
		requireCsrf(request);
		String raw = cookie(request);
		if (raw != null && !raw.isBlank()) {
			List<TokenRow> tokensFound = jdbcTemplate.query("""
					SELECT refresh_token_pk, platform_admin_pk, token_family_id, expire_datetime, revoked_datetime, revoke_reason_code
					FROM pmis.platform_admin_refresh_token WHERE token_hash = ?
					""", (resultSet, row) -> new TokenRow(resultSet.getLong(1), resultSet.getLong(2), resultSet.getString(3),
					instant(resultSet.getObject(4)), instant(resultSet.getObject(5)), resultSet.getString(6)), RefreshTokenValues.hash(raw));
			if (!tokensFound.isEmpty() && tokensFound.get(0).revokedAt() == null && tokensFound.get(0).adminPk() == currentAdminPk(request)) {
				jdbcTemplate.update("""
						UPDATE pmis.platform_admin_refresh_token
						SET revoked_datetime = CURRENT_TIMESTAMP, revoke_reason_code = 'LOGOUT'
						WHERE token_hash = ? AND revoked_datetime IS NULL
						""", RefreshTokenValues.hash(raw));
			}
		}
		RefreshCookies.clear(response, RefreshCookies.PLATFORM_NAME, RefreshCookies.PLATFORM_PATH, properties.getCookie().isSecure());
	}

	@Transactional(noRollbackFor = ApiException.class)
	public AuthSession changePassword(String currentPassword, String newPassword, HttpServletRequest request, HttpServletResponse response) {
		long adminPk = currentAdminPk(request);
		MdmSecurityProperties.Platform policy = properties.getPlatform();
		AccountRow account = requireAccount(adminPk, request);
		if (account.hash() == null || !passwordHasher.matches(currentPassword, account.hash())) {
			throw fail(request, 400, "AUTH.PASSWORD_CURRENT", "현재 비밀번호가 올바르지 않습니다.");
		}
		if (passwordHasher.matches(newPassword, account.hash())) {
			throw fail(request, 400, "AUTH.PASSWORD_SAME", "새 비밀번호는 현재 비밀번호와 달라야 합니다.");
		}
		List<String> violations = PasswordPolicy.violations(newPassword, account.loginId(), policy.getPasswordMinLength(),
				policy.getPasswordMinClasses());
		if (!violations.isEmpty()) {
			throw new ApiException(400, "AUTH.PASSWORD_POLICY", PlatformMessages.text(request, "AUTH.PASSWORD_POLICY", "비밀번호 정책에 맞지 않습니다."), 2,
					violations.stream().map(reason -> new FieldErrorItem("newPassword", reason)).toList());
		}
		String nextHash = passwordHasher.hash(newPassword);
		jdbcTemplate.update("""
				UPDATE pmis.platform_admin
				SET password_hash = ?, must_change_password_yn = 'N',
				    password_changed_datetime = CURRENT_TIMESTAMP,
				    password_expire_datetime = CURRENT_TIMESTAMP + (? * INTERVAL '1 day')
				WHERE platform_admin_pk = ?
				""", nextHash, policy.getPasswordExpireDays(), adminPk);
		revokeUser(adminPk, "PASSWORD_CHANGED");
		accountStatus.evict(adminPk);
		return issue(adminPk, false, request, response, true, null);
	}

	@Transactional(readOnly = true)
	public Me me(HttpServletRequest request) {
		long adminPk = currentAdminPk(request);
		AccountRow account = requireAccount(adminPk, request);
		return new Me(adminPk, account.loginId(), account.name(), account.email(), changeRequired(account.mustChange(), account.expireAt()));
	}

	private AuthSession issue(long adminPk, boolean changeRequired, HttpServletRequest request, HttpServletResponse response,
			boolean newFamily, String familyId) {
		MdmSecurityProperties.Platform policy = properties.getPlatform();
		String raw = RefreshTokenValues.raw();
		String family = newFamily || familyId == null ? UUID.randomUUID().toString() : familyId;
		String ip = ClientAddresses.ip(request, properties.isTrustForwardedHeaders());
		String agent = ClientAddresses.userAgent(request);
		jdbcTemplate.update("""
				INSERT INTO pmis.platform_admin_refresh_token (
				    platform_admin_pk, token_hash, token_family_id, issued_datetime, expire_datetime, ip_address, user_agent)
				VALUES (?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + (? * INTERVAL '1 day'), ?, ?)
				""", adminPk, RefreshTokenValues.hash(raw), family, policy.getRefreshTokenDays(), ip == null ? "" : ip, agent == null ? "" : agent);
		RefreshCookies.set(response, RefreshCookies.PLATFORM_NAME, RefreshCookies.PLATFORM_PATH, raw,
				Duration.ofDays(policy.getRefreshTokenDays()), properties.getCookie().isSecure());
		String access = tokens.issue(Long.toString(adminPk), JwtTokenService.PLATFORM_AUDIENCE, JwtTokenService.PLATFORM_TYPE, null,
				changeRequired, Duration.ofMinutes(policy.getAccessTokenMinutes()));
		return new AuthSession(access, policy.getAccessTokenMinutes() * 60L, changeRequired);
	}

	private boolean applyFailure(long adminPk, LoginLockRules.Action action, int maxFail) {
		if (action == LoginLockRules.Action.FAIL_RESTART) {
			jdbcTemplate.update("""
					UPDATE pmis.platform_admin
					SET login_fail_count = 1,
					    admin_status_code = CASE WHEN ? <= 1 THEN 'LOCKED' ELSE 'ACTIVE' END,
					    locked_datetime = CASE WHEN ? <= 1 THEN CURRENT_TIMESTAMP ELSE NULL END
					WHERE platform_admin_pk = ?
					""", maxFail, maxFail, adminPk);
			return maxFail <= 1;
		}
		jdbcTemplate.update("""
				UPDATE pmis.platform_admin
				SET login_fail_count = login_fail_count + 1,
				    admin_status_code = CASE WHEN login_fail_count + 1 >= ? THEN 'LOCKED' ELSE admin_status_code END,
				    locked_datetime = CASE WHEN login_fail_count + 1 >= ? THEN CURRENT_TIMESTAMP ELSE locked_datetime END
				WHERE platform_admin_pk = ?
				""", maxFail, maxFail, adminPk);
		String status = jdbcTemplate.queryForObject("SELECT admin_status_code FROM pmis.platform_admin WHERE platform_admin_pk = ?",
				String.class, adminPk);
		return "LOCKED".equals(status);
	}

	private void revokeUser(long adminPk, String reason) {
		jdbcTemplate.update("""
				UPDATE pmis.platform_admin_refresh_token
				SET revoked_datetime = CURRENT_TIMESTAMP, revoke_reason_code = ?
				WHERE platform_admin_pk = ? AND revoked_datetime IS NULL
				""", reason, adminPk);
	}

	private void revokeFamily(String familyId, String reason) {
		jdbcTemplate.update("""
				UPDATE pmis.platform_admin_refresh_token
				SET revoked_datetime = COALESCE(revoked_datetime, CURRENT_TIMESTAMP), revoke_reason_code = ?
				WHERE token_family_id = ?
				""", reason, familyId);
	}

	private boolean usable(long adminPk) {
		AccountRow account = requireAccount(adminPk, null);
		return "ACTIVE".equals(account.status()) && "Y".equals(account.useYn());
	}

	private AccountRow requireAccount(long adminPk, HttpServletRequest request) {
		List<AccountRow> rows = jdbcTemplate.query("""
				SELECT platform_admin_pk, password_hash, admin_status_code, use_yn, login_id, admin_name, email,
				       must_change_password_yn, password_expire_datetime, locked_datetime
				FROM pmis.platform_admin WHERE platform_admin_pk = ?
				""", (resultSet, row) -> new AccountRow(resultSet.getLong(1), resultSet.getString(2), resultSet.getString(3),
				trim(resultSet.getString(4)), resultSet.getString(5), resultSet.getString(6), resultSet.getString(7),
				trim(resultSet.getString(8)), instant(resultSet.getObject(9)), instant(resultSet.getObject(10))), adminPk);
		if (rows.isEmpty()) {
			throw unauthorized(request);
		}
		return rows.get(0);
	}

	private void history(Long adminPk, String loginId, String result, String ip, String agent) {
		jdbcTemplate.update("""
				INSERT INTO pmis.platform_admin_login_history (
				    platform_admin_pk, login_id_input, login_result_code, login_datetime, ip_address, user_agent)
				VALUES (?, ?, ?, CURRENT_TIMESTAMP, ?, ?)
				""", (java.sql.PreparedStatement statement) -> {
			if (adminPk == null) {
				statement.setNull(1, java.sql.Types.BIGINT);
			}
			else {
				statement.setLong(1, adminPk);
			}
			statement.setString(2, loginId);
			statement.setString(3, result);
			statement.setString(4, ip);
			statement.setString(5, agent);
		});
	}

	private void requireCsrf(HttpServletRequest request) {
		if (!"1".equals(request.getHeader("X-MDM-Request"))) {
			throw fail(request, 403, "AUTH.CSRF", "요청 헤더가 없습니다.");
		}
	}

	private static String cookie(HttpServletRequest request) {
		Cookie[] cookies = request.getCookies();
		if (cookies == null) {
			return null;
		}
		for (Cookie cookie : cookies) {
			if (RefreshCookies.PLATFORM_NAME.equals(cookie.getName())) {
				return cookie.getValue();
			}
		}
		return null;
	}

	private static long currentAdminPk(HttpServletRequest request) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)
				|| !JwtTokenService.PLATFORM_TYPE.equals(jwt.getClaimAsString("typ"))) {
			throw unauthorized(request);
		}
		return Long.parseLong(jwt.getSubject());
	}

	private static boolean changeRequired(String mustChange, Instant expireAt) {
		return "Y".equals(mustChange) || (expireAt != null && !expireAt.isAfter(Instant.now()));
	}

	private static ApiException invalid(HttpServletRequest request) {
		return fail(request, 401, "AUTH.INVALID_CREDENTIALS", "로그인에 실패했습니다.");
	}

	private static ApiException unauthorized(HttpServletRequest request) {
		return fail(request, 401, "AUTH.UNAUTHORIZED", "인증이 필요합니다.");
	}

	private static ApiException fail(HttpServletRequest request, int status, String code, String korean) {
		return new ApiException(status, code, PlatformMessages.text(request, code, korean), 2);
	}

	private static String trim(String value) {
		return value == null ? null : value.trim();
	}

	private static Instant instant(Object value) {
		if (value == null) {
			return null;
		}
		if (value instanceof Instant instant) {
			return instant;
		}
		if (value instanceof LocalDateTime local) {
			return local.toInstant(ZoneOffset.UTC);
		}
		if (value instanceof java.sql.Timestamp timestamp) {
			return timestamp.toInstant();
		}
		if (value instanceof Date date) {
			return date.toInstant();
		}
		if (value instanceof OffsetDateTime offset) {
			return offset.toInstant();
		}
		return null;
	}

	public record AuthSession(String accessToken, long expiresIn, boolean passwordChangeRequired) {
	}

	public record Me(long platformAdminPk, String loginId, String adminName, String email, boolean passwordChangeRequired) {
	}

	private record AccountRow(long pk, String hash, String status, String useYn, String loginId, String name, String email,
			String mustChange, Instant expireAt, Instant lockedAt) {
		private AccountRow(long pk, String hash, String status, String useYn, int ignoredFail, Instant lockedAt, String mustChange,
				Instant expireAt) {
			this(pk, hash, status, useYn, null, null, null, mustChange, expireAt, lockedAt);
		}
	}

	private record TokenRow(long pk, long adminPk, String familyId, Instant expireAt, Instant revokedAt, String reason) {
	}

}
