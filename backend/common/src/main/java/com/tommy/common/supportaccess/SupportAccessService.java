package com.tommy.common.supportaccess;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.core.env.Environment;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tommy.common.auth.ClientAddresses;
import com.tommy.common.auth.RefreshTokenValues;
import com.tommy.common.global.exception.ApiException;
import com.tommy.common.global.multitenancy.SchemaNames;
import com.tommy.common.global.multitenancy.TenantContext;
import com.tommy.common.global.paging.PageRequests;
import com.tommy.common.global.query.LikeEscape;
import com.tommy.common.global.response.PageResponse;
import com.tommy.common.global.security.JwtTokenService;
import com.tommy.common.global.security.MdmSecurityProperties;
import com.tommy.common.platform.PlatformAuthController;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class SupportAccessService {

	private static final long CACHE_MILLIS = 60_000L;

	private static final Set<String> PLATFORM_SORTS = Set.of("issuedDatetime", "startedDatetime", "tenantCode");

	private static final Set<String> TENANT_SORTS = Set.of("issuedDatetime", "startedDatetime");

	private final JdbcTemplate jdbcTemplate;

	private final JwtTokenService tokens;

	private final MdmSecurityProperties securityProperties;

	private final SupportAccessLocationProperties locationProperties;

	private final Environment environment;

	private final ConcurrentHashMap<Long, CachedSession> sessions = new ConcurrentHashMap<>();

	public SupportAccessService(JdbcTemplate jdbcTemplate, JwtTokenService tokens, MdmSecurityProperties securityProperties,
			SupportAccessLocationProperties locationProperties, Environment environment) {
		this.jdbcTemplate = jdbcTemplate;
		this.tokens = tokens;
		this.securityProperties = securityProperties;
		this.locationProperties = locationProperties;
		this.environment = environment;
	}

	public Issued issue(String tenantCode, String reason, HttpServletRequest request) {
		TenantRow tenant = requireIssuableTenant(tenantCode);
		long adminPk = PlatformAuthController.currentAdminPk();
		String loginId = operatorLogin(adminPk);
		SupportAccessRules.Reason resolved = SupportAccessRules.reason(reason, loginId, ZonedDateTime.now(SupportAccessRules.SEOUL));
		String ticket = RefreshTokenValues.raw();
		String hash = RefreshTokenValues.hash(ticket);
		int ticketSeconds = securityProperties.getSupportAccess().getTicketTtlSeconds();
		String ip = ClientAddresses.ip(request, securityProperties.isTrustForwardedHeaders());
		Long id = jdbcTemplate.queryForObject("""
				INSERT INTO pmis.tenant_support_access (
				    support_access_pk, tenant_pk, platform_admin_pk, platform_login_id, reason, reason_default_yn,
				    status_code, ticket_hash, ticket_expire_datetime, issued_datetime, issue_ip)
				VALUES (nextval('pmis.tenant_support_access_seq'), ?, ?, ?, ?, ?,
				        'ISSUED', ?, CURRENT_TIMESTAMP + (? * INTERVAL '1 second'), CURRENT_TIMESTAMP, ?)
				RETURNING support_access_pk
				""", Long.class, tenant.tenantPk(), adminPk, loginId, resolved.text(), resolved.defaulted() ? "Y" : "N", hash,
				ticketSeconds, ip);
		Instant expire = jdbcTemplate.queryForObject(
				"SELECT ticket_expire_datetime FROM pmis.tenant_support_access WHERE support_access_pk = ?",
				(result, row) -> instant(result, 1), id);
		return new Issued(id, accessUrl(tenant.tenantCode(), tenant.tenantPk(), ticket), expire);
	}

	public Exchanged exchange(String ticket, HttpServletRequest request) {
		String presented = ticket == null ? "" : ticket.trim();
		String hash = RefreshTokenValues.hash(presented);
		String schema = TenantContext.schema();
		SchemaNames.requireTenantSchema(schema);
		int minutes = securityProperties.getSupportAccess().getTtlMinutes();
		String ip = ClientAddresses.ip(request, securityProperties.isTrustForwardedHeaders());
		String agent = ClientAddresses.userAgent(request);
		int updated = jdbcTemplate.update("""
				UPDATE pmis.tenant_support_access a
				SET status_code = 'ACTIVE',
				    started_datetime = CURRENT_TIMESTAMP,
				    expire_datetime = CURRENT_TIMESTAMP + (? * INTERVAL '1 minute'),
				    start_ip = ?,
				    user_agent = ?
				WHERE a.ticket_hash = ?
				  AND a.status_code = 'ISSUED'
				  AND a.ticket_expire_datetime > CURRENT_TIMESTAMP
				  AND a.tenant_pk = (
				      SELECT t.tenant_pk FROM pmis.tenant t
				      WHERE t.schema_name = ? AND t.tenant_status_code = 'ACTIVE' AND btrim(t.use_yn) = 'Y')
				  AND EXISTS (
				      SELECT 1 FROM pmis.platform_admin p
				      WHERE p.platform_admin_pk = a.platform_admin_pk
				        AND p.admin_status_code = 'ACTIVE' AND btrim(p.use_yn) = 'Y')
				""", minutes, ip, agent == null ? "" : agent, hash, schema);
		if (updated != 1) {
			throw ticketInvalid();
		}
		AccessTokenRow row = jdbcTemplate.query("""
				SELECT a.support_access_pk, a.platform_admin_pk, a.expire_datetime, t.tenant_code
				FROM pmis.tenant_support_access a
				JOIN pmis.tenant t ON t.tenant_pk = a.tenant_pk
				WHERE a.ticket_hash = ? AND a.status_code = 'ACTIVE'
				""", (result, index) -> new AccessTokenRow(result.getLong(1), result.getLong(2), instant(result, 3), result.getString(4)),
				hash).stream().findFirst().orElseThrow(SupportAccessService::ticketInvalid);
		long systemUserPk = systemUserPk(schema);
		String access = tokens.issue(Long.toString(systemUserPk), JwtTokenService.TENANT_AUDIENCE, JwtTokenService.TENANT_TYPE,
				row.tenantCode(), false, Duration.ofMinutes(minutes), java.util.Map.of("sup", row.supportAccessPk(), "act", row.platformAdminPk()));
		sessions.remove(row.supportAccessPk());
		return new Exchanged(access, minutes * 60L, row.supportAccessPk());
	}

	@Transactional
	public void end(Jwt jwt) {
		Long accessPk = SupportAccessRules.claimLong(jwt, "sup");
		if (accessPk == null) {
			throw new ApiException(403, "AUTH.SUPPORT_FORBIDDEN", "지원 접속 토큰이 아닙니다.", 2);
		}
		String schema = TenantContext.schema();
		SchemaNames.requireTenantSchema(schema);
		List<String> statuses = jdbcTemplate.query("""
				SELECT a.status_code
				FROM pmis.tenant_support_access a
				JOIN pmis.tenant t ON t.tenant_pk = a.tenant_pk
				WHERE a.support_access_pk = ? AND t.schema_name = ?
				""", (result, index) -> result.getString(1), accessPk, schema);
		if (statuses.isEmpty()) {
			throw ended();
		}
		String status = statuses.get(0);
		if ("ENDED".equals(status) || "EXPIRED".equals(status) || "REVOKED".equals(status)) {
			sessions.remove(accessPk);
			return;
		}
		requireSession(jwt);
		jdbcTemplate.update("""
				UPDATE pmis.tenant_support_access
				SET status_code = 'ENDED', end_reason_code = 'USER_ENDED', ended_datetime = CURRENT_TIMESTAMP
				WHERE support_access_pk = ? AND status_code IN ('ISSUED', 'ACTIVE')
				""", accessPk);
		sessions.remove(accessPk);
	}

	public void requireSession(Jwt jwt) {
		Long accessPk = SupportAccessRules.claimLong(jwt, "sup");
		if (accessPk == null) {
			return;
		}
		String schema = TenantContext.schema();
		SchemaNames.requireTenantSchema(schema);
		CachedSession cached = sessions.get(accessPk);
		long now = System.currentTimeMillis();
		if (cached != null && cached.cachedAt() + CACHE_MILLIS > now) {
			if (!schema.equals(cached.schema()) || expired(cached.expire())) {
				expireIfActive(accessPk);
				throw ended();
			}
			if (!operatorActive(cached.platformAdminPk())) {
				revoke(accessPk);
				throw ended();
			}
			return;
		}
		List<SessionRow> rows = jdbcTemplate.query("""
				SELECT a.status_code, a.expire_datetime, a.platform_admin_pk, t.schema_name,
				       p.admin_status_code, btrim(p.use_yn)
				FROM pmis.tenant_support_access a
				JOIN pmis.tenant t ON t.tenant_pk = a.tenant_pk
				JOIN pmis.platform_admin p ON p.platform_admin_pk = a.platform_admin_pk
				WHERE a.support_access_pk = ?
				""", (result, index) -> new SessionRow(result.getString(1), instant(result, 2), result.getLong(3), result.getString(4),
						result.getString(5), result.getString(6)), accessPk);
		if (rows.isEmpty() || !schema.equals(rows.get(0).schema())) {
			throw ended();
		}
		SessionRow row = rows.get(0);
		if ("ACTIVE".equals(row.status()) && expired(row.expire())) {
			expireIfActive(accessPk);
			throw ended();
		}
		if (!"ACTIVE".equals(row.status())) {
			throw ended();
		}
		if (!"ACTIVE".equals(row.operatorStatus()) || !"Y".equals(row.operatorUse())) {
			revoke(accessPk);
			throw ended();
		}
		Long act = SupportAccessRules.claimLong(jwt, "act");
		if (act == null || act != row.platformAdminPk()) {
			throw ended();
		}
		sessions.put(accessPk, new CachedSession(row.schema(), row.platformAdminPk(), row.expire(), now));
	}

	public SupportView currentSupport(Jwt jwt) {
		Long accessPk = SupportAccessRules.claimLong(jwt, "sup");
		if (accessPk == null) {
			return null;
		}
		List<SupportView> rows = jdbcTemplate.query("""
				SELECT support_access_pk, platform_login_id, expire_datetime
				FROM pmis.tenant_support_access
				WHERE support_access_pk = ?
				""", (result, index) -> new SupportView(result.getLong(1), result.getString(2), instant(result, 3)), accessPk);
		return rows.isEmpty() ? null : rows.get(0);
	}

	public PageResponse<AccessItem> platformList(String tenantCode, String platformLoginId, String status, String from, String to,
			Integer page, Integer size, List<String> sort) {
		Pageable pageable = PageRequests.parse(page, size, sort, PLATFORM_SORTS, List.of(Sort.Order.desc("issuedDatetime")),
				"supportAccessPk");
		return list(null, tenantCode, platformLoginId, status, from, to, pageable, true);
	}

	public PageResponse<AccessItem> tenantList(String status, String from, String to, Integer page, Integer size, List<String> sort) {
		Pageable pageable = PageRequests.parse(page, size, sort, TENANT_SORTS, List.of(Sort.Order.desc("issuedDatetime")),
				"supportAccessPk");
		return list(currentTenantPk(), null, null, status, from, to, pageable, false);
	}

	public PageResponse<ActionItem> platformActions(long supportAccessPk, Integer page, Integer size) {
		requirePlatformAccess(supportAccessPk);
		return actions(supportAccessPk, page, size);
	}

	public PageResponse<ActionItem> tenantActions(long supportAccessPk, Integer page, Integer size) {
		requireTenantAccess(supportAccessPk);
		return actions(supportAccessPk, page, size);
	}

	private PageResponse<AccessItem> list(Long tenantPk, String tenantCode, String platformLoginId, String status, String from,
			String to, Pageable pageable, boolean includeTenant) {
		List<Object> args = new ArrayList<>();
		StringBuilder where = new StringBuilder(" WHERE 1 = 1");
		if (tenantPk != null) {
			where.append(" AND a.tenant_pk = ?");
			args.add(tenantPk);
		}
		if (tenantCode != null && !tenantCode.isBlank()) {
			where.append(" AND t.tenant_code = ?");
			args.add(tenantCode.trim());
		}
		if (platformLoginId != null && !platformLoginId.isBlank()) {
			where.append(" AND lower(a.platform_login_id) LIKE ? ESCAPE '\\'");
			args.add(LikeEscape.contains(platformLoginId));
		}
		if (status != null && !status.isBlank()) {
			where.append(" AND ").append(displayStatusSql()).append(" = ?");
			args.add(status.trim());
		}
		Timestamp fromTime = bound(from, false);
		Timestamp toTime = bound(to, true);
		if (fromTime != null) {
			where.append(" AND a.issued_datetime >= ?");
			args.add(fromTime);
		}
		if (toTime != null) {
			where.append(" AND a.issued_datetime <= ?");
			args.add(toTime);
		}
		String fromSql = """
				FROM pmis.tenant_support_access a
				JOIN pmis.tenant t ON t.tenant_pk = a.tenant_pk
				""";
		Long total = jdbcTemplate.queryForObject("SELECT count(*) " + fromSql + where, Long.class, args.toArray());
		String sql = """
				SELECT a.support_access_pk, t.tenant_code, a.platform_login_id, a.reason, btrim(a.reason_default_yn),
				       %s, a.issued_datetime, a.started_datetime, a.expire_datetime, a.ended_datetime,
				       a.end_reason_code, a.start_ip,
				       (SELECT count(*) FROM pmis.tenant_support_access_action x WHERE x.support_access_pk = a.support_access_pk)
				""".formatted(displayStatusSql()) + fromSql + where + orderBy(pageable) + " LIMIT ? OFFSET ?";
		List<Object> pageArgs = new ArrayList<>(args);
		pageArgs.add(pageable.getPageSize());
		pageArgs.add(pageable.getOffset());
		List<AccessItem> items = jdbcTemplate.query(sql, (result, index) -> mapItem(result, includeTenant), pageArgs.toArray());
		return new PageResponse<>(items, total == null ? 0 : total, pageable.getPageNumber() + 1, pageable.getPageSize());
	}

	private PageResponse<ActionItem> actions(long supportAccessPk, Integer page, Integer size) {
		Pageable pageable = PageRequests.parse(page, size, List.of("occurredDatetime,asc"), Set.of("occurredDatetime"),
				List.of(Sort.Order.asc("occurredDatetime")), "actionPk");
		Long total = jdbcTemplate.queryForObject(
				"SELECT count(*) FROM pmis.tenant_support_access_action WHERE support_access_pk = ?", Long.class, supportAccessPk);
		List<ActionItem> items = jdbcTemplate.query("""
				SELECT action_pk, occurred_datetime, http_method, path, status_code, btrim(sensitive_yn)
				FROM pmis.tenant_support_access_action
				WHERE support_access_pk = ?
				ORDER BY occurred_datetime ASC, action_pk ASC
				LIMIT ? OFFSET ?
				""", (result, index) -> new ActionItem(result.getLong(1), instant(result, 2), result.getString(3), result.getString(4),
						result.getObject(5) == null ? null : result.getInt(5), "Y".equals(result.getString(6))), supportAccessPk,
				pageable.getPageSize(), pageable.getOffset());
		return new PageResponse<>(items, total == null ? 0 : total, pageable.getPageNumber() + 1, pageable.getPageSize());
	}

	private void requirePlatformAccess(long supportAccessPk) {
		Integer count = jdbcTemplate.queryForObject(
				"SELECT count(*) FROM pmis.tenant_support_access WHERE support_access_pk = ?", Integer.class, supportAccessPk);
		if (count == null || count == 0) {
			throw notFound();
		}
	}

	private void requireTenantAccess(long supportAccessPk) {
		Integer count = jdbcTemplate.queryForObject(
				"SELECT count(*) FROM pmis.tenant_support_access WHERE support_access_pk = ? AND tenant_pk = ?", Integer.class,
				supportAccessPk, currentTenantPk());
		if (count == null || count == 0) {
			throw notFound();
		}
	}

	private long currentTenantPk() {
		String schema = TenantContext.schema();
		SchemaNames.requireTenantSchema(schema);
		List<Long> ids = jdbcTemplate.query("SELECT tenant_pk FROM pmis.tenant WHERE schema_name = ?",
				(result, index) -> result.getLong(1), schema);
		if (ids.isEmpty()) {
			throw new ApiException(400, "TENANT.UNRESOLVED", "테넌트를 식별할 수 없습니다.", 2);
		}
		return ids.get(0);
	}

	private TenantRow requireIssuableTenant(String tenantCode) {
		List<TenantRow> rows = jdbcTemplate.query("""
				SELECT tenant_pk, tenant_code, schema_name, tenant_status_code, btrim(use_yn)
				FROM pmis.tenant
				WHERE tenant_code = ?
				""", (result, index) -> new TenantRow(result.getLong(1), result.getString(2), result.getString(3), result.getString(4),
						result.getString(5)), tenantCode);
		if (rows.isEmpty() || !"Y".equals(rows.get(0).useYn())) {
			throw new ApiException(404, "TENANT.NOT_FOUND", "테넌트를 찾을 수 없습니다.", 2);
		}
		TenantRow tenant = rows.get(0);
		if (!"ACTIVE".equals(tenant.status()) || tenant.schemaName() == null || !schemaExists(tenant.schemaName())) {
			throw new ApiException(409, "SUPPORT.TENANT_NOT_ACTIVE", "지원 접속을 열 수 없는 테넌트입니다.", 2);
		}
		return tenant;
	}

	private boolean schemaExists(String schema) {
		Integer count = jdbcTemplate.queryForObject("SELECT count(*) FROM pg_namespace WHERE nspname = ?", Integer.class, schema);
		return count != null && count > 0;
	}

	private String operatorLogin(long adminPk) {
		List<String> logins = jdbcTemplate.query("""
				SELECT login_id FROM pmis.platform_admin
				WHERE platform_admin_pk = ? AND admin_status_code = 'ACTIVE' AND btrim(use_yn) = 'Y'
				""", (result, index) -> result.getString(1), adminPk);
		if (logins.isEmpty()) {
			throw new ApiException(401, "AUTH.ACCOUNT_DISABLED", "사용할 수 없는 계정입니다.", 2);
		}
		return logins.get(0);
	}

	private long systemUserPk(String schema) {
		SchemaNames.requireTenantSchema(schema);
		List<Long> ids = jdbcTemplate.query("SELECT user_pk FROM " + schema + ".user_account WHERE login_id = ?",
				(result, index) -> result.getLong(1), SupportAccounts.LOGIN_ID);
		if (ids.isEmpty()) {
			throw new ApiException(500, "COMMON.INTERNAL", "요청을 처리하지 못했습니다.", 2);
		}
		return ids.get(0);
	}

	private String accessUrl(String tenantCode, long tenantPk, String ticket) {
		String encodedTicket = URLEncoder.encode(ticket, StandardCharsets.UTF_8);
		if (environment.acceptsProfiles(org.springframework.core.env.Profiles.of("dev"))) {
			String base = locationProperties.getDevBaseUrl() == null ? "http://localhost:8000" : locationProperties.getDevBaseUrl().trim();
			while (base.endsWith("/")) {
				base = base.substring(0, base.length() - 1);
			}
			return base + "/support-access?tenant=" + URLEncoder.encode(tenantCode, StandardCharsets.UTF_8) + "#ticket=" + encodedTicket;
		}
		String domain = primaryDomain(tenantPk);
		String scheme = locationProperties.getScheme() == null ? "https" : locationProperties.getScheme().trim().toLowerCase(Locale.ROOT);
		if (!"https".equals(scheme) && !"http".equals(scheme)) {
			scheme = "https";
		}
		return scheme + "://" + domain + "/support-access#ticket=" + encodedTicket;
	}

	private String primaryDomain(long tenantPk) {
		List<String> domains = jdbcTemplate.query("""
				SELECT domain_name FROM pmis.tenant_domain
				WHERE tenant_pk = ? AND primary_yn = 'Y' AND verified_yn = 'Y' AND btrim(use_yn) = 'Y'
				ORDER BY tenant_domain_pk
				""", (result, index) -> result.getString(1), tenantPk);
		if (domains.isEmpty() || domains.get(0) == null || domains.get(0).isBlank()) {
			throw new ApiException(409, "SUPPORT.NO_DOMAIN", "대표 도메인이 없습니다.", 2);
		}
		return domains.get(0).trim();
	}

	private boolean operatorActive(long platformAdminPk) {
		List<Boolean> rows = jdbcTemplate.query("""
				SELECT admin_status_code, btrim(use_yn) FROM pmis.platform_admin WHERE platform_admin_pk = ?
				""", (result, index) -> "ACTIVE".equals(result.getString(1)) && "Y".equals(result.getString(2)), platformAdminPk);
		return !rows.isEmpty() && Boolean.TRUE.equals(rows.get(0));
	}

	private void expireIfActive(long accessPk) {
		jdbcTemplate.update("""
				UPDATE pmis.tenant_support_access
				SET status_code = 'EXPIRED', end_reason_code = 'EXPIRED', ended_datetime = CURRENT_TIMESTAMP
				WHERE support_access_pk = ? AND status_code = 'ACTIVE' AND expire_datetime < CURRENT_TIMESTAMP
				""", accessPk);
		sessions.remove(accessPk);
	}

	private void revoke(long accessPk) {
		jdbcTemplate.update("""
				UPDATE pmis.tenant_support_access
				SET status_code = 'REVOKED', end_reason_code = 'OPERATOR_DISABLED', ended_datetime = CURRENT_TIMESTAMP
				WHERE support_access_pk = ? AND status_code IN ('ISSUED', 'ACTIVE')
				""", accessPk);
		sessions.remove(accessPk);
	}

	private static boolean expired(Instant expire) {
		return expire == null || expire.isBefore(Instant.now());
	}

	private static String displayStatusSql() {
		return "CASE WHEN a.status_code = 'ACTIVE' AND a.expire_datetime < CURRENT_TIMESTAMP THEN 'EXPIRED' ELSE a.status_code END";
	}

	private static String orderBy(Pageable pageable) {
		StringBuilder order = new StringBuilder(" ORDER BY ");
		boolean first = true;
		for (Sort.Order item : pageable.getSort()) {
			if (!first) {
				order.append(", ");
			}
			order.append(column(item.getProperty())).append(item.isAscending() ? " ASC" : " DESC");
			first = false;
		}
		return order.toString();
	}

	private static String column(String property) {
		return switch (property) {
			case "issuedDatetime" -> "a.issued_datetime";
			case "startedDatetime" -> "a.started_datetime";
			case "tenantCode" -> "t.tenant_code";
			case "supportAccessPk" -> "a.support_access_pk";
			default -> throw new ApiException(400, "COMMON.INVALID_SORT", "정렬 필드가 올바르지 않습니다.", 2);
		};
	}

	private static AccessItem mapItem(ResultSet result, boolean includeTenant) throws SQLException {
		return new AccessItem(result.getLong(1), includeTenant ? result.getString(2) : null, result.getString(3), result.getString(4),
				"Y".equals(result.getString(5)), result.getString(6), instant(result, 7), instant(result, 8), instant(result, 9),
				instant(result, 10), result.getString(11), result.getString(12), result.getLong(13));
	}

	private static Timestamp bound(String raw, boolean end) {
		if (raw == null || raw.isBlank()) {
			return null;
		}
		String value = raw.trim();
		try {
			return Timestamp.from(Instant.parse(value));
		}
		catch (DateTimeParseException ignored) {
			try {
				return Timestamp.from(OffsetDateTime.parse(value).toInstant());
			}
			catch (DateTimeParseException ignoredAgain) {
				try {
					return Timestamp.valueOf(LocalDateTime.parse(value));
				}
				catch (DateTimeParseException ignoredDateTime) {
					try {
						LocalDate date = LocalDate.parse(value);
						LocalDateTime time = end ? date.atTime(23, 59, 59) : date.atStartOfDay();
						return Timestamp.from(time.toInstant(ZoneOffset.UTC));
					}
					catch (DateTimeParseException ex) {
						throw new ApiException(400, "COMMON.VALIDATION_FAILED", "요청 값이 올바르지 않습니다.", 2);
					}
				}
			}
		}
	}

	private static Instant instant(ResultSet result, int index) throws SQLException {
		Timestamp timestamp = result.getTimestamp(index);
		return timestamp == null ? null : timestamp.toInstant();
	}

	private static ApiException ticketInvalid() {
		return new ApiException(401, "AUTH.SUPPORT_TICKET_INVALID", "지원 접속 티켓이 올바르지 않습니다.", 2);
	}

	private static ApiException ended() {
		return new ApiException(401, "AUTH.SUPPORT_ENDED", "지원 접속이 끝났습니다.", 2);
	}

	private static ApiException notFound() {
		return new ApiException(404, "SUPPORT.NOT_FOUND", "지원 접속을 찾을 수 없습니다.", 2);
	}

	private record TenantRow(long tenantPk, String tenantCode, String schemaName, String status, String useYn) {
	}

	private record AccessTokenRow(long supportAccessPk, long platformAdminPk, Instant expire, String tenantCode) {
	}

	private record SessionRow(String status, Instant expire, long platformAdminPk, String schema, String operatorStatus,
			String operatorUse) {
	}

	private record CachedSession(String schema, long platformAdminPk, Instant expire, long cachedAt) {
	}

	public record Issued(long supportAccessId, String accessUrl, Instant ticketExpireDatetime) {
	}

	public record Exchanged(String accessToken, long expiresInSeconds, long supportAccessId) {
	}

	public record SupportView(long supportAccessId, String operatorLoginId, Instant expireDatetime) {
	}

	public record AccessItem(long supportAccessId, String tenantCode, String operatorLoginId, String reason, boolean reasonDefault,
			String status, Instant issuedDatetime, Instant startedDatetime, Instant expireDatetime, Instant endedDatetime,
			String endReasonCode, String startIp, long writeCount) {
	}

	public record ActionItem(long actionId, Instant occurredDatetime, String httpMethod, String path, Integer statusCode,
			boolean sensitive) {
	}

}
