package com.tommy.common.platform;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.tommy.common.global.exception.ApiException;
import com.tommy.common.global.multitenancy.SchemaNames;
import com.tommy.common.global.multitenancy.TenantDirectory;
import com.tommy.common.global.paging.PageRequests;
import com.tommy.common.global.response.PageResponse;
import com.tommy.common.tenant.provision.ProvisionTenantCommand;
import com.tommy.common.tenant.provision.ProvisioningJournal;
import com.tommy.common.tenant.provision.TenantAdminPasswordService;
import com.tommy.common.tenant.provision.TenantProvisioningService;
import com.tommy.common.tenant.provision.TenantUpgradeService;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class PlatformTenantService {

	private static final Set<String> TENANT_SORTS = Set.of("tenantCode", "tenantName", "tenantStatusCode", "createDatetime");

	private static final Map<String, String> TENANT_COLUMNS = Map.of(
			"tenantCode", "tenant_code",
			"tenantName", "tenant_name",
			"tenantStatusCode", "tenant_status_code",
			"createDatetime", "create_datetime",
			"tenantPk", "tenant_pk");

	private static final Set<String> MIGRATION_SORTS = Set.of("startedDatetime", "executionStatusCode");

	private static final Map<String, String> MIGRATION_COLUMNS = Map.of(
			"startedDatetime", "started_datetime",
			"executionStatusCode", "execution_status_code",
			"migrationPk", "migration_pk");

	private final JdbcTemplate jdbcTemplate;

	private final TenantProvisioningService provisioningService;

	private final TenantAdminPasswordService passwordService;

	private final TenantUpgradeService upgradeService;

	private final ProvisioningJournal journal;

	private final TenantDirectory tenantDirectory;

	public PlatformTenantService(JdbcTemplate jdbcTemplate, TenantProvisioningService provisioningService,
			TenantAdminPasswordService passwordService, TenantUpgradeService upgradeService, ProvisioningJournal journal,
			TenantDirectory tenantDirectory) {
		this.jdbcTemplate = jdbcTemplate;
		this.provisioningService = provisioningService;
		this.passwordService = passwordService;
		this.upgradeService = upgradeService;
		this.journal = journal;
		this.tenantDirectory = tenantDirectory;
	}

	public PageResponse<TenantSummary> list(String status, String keyword, Integer page, Integer size, List<String> sort,
			HttpServletRequest request) {
		Pageable pageable = PageRequests.parse(page, size, normalizeSort(sort), TENANT_SORTS, List.of(Sort.Order.asc("tenantCode")), "tenantPk");
		List<Object> args = new ArrayList<>();
		StringBuilder where = new StringBuilder(" WHERE 1 = 1");
		if (status != null && !status.isBlank()) {
			where.append(" AND t.tenant_status_code = ?");
			args.add(status.trim());
		}
		if (keyword != null && !keyword.isBlank()) {
			where.append(" AND (lower(t.tenant_code) LIKE ? ESCAPE '\\' OR lower(t.tenant_name) LIKE ? ESCAPE '\\')");
			String pattern = "%" + keyword.trim().toLowerCase(Locale.ROOT).replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
			args.add(pattern);
			args.add(pattern);
		}
		Long total = jdbcTemplate.queryForObject("SELECT count(*) FROM pmis.tenant t" + where, Long.class, args.toArray());
		List<Object> pageArgs = new ArrayList<>(args);
		pageArgs.add(pageable.getPageSize());
		pageArgs.add(pageable.getOffset());
		List<TenantSummary> items = jdbcTemplate.query("""
				SELECT t.tenant_code, t.tenant_name, t.schema_name, t.tenant_status_code, t.default_language_code, t.timezone_id,
				       (SELECT p.current_step_code FROM pmis.tenant_provisioning p
				        WHERE p.tenant_pk = t.tenant_pk
				        ORDER BY p.provisioning_pk DESC
				        LIMIT 1),
				       (SELECT d.domain_name FROM pmis.tenant_domain d
				        WHERE d.tenant_pk = t.tenant_pk AND d.primary_yn = 'Y' AND d.use_yn = 'Y'
				        LIMIT 1),
				       t.create_datetime
				FROM pmis.tenant t
				""" + where + " ORDER BY " + orderBy(pageable, TENANT_COLUMNS) + " LIMIT ? OFFSET ?",
				(resultSet, row) -> new TenantSummary(resultSet.getString(1), resultSet.getString(2), resultSet.getString(3),
						resultSet.getString(4), resultSet.getString(5), resultSet.getString(6), resultSet.getString(7),
						resultSet.getString(8), utcIso(resultSet.getObject(9, LocalDateTime.class))),
				pageArgs.toArray());
		return new PageResponse<>(items, total == null ? 0 : total, pageable.getPageNumber() + 1, pageable.getPageSize());
	}

	public TenantDetail get(String tenantCode, HttpServletRequest request) {
		long tenantPk = requireTenantPk(tenantCode, request);
		TenantDetail detail = jdbcTemplate.query("""
				SELECT tenant_code, tenant_name, schema_name, tenant_status_code, default_language_code, timezone_id,
				       schema_version, contract_start_date, contract_end_date
				FROM pmis.tenant WHERE tenant_pk = ?
				""", (resultSet, row) -> new TenantDetail(resultSet.getString(1), resultSet.getString(2), resultSet.getString(3),
				resultSet.getString(4), resultSet.getString(5), resultSet.getString(6), resultSet.getString(7),
				resultSet.getObject(8, LocalDate.class), resultSet.getObject(9, LocalDate.class), domains(tenantPk),
				provisioning(tenantPk), lastMigration(tenantPk)), tenantPk).stream().findFirst()
				.orElseThrow(() -> missing(request));
		return detail;
	}

	public CreatedTenant create(CreateTenant command, long operatorPk, HttpServletRequest request) {
		List<String> domains = normalizeDomains(command.domains(), request);
		for (String domain : domains) {
			Integer taken = jdbcTemplate.queryForObject("SELECT count(*) FROM pmis.tenant_domain WHERE domain_name = ? AND use_yn = 'Y'", Integer.class, domain);
			if (taken != null && taken > 0) {
				throw fail(request, 409, "TENANT.DUPLICATE", "도메인이 이미 있습니다.");
			}
		}
		var result = provisioningService.provision(new ProvisionTenantCommand(command.tenantCode(), command.tenantName(), null,
				command.defaultLanguageCode(), command.adminName(), command.adminEmail(), operatorPk));
		jdbcTemplate.update("""
				UPDATE pmis.tenant
				SET timezone_id = ?, contract_start_date = ?, contract_end_date = ?, update_datetime = CURRENT_TIMESTAMP, update_user_pk = ?
				WHERE tenant_code = ?
				""", blank(command.timezoneId()), command.contractStartDate(), command.contractEndDate(), operatorPk, command.tenantCode());
		Long tenantPk = jdbcTemplate.queryForObject("SELECT tenant_pk FROM pmis.tenant WHERE tenant_code = ?", Long.class, command.tenantCode());
		insertDomains(tenantPk, domains, operatorPk);
		tenantDirectory.evict(command.tenantCode());
		String password = result.temporaryPassword() == null ? null : result.temporaryPassword().reveal();
		return created(result, password);
	}

	public CreatedTenant retry(String tenantCode, long operatorPk, HttpServletRequest request) {
		ProvisioningJournal.TenantState tenant = journal.findTenant(tenantCode).orElseThrow(() -> missing(request));
		if (!"FAILED".equals(tenant.statusCode()) && !"PROVISIONING".equals(tenant.statusCode())) {
			throw fail(request, 409, "TENANT.ALREADY_ACTIVE", "프로비저닝을 다시 실행할 수 없는 상태입니다.");
		}
		var provisioning = journal.findProvisioning(tenant.tenantPk()).orElseThrow(() -> missing(request));
		var loaded = jdbcTemplate.queryForMap("""
				SELECT t.tenant_name, t.default_language_code, p.admin_name, p.admin_email
				FROM pmis.tenant t
				JOIN pmis.tenant_provisioning p ON p.tenant_pk = t.tenant_pk
				WHERE t.tenant_pk = ?
				ORDER BY p.provisioning_pk DESC
				LIMIT 1
				""", tenant.tenantPk());
		var result = provisioningService.provision(new ProvisionTenantCommand(tenantCode, String.valueOf(loaded.get("tenant_name")),
				tenant.schemaName(), String.valueOf(loaded.get("default_language_code")), String.valueOf(loaded.get("admin_name")),
				String.valueOf(loaded.get("admin_email")), operatorPk));
		tenantDirectory.evict(tenantCode);
		String password = result.temporaryPassword() == null ? null : result.temporaryPassword().reveal();
		return created(result, password);
	}

	public ReissuedPasswordView reissue(String tenantCode, long operatorPk, HttpServletRequest request) {
		var reissued = passwordService.reissue(tenantCode, operatorPk);
		return new ReissuedPasswordView(reissued.tenantCode(), reissued.temporaryPassword().reveal(), reissued.adminLoginId());
	}

	public void changeStatus(String tenantCode, String toStatus, String changeReason, long operatorPk, HttpServletRequest request) {
		if (changeReason == null || changeReason.isBlank()) {
			throw fail(request, 400, "COMMON.VALIDATION_FAILED", "변경 사유가 없습니다.");
		}
		long tenantPk = requireTenantPk(tenantCode, request);
		String from = jdbcTemplate.queryForObject("SELECT tenant_status_code FROM pmis.tenant WHERE tenant_pk = ?", String.class, tenantPk);
		if (!TenantStatusTransitions.allowed(from, toStatus)) {
			throw fail(request, 409, "TENANT.INVALID_STATUS_TRANSITION", "허용되지 않은 상태 변경입니다.");
		}
		jdbcTemplate.update("""
				UPDATE pmis.tenant
				SET tenant_status_code = ?, update_datetime = CURRENT_TIMESTAMP, update_user_pk = ?
				WHERE tenant_pk = ?
				""", toStatus, operatorPk, tenantPk);
		jdbcTemplate.update("""
				INSERT INTO pmis.tenant_status_history (
				    tenant_pk, from_status_code, to_status_code, change_reason, change_datetime, change_user_pk)
				VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, ?)
				""", tenantPk, from, toStatus, changeReason.trim(), operatorPk);
		tenantDirectory.evict(tenantCode);
	}

	public List<DomainView> domains(String tenantCode, HttpServletRequest request) {
		return domains(requireTenantPk(tenantCode, request));
	}

	public DomainView addDomain(String tenantCode, String domainName, long operatorPk, HttpServletRequest request) {
		String domain = DomainNames.require(domainName);
		long tenantPk = requireTenantPk(tenantCode, request);
		Integer taken = jdbcTemplate.queryForObject("SELECT count(*) FROM pmis.tenant_domain WHERE domain_name = ? AND use_yn = 'Y'", Integer.class, domain);
		if (taken != null && taken > 0) {
			throw fail(request, 409, "TENANT.DUPLICATE", "도메인이 이미 있습니다.");
		}
		Integer primary = jdbcTemplate.queryForObject("""
				SELECT count(*) FROM pmis.tenant_domain
				WHERE tenant_pk = ? AND primary_yn = 'Y' AND use_yn = 'Y'
				""", Integer.class, tenantPk);
		String primaryYn = primary != null && primary > 0 ? "N" : "Y";
		jdbcTemplate.update("""
				INSERT INTO pmis.tenant_domain (
				    tenant_pk, domain_name, domain_type_code, primary_yn, verified_yn, use_yn, create_user_pk)
				VALUES (?, ?, ?, ?, 'Y', 'Y', ?)
				""", tenantPk, domain, DomainNames.typeCode(domain), primaryYn, operatorPk);
		tenantDirectory.evict(tenantCode);
		return new DomainView(domain, DomainNames.typeCode(domain), primaryYn, "Y");
	}

	public void deleteDomain(String tenantCode, String domainName, long operatorPk, HttpServletRequest request) {
		String domain = DomainNames.require(domainName);
		long tenantPk = requireTenantPk(tenantCode, request);
		Integer active = jdbcTemplate.queryForObject("""
				SELECT count(*) FROM pmis.tenant_domain WHERE tenant_pk = ? AND use_yn = 'Y'
				""", Integer.class, tenantPk);
		if (active == null || active <= 1) {
			throw fail(request, 409, "TENANT.LAST_DOMAIN", "마지막 활성 도메인은 삭제할 수 없습니다.");
		}
		List<String> primary = jdbcTemplate.query("""
				SELECT primary_yn FROM pmis.tenant_domain
				WHERE tenant_pk = ? AND domain_name = ? AND use_yn = 'Y'
				""", (resultSet, row) -> trim(resultSet.getString(1)), tenantPk, domain);
		if (primary.isEmpty()) {
			throw missing(request);
		}
		jdbcTemplate.update("""
				UPDATE pmis.tenant_domain
				SET use_yn = 'N', primary_yn = 'N', update_datetime = CURRENT_TIMESTAMP, update_user_pk = ?
				WHERE tenant_pk = ? AND domain_name = ? AND use_yn = 'Y'
				""", operatorPk, tenantPk, domain);
		if ("Y".equals(primary.get(0))) {
			jdbcTemplate.update("""
					UPDATE pmis.tenant_domain
					SET primary_yn = 'Y', update_datetime = CURRENT_TIMESTAMP, update_user_pk = ?
					WHERE tenant_domain_pk = (
					    SELECT tenant_domain_pk FROM pmis.tenant_domain
					    WHERE tenant_pk = ? AND use_yn = 'Y'
					    ORDER BY tenant_domain_pk
					    LIMIT 1)
					""", operatorPk, tenantPk);
		}
		tenantDirectory.evict(tenantCode);
	}

	public TenantUpgradeService.UpgradeTenantResult upgrade(String tenantCode, long operatorPk, HttpServletRequest request) {
		return upgradeService.upgradeTenant(tenantCode, operatorPk);
	}

	public TenantUpgradeService.UpgradeBatchResult upgradeAll(long operatorPk) {
		return upgradeService.upgradeAll(operatorPk);
	}

	public PageResponse<MigrationView> migrations(String tenantCode, Integer page, Integer size, List<String> sort, HttpServletRequest request) {
		long tenantPk = requireTenantPk(tenantCode, request);
		Pageable pageable = PageRequests.parse(page, size, normalizeSort(sort), MIGRATION_SORTS, List.of(Sort.Order.desc("startedDatetime")), "migrationPk");
		Long total = jdbcTemplate.queryForObject("SELECT count(*) FROM pmis.tenant_schema_migration WHERE tenant_pk = ?", Long.class, tenantPk);
		List<MigrationView> items = jdbcTemplate.query("""
				SELECT migration_type_code, target_version, execution_status_code, started_datetime, finished_datetime, error_message
				FROM pmis.tenant_schema_migration
				WHERE tenant_pk = ?
				ORDER BY %s
				LIMIT ? OFFSET ?
				""".formatted(orderBy(pageable, MIGRATION_COLUMNS)),
				(resultSet, row) -> new MigrationView(resultSet.getString(1), resultSet.getString(2), resultSet.getString(3),
						String.valueOf(resultSet.getObject(4)), resultSet.getObject(5) == null ? null : String.valueOf(resultSet.getObject(5)),
						resultSet.getString(6)),
				tenantPk, pageable.getPageSize(), pageable.getOffset());
		return new PageResponse<>(items, total == null ? 0 : total, pageable.getPageNumber() + 1, pageable.getPageSize());
	}

	private void insertDomains(long tenantPk, List<String> domains, long operatorPk) {
		boolean first = true;
		for (String domain : domains) {
			jdbcTemplate.update("""
					INSERT INTO pmis.tenant_domain (
					    tenant_pk, domain_name, domain_type_code, primary_yn, verified_yn, use_yn, create_user_pk)
					VALUES (?, ?, ?, ?, 'Y', 'Y', ?)
					""", tenantPk, domain, DomainNames.typeCode(domain), first ? "Y" : "N", operatorPk);
			first = false;
		}
	}

	private List<DomainView> domains(long tenantPk) {
		return jdbcTemplate.query("""
				SELECT domain_name, domain_type_code, primary_yn, use_yn
				FROM pmis.tenant_domain
				WHERE tenant_pk = ? AND use_yn = 'Y'
				ORDER BY primary_yn DESC, domain_name
				""", (resultSet, row) -> new DomainView(resultSet.getString(1), resultSet.getString(2), trim(resultSet.getString(3)),
				trim(resultSet.getString(4))), tenantPk);
	}

	private String provisioning(long tenantPk) {
		List<String> values = jdbcTemplate.query("""
				SELECT current_step_code || ':' || execution_status_code
				FROM pmis.tenant_provisioning WHERE tenant_pk = ?
				ORDER BY provisioning_pk DESC LIMIT 1
				""", (resultSet, row) -> resultSet.getString(1), tenantPk);
		return values.isEmpty() ? null : values.get(0);
	}

	private MigrationView lastMigration(long tenantPk) {
		List<MigrationView> values = jdbcTemplate.query("""
				SELECT migration_type_code, target_version, execution_status_code, started_datetime, finished_datetime, error_message
				FROM pmis.tenant_schema_migration WHERE tenant_pk = ?
				ORDER BY migration_pk DESC LIMIT 1
				""", (resultSet, row) -> new MigrationView(resultSet.getString(1), resultSet.getString(2), resultSet.getString(3),
				String.valueOf(resultSet.getObject(4)), resultSet.getObject(5) == null ? null : String.valueOf(resultSet.getObject(5)),
				resultSet.getString(6)), tenantPk);
		return values.isEmpty() ? null : values.get(0);
	}

	private long requireTenantPk(String tenantCode, HttpServletRequest request) {
		try {
			SchemaNames.requireTenantCode(tenantCode);
		}
		catch (IllegalArgumentException ex) {
			throw fail(request, 400, "COMMON.VALIDATION_FAILED", ex.getMessage());
		}
		List<Long> ids = jdbcTemplate.query("SELECT tenant_pk FROM pmis.tenant WHERE tenant_code = ?",
				(resultSet, row) -> resultSet.getLong(1), tenantCode);
		if (ids.isEmpty()) {
			throw missing(request);
		}
		return ids.get(0);
	}

	private static List<String> normalizeDomains(List<String> domains, HttpServletRequest request) {
		if (domains == null || domains.isEmpty()) {
			throw fail(request, 400, "COMMON.VALIDATION_FAILED", "도메인이 없습니다.");
		}
		LinkedHashSet<String> unique = new LinkedHashSet<>();
		for (String domain : domains) {
			String normalized = DomainNames.require(domain);
			if (!unique.add(normalized)) {
				throw fail(request, 409, "TENANT.DUPLICATE", "도메인이 중복되었습니다.");
			}
		}
		return List.copyOf(unique);
	}

	private static List<String> normalizeSort(List<String> sort) {
		if (sort == null || sort.isEmpty()) {
			return sort;
		}
		List<String> combined = new ArrayList<>();
		for (int index = 0; index < sort.size(); index++) {
			String current = sort.get(index);
			if (current != null && !current.contains(",") && index + 1 < sort.size()
					&& ("asc".equalsIgnoreCase(sort.get(index + 1)) || "desc".equalsIgnoreCase(sort.get(index + 1)))) {
				combined.add(current + "," + sort.get(index + 1));
				index++;
			}
			else {
				combined.add(current);
			}
		}
		return combined;
	}

	private static String orderBy(Pageable pageable, Map<String, String> columns) {
		StringBuilder order = new StringBuilder();
		for (Sort.Order item : pageable.getSort()) {
			String column = columns.get(item.getProperty());
			if (column == null) {
				throw new ApiException(400, "COMMON.INVALID_SORT", "정렬 필드가 올바르지 않습니다.", 2);
			}
			if (!order.isEmpty()) {
				order.append(", ");
			}
			order.append(column).append(item.isAscending() ? " ASC" : " DESC");
		}
		return order.toString();
	}

	private static String blank(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}

	private CreatedTenant created(com.tommy.common.tenant.provision.ProvisioningResult result, String password) {
		String adminLoginId = result.adminLoginId();
		if (adminLoginId == null || adminLoginId.isBlank()) {
			adminLoginId = passwordService.storedAdminLoginId(result.tenantCode());
		}
		return new CreatedTenant(result.tenantCode(), result.schemaName(), result.tenantStatus(),
				result.currentStep() == null ? null : result.currentStep().name(), result.executionStatus(), password, adminLoginId);
	}

	private static String utcIso(LocalDateTime value) {
		if (value == null) {
			return null;
		}
		return value.atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
	}

	private static String trim(String value) {
		return value == null ? null : value.trim();
	}

	private static ApiException missing(HttpServletRequest request) {
		return fail(request, 404, "TENANT.NOT_FOUND", "테넌트를 찾을 수 없습니다.");
	}

	private static ApiException fail(HttpServletRequest request, int status, String code, String korean) {
		return new ApiException(status, code, PlatformMessages.text(request, code, korean), 2);
	}

	public record TenantSummary(String tenantCode, String tenantName, String schemaName, String tenantStatusCode,
			String defaultLanguageCode, String timezoneId, String provisioningStatus, String primaryDomain, String createDatetime) {
	}

	public record DomainView(String domainName, String domainTypeCode, String primaryYn, String useYn) {
	}

	public record MigrationView(String migrationTypeCode, String targetVersion, String executionStatusCode, String startedDatetime,
			String finishedDatetime, String errorMessage) {
	}

	public record TenantDetail(String tenantCode, String tenantName, String schemaName, String tenantStatusCode,
			String defaultLanguageCode, String timezoneId, String schemaVersion, LocalDate contractStartDate, LocalDate contractEndDate,
			List<DomainView> domains, String provisioning, MigrationView lastMigration) {
	}

	public record CreateTenant(String tenantCode, String tenantName, String defaultLanguageCode, String timezoneId,
			LocalDate contractStartDate, LocalDate contractEndDate, String adminName, String adminEmail, List<String> domains) {
	}

	public record CreatedTenant(String tenantCode, String schemaName, String tenantStatus, String provisioningStep, String executionStatus,
			String temporaryPassword, String adminLoginId) {
		@Override
		public String toString() {
			return "CreatedTenant[tenantCode=" + tenantCode + ", temporaryPassword=masked]";
		}
	}

	public record ReissuedPasswordView(String tenantCode, String temporaryPassword, String adminLoginId) {
		@Override
		public String toString() {
			return "ReissuedPasswordView[tenantCode=" + tenantCode + ", temporaryPassword=masked]";
		}
	}

}
