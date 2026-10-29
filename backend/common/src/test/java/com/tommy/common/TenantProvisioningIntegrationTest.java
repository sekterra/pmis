package com.tommy.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import com.tommy.common.global.exception.ApiException;
import com.tommy.common.global.multitenancy.TenantDirectory;
import com.tommy.common.global.multitenancy.TenantSnapshot;
import com.tommy.common.global.security.BcryptPasswordHasher;
import com.tommy.common.global.security.PasswordHasher;
import com.tommy.common.tenant.provision.AdminCreatedProvisioningStep;
import com.tommy.common.tenant.provision.CompletedProvisioningStep;
import com.tommy.common.tenant.provision.FlywayTenantSchemaMigrator;
import com.tommy.common.tenant.provision.MigratedProvisioningStep;
import com.tommy.common.tenant.provision.PgTenantAdvisoryLock;
import com.tommy.common.tenant.provision.ProvisionTenantCommand;
import com.tommy.common.tenant.provision.ProvisioningJournal;
import com.tommy.common.tenant.provision.ProvisioningResult;
import com.tommy.common.tenant.provision.ProvisioningStep;
import com.tommy.common.tenant.provision.ProvisioningStepCode;
import com.tommy.common.tenant.provision.RequestedProvisioningStep;
import com.tommy.common.tenant.provision.SchemaCreatedProvisioningStep;
import com.tommy.common.tenant.provision.ReissuedPassword;
import com.tommy.common.tenant.provision.SeededProvisioningStep;
import com.tommy.common.tenant.provision.TenantAdminPasswordService;
import com.tommy.common.tenant.provision.TenantAdminWriter;
import com.tommy.common.tenant.provision.TenantProvisioningService;
import com.tommy.common.tenant.provision.TenantSeedRunner;
import com.tommy.common.tenant.provision.TenantSchemaMigrator;
import com.tommy.common.tenant.provision.TenantUpgradeService;
import com.zaxxer.hikari.HikariDataSource;

@Tag("integration")
class TenantProvisioningIntegrationTest {

	private static final String SCHEMA_PATTERN = "^t_it_[a-z0-9]{16}$";

	private final List<String> tenantCodes = new ArrayList<>();
	private final List<String> schemas = new ArrayList<>();
	private HikariDataSource pool;

	@BeforeAll
	static void removeLeftoverItTenants() throws Exception {
		if (!TestcontainersConfiguration.isConfigured()) {
			return;
		}
		try (Connection connection = openConnection()) {
			IntegrationDatabase.requirePmis(connection);
			removeItPrefixed(connection);
		}
	}

	@AfterEach
	void cleanup() throws Exception {
		if (!TestcontainersConfiguration.isConfigured()) {
			return;
		}
		try (Connection connection = openConnection()) {
			IntegrationDatabase.requirePmis(connection);
			for (String tenantCode : tenantCodes) {
				deleteTenant(connection, tenantCode);
			}
			for (String schema : schemas) {
				dropSchema(connection, schema);
			}
		}
		finally {
			if (pool != null) {
				pool.close();
			}
		}
	}

	@Test
	void provisionsSchemaAndStaysAtMigrated() throws Exception {
		org.junit.jupiter.api.Assumptions.assumeTrue(TestcontainersConfiguration.isConfigured());
		String suffix = suffix();
		String code = "it-" + suffix;
		String schema = "t_it_" + suffix;
		track(code, schema);
		TenantProvisioningService service = service(pool(), new FlywayTenantSchemaMigrator(pool));
		ProvisioningResult result = service.provision(command(code));
		assertPausedAtMigrated(result, schema);
		assertEquals(31, tableCount(schema));
		assertTrue(flywayHistoryExists(schema));
		assertEquals("6", schemaVersion(code));
		assertFalse(searchPath().contains(schema));
		ProvisioningResult again = service.provision(command(code));
		assertPausedAtMigrated(again, schema);
		assertEquals(1, migrationCount(code, "PROVISION"));
	}

	@Test
	void seedStaysIdempotentAndMatchesRules() throws Exception {
		org.junit.jupiter.api.Assumptions.assumeTrue(TestcontainersConfiguration.isConfigured());
		String suffix = suffix();
		String code = "it-" + suffix;
		String schema = "t_it_" + suffix;
		track(code, schema);
		TenantProvisioningService service = service(pool(), new FlywayTenantSchemaMigrator(pool));
		assertPausedAtMigrated(service.provision(command(code)), schema);
		int languages = count(schema, "support_language");
		int codes = count(schema, "common_code");
		int messages = count(schema, "i18n_message");
		int grants = count(schema, "role_permission");
		try (Connection connection = openConnection()) {
			IntegrationDatabase.requirePmis(connection);
			try (Statement statement = connection.createStatement()) {
				statement.execute("SET search_path TO " + schema);
				statement.execute("""
						UPDATE common_code SET code_name = 'CHANGED'
						WHERE code_value = 'GROUP'
						  AND code_group_pk = (SELECT code_group_pk FROM code_group WHERE code_group_code = 'MENU_TYPE')
						""");
				statement.execute("UPDATE role SET role_name = 'CHANGED' WHERE role_code = 'USER'");
			}
		}
		assertPausedAtMigrated(service.provision(command(code)), schema);
		assertEquals(languages, count(schema, "support_language"));
		assertEquals(codes, count(schema, "common_code"));
		assertEquals(messages, count(schema, "i18n_message"));
		assertEquals(grants, count(schema, "role_permission"));
		assertEquals("CHANGED", scalarInSchema(schema, """
				SELECT code_name FROM common_code
				WHERE code_value = 'GROUP'
				  AND code_group_pk = (SELECT code_group_pk FROM code_group WHERE code_group_code = 'MENU_TYPE')
				"""));
		assertEquals("CHANGED", scalarInSchema(schema, "SELECT role_name FROM role WHERE role_code = 'USER'"));
		assertEquals("ko-KR", scalarInSchema(schema, """
				SELECT support_language_code FROM support_language WHERE default_yn = 'Y' AND use_yn = 'Y'
				"""));
		assertEquals("1", scalarInSchema(schema,
				"SELECT count(*)::text FROM support_language WHERE default_yn = 'Y' AND use_yn = 'Y'"));
		assertEquals("0", scalarInSchema(schema, """
				SELECT count(*)::text FROM i18n_message m
				WHERE (SELECT count(*) FROM i18n_message_text t
				       WHERE t.message_pk = m.message_pk
				         AND t.support_language_code IN ('ko-KR', 'en-US')) < 2
				"""));
		assertEquals("0", scalarInSchema(schema, """
				SELECT count(*)::text FROM common_code c
				WHERE (SELECT count(*) FROM common_code_lang l
				       WHERE l.code_pk = c.code_pk
				         AND l.support_language_code IN ('ko-KR', 'en-US')) < 2
				"""));
		for (String group : java.util.List.of(
				"STORAGE_TYPE", "FILE_STATUS", "TEAM_TYPE", "TEAM_ROLE", "MENU_TYPE", "PERMISSION_ACTION",
				"COMPANY_TYPE", "DEPT_TYPE", "DEPT_CHANGE_TYPE", "EMPLOYMENT_TYPE", "ASSIGNMENT_STATUS",
				"ACCOUNT_TYPE", "USER_STATUS", "PASSWORD_CHANGE_TYPE", "LOGIN_RESULT", "TOKEN_REVOKE_REASON",
				"I18N_MESSAGE_TYPE", "I18N_TRANSLATION_STATUS", "ATTACHMENT_CATEGORY", "POSITION", "JOB_TITLE",
				"SECURITY_POLICY")) {
			assertEquals("1", scalarInSchema(schema,
					"SELECT count(*)::text FROM code_group WHERE code_group_code = '" + group + "'"));
		}
		assertEquals("12", scalarInSchema(schema, """
				SELECT count(*)::text FROM common_code c
				JOIN code_group g ON g.code_group_pk = c.code_group_pk
				WHERE g.code_group_code IN ('POSITION', 'JOB_TITLE')
				"""));
		assertEquals("5", scalarInSchema(schema, """
				SELECT count(*)::text FROM permission p
				JOIN menu m ON m.menu_pk = p.menu_pk
				WHERE m.menu_code = 'COMPANY'
				"""));
		assertEquals("READ", scalarInSchema(schema, """
				SELECT string_agg(p.action_code, ',' ORDER BY p.action_code)
				FROM permission p JOIN menu m ON m.menu_pk = p.menu_pk
				WHERE m.menu_code = 'SYSTEM'
				"""));
		assertEquals("0", scalarInSchema(schema, """
				SELECT count(*)::text
				FROM role_permission rp
				JOIN permission p ON p.permission_pk = rp.permission_pk
				JOIN menu m ON m.menu_pk = p.menu_pk
				WHERE p.action_code <> 'READ'
				  AND NOT EXISTS (
				      SELECT 1 FROM role_permission read_grant
				      JOIN permission read_permission ON read_permission.permission_pk = read_grant.permission_pk
				      WHERE read_grant.role_pk = rp.role_pk
				        AND read_permission.menu_pk = m.menu_pk
				        AND read_permission.action_code = 'READ')
				"""));
		assertEquals(scalarInSchema(schema, "SELECT count(*)::text FROM permission"), scalarInSchema(schema, """
				SELECT count(*)::text FROM role_permission rp
				JOIN role r ON r.role_pk = rp.role_pk
				WHERE r.role_code = 'TENANT_ADMIN'
				"""));
		assertEquals("0", scalarInSchema(schema, """
				SELECT count(*)::text FROM role_permission rp
				JOIN role r ON r.role_pk = rp.role_pk
				JOIN permission p ON p.permission_pk = rp.permission_pk
				WHERE r.role_code = 'USER' AND p.action_code <> 'READ'
				"""));
		assertEquals("CONTRACT,GENERAL,PHOTO,PROFILE", scalarInSchema(schema, """
				SELECT string_agg(c.code_value, ',' ORDER BY c.code_value)
				FROM common_code c
				JOIN code_group g ON g.code_group_pk = c.code_group_pk
				WHERE g.code_group_code = 'ATTACHMENT_CATEGORY'
				"""));
		assertEquals("7", scalarInSchema(schema, """
				SELECT c.ext_attr1
				FROM common_code c
				JOIN code_group g ON g.code_group_pk = c.code_group_pk
				WHERE g.code_group_code = 'SECURITY_POLICY' AND c.code_value = 'TEMP_PASSWORD_VALID_DAYS'
				"""));
	}

	@Test
	void retriesMigrationAfterFailure() throws Exception {
		org.junit.jupiter.api.Assumptions.assumeTrue(TestcontainersConfiguration.isConfigured());
		String suffix = suffix();
		String code = "it-" + suffix;
		String schema = "t_it_" + suffix;
		track(code, schema);
		AtomicInteger attempts = new AtomicInteger();
		TenantSchemaMigrator migrator = schemaName -> {
			if (attempts.getAndIncrement() == 0) {
				throw new IllegalStateException("migration failed");
			}
			return new FlywayTenantSchemaMigrator(pool).migrate(schemaName);
		};
		TenantProvisioningService service = service(pool(), migrator);
		ApiException failed = assertThrows(ApiException.class, () -> service.provision(command(code)));
		assertEquals("TENANT.PROVISIONING_FAILED", failed.errorCode());
		assertEquals("FAILED", tenantStatus(code));
		assertEquals("SCHEMA_CREATED", currentStep(code));
		assertTrue(retryCount(code) >= 1);
		assertFalse(errorMessage(code).contains("jdbc:"));
		ProvisioningResult retried = service.provision(command(code));
		assertPausedAtMigrated(retried, schema);
		assertEquals(31, tableCount(schema));
	}

	@Test
	void rejectsExistingSchemaWithoutRecord() throws Exception {
		org.junit.jupiter.api.Assumptions.assumeTrue(TestcontainersConfiguration.isConfigured());
		String suffix = suffix();
		String code = "it-" + suffix;
		String schema = "t_it_" + suffix;
		track(code, schema);
		try (Connection connection = openConnection()) {
			IntegrationDatabase.requirePmis(connection);
			try (Statement statement = connection.createStatement()) {
				statement.execute("CREATE SCHEMA " + schema);
			}
		}
		TenantProvisioningService service = service(pool(), new FlywayTenantSchemaMigrator(pool));
		ApiException exception = assertThrows(ApiException.class,
				() -> service.provision(new ProvisionTenantCommand(code, "IT", schema, "ko", "Admin", "it-admin@example.com", null)));
		assertEquals("TENANT.SCHEMA_NOT_OWNED", exception.errorCode());
		assertEquals("FAILED", tenantStatus(code));
		assertEquals("REQUESTED", currentStep(code));
		assertFalse(flywayHistoryExists(schema));
	}

	@Test
	void rejectsConcurrentProvisioning() throws Exception {
		org.junit.jupiter.api.Assumptions.assumeTrue(TestcontainersConfiguration.isConfigured());
		String suffix = suffix();
		String code = "it-" + suffix;
		String schema = "t_it_" + suffix;
		track(code, schema);
		CountDownLatch entered = new CountDownLatch(1);
		CountDownLatch release = new CountDownLatch(1);
		TenantSchemaMigrator migrator = schemaName -> {
			entered.countDown();
			await(release);
			return new FlywayTenantSchemaMigrator(pool).migrate(schemaName);
		};
		TenantProvisioningService service = service(pool(), migrator);
		AtomicReference<Throwable> failure = new AtomicReference<>();
		Thread first = new Thread(() -> {
			try {
				service.provision(command(code));
			}
			catch (Throwable ex) {
				failure.set(ex);
			}
		});
		first.start();
		assertTrue(entered.await(30, TimeUnit.SECONDS));
		ApiException rejected = assertThrows(ApiException.class, () -> service.provision(command(code)));
		assertEquals("TENANT.PROVISIONING_IN_PROGRESS", rejected.errorCode());
		release.countDown();
		first.join(60_000);
		assertTrue(failure.get() == null, "첫 프로비저닝이 실패했습니다.");
		assertPausedAtMigrated(read(code), schema);
	}

	@Test
	void upgradesActiveTenantsAndContinuesAfterFailure() throws Exception {
		org.junit.jupiter.api.Assumptions.assumeTrue(TestcontainersConfiguration.isConfigured());
		String suffixA = suffix();
		String suffixB = suffix();
		String suffixS = suffix();
		String codeA = "it-" + suffixA;
		String codeB = "it-" + suffixB;
		String codeS = "it-" + suffixS;
		String schemaA = "t_it_" + suffixA;
		String schemaB = "t_it_" + suffixB;
		String schemaS = "t_it_" + suffixS;
		track(codeA, schemaA);
		track(codeB, schemaB);
		track(codeS, schemaS);
		try (Connection connection = openConnection()) {
			IntegrationDatabase.requirePmis(connection);
			createSchema(connection, schemaA);
			createSchema(connection, schemaB);
			createSchema(connection, schemaS);
			insertTenant(connection, codeA, schemaA, "ACTIVE");
			insertTenant(connection, codeB, schemaB, "ACTIVE");
			insertTenant(connection, codeS, schemaS, "SUSPENDED");
		}
		DataSource dataSource = pool();
		TenantSchemaMigrator migrator = schemaName -> {
			if (schemaB.equals(schemaName)) {
				throw new IllegalStateException("migration failed");
			}
			return new FlywayTenantSchemaMigrator(dataSource).migrate(schemaName);
		};
		TenantUpgradeService service = new TenantUpgradeService(new PgTenantAdvisoryLock(dataSource), journal(dataSource),
				migrator, new com.tommy.common.tenant.provision.TenantSeedRunner(dataSource), directory());
		TenantUpgradeService.UpgradeBatchResult result = service.upgradeTenants(List.of(codeA, codeB), null);
		assertFalse(result.results().stream().anyMatch(item -> codeS.equals(item.tenantCode())));
		assertEquals("SUCCESS", statusOf(result, codeA));
		assertEquals("FAILED", statusOf(result, codeB));
		assertEquals(31, tableCount(schemaA));
		assertTrue(flywayHistoryExists(schemaA));
		assertFalse(flywayHistoryExists(schemaB));
		assertFalse(flywayHistoryExists(schemaS));
		assertEquals(0, migrationCount(codeS, "UPGRADE"));
		assertFalse(searchPath().contains(schemaA));
		assertEquals("CONTRACT,GENERAL,PHOTO,PROFILE", scalarInSchema(schemaA, """
				SELECT string_agg(c.code_value, ',' ORDER BY c.code_value)
				FROM common_code c
				JOIN code_group g ON g.code_group_pk = c.code_group_pk
				WHERE g.code_group_code = 'ATTACHMENT_CATEGORY'
				"""));
		assertEquals("7", policyDays(schemaA));
		try (Connection connection = openConnection()) {
			IntegrationDatabase.requirePmis(connection);
			try (Statement statement = connection.createStatement()) {
				statement.execute("SET search_path TO " + schemaA);
				statement.execute("""
						UPDATE common_code SET code_name = 'CHANGED', ext_attr1 = '11'
						WHERE code_value = 'GENERAL'
						   OR code_value = 'TEMP_PASSWORD_VALID_DAYS'
						""");
			}
		}
		TenantUpgradeService.UpgradeBatchResult again = service.upgradeTenants(List.of(codeA, codeB), null);
		assertEquals("SUCCESS", statusOf(again, codeA));
		assertEquals("FAILED", statusOf(again, codeB));
		assertEquals("CHANGED", scalarInSchema(schemaA, """
				SELECT code_name FROM common_code WHERE code_value = 'GENERAL'
				"""));
		assertEquals("11", policyDays(schemaA));
	}

	@Test
	void provisionsInitialAdminAndReissuesPassword() throws Exception {
		org.junit.jupiter.api.Assumptions.assumeTrue(TestcontainersConfiguration.isConfigured());
		String suffix = suffix();
		String code = "it-" + suffix;
		String schema = "t_it_" + suffix;
		track(code, schema);
		DataSource dataSource = pool();
		PasswordHasher hasher = new BcryptPasswordHasher(4);
		assertPausedAtMigrated(service(dataSource, new FlywayTenantSchemaMigrator(dataSource)).provision(adminCommand(code)), schema);
		executeInSchema(schema, """
				UPDATE common_code SET ext_attr1 = '3'
				WHERE code_value = 'TEMP_PASSWORD_VALID_DAYS'
				  AND code_group_pk = (SELECT code_group_pk FROM code_group WHERE code_group_code = 'SECURITY_POLICY')
				""");
		ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(
				org.slf4j.Logger.ROOT_LOGGER_NAME);
		ch.qos.logback.core.read.ListAppender<ch.qos.logback.classic.spi.ILoggingEvent> logs = new ch.qos.logback.core.read.ListAppender<>();
		logs.start();
		root.addAppender(logs);
		ProvisioningResult created;
		try {
			created = fullService(dataSource, hasher).provision(adminCommand(code));
		}
		finally {
			root.detachAppender(logs);
		}
		String plain = created.temporaryPassword().reveal();
		assertEquals("ACTIVE", created.tenantStatus());
		assertEquals(ProvisioningStepCode.COMPLETED, created.currentStep());
		assertEquals("SUCCESS", created.executionStatus());
		assertFalse(created.adminAlreadyPresent());
		assertEquals("admin.user@example.com", created.adminLoginId());
		assertFalse(created.toString().contains(plain), "프로비저닝 결과 문자열에 평문이 있습니다.");
		assertFalse(logs.list.stream().anyMatch(event -> String.valueOf(event.getFormattedMessage()).contains(plain)),
				"로그에 임시 비밀번호가 있습니다.");
		assertEquals("0", plaintextHits(schema, plain));
		assertFalse(errorMessage(code).contains(plain), "오류 메시지에 평문이 있습니다.");
		assertTrue(hasher.matches(plain, passwordHash(schema)));
		assertEquals(code, scalarInSchema(schema, "SELECT company_code FROM company"));
		assertEquals("아이티", scalarInSchema(schema, "SELECT company_name FROM company"));
		assertEquals("OWN", scalarInSchema(schema, "SELECT company_type_code FROM company"));
		assertEquals("관리자", scalarInSchema(schema, "SELECT person_name FROM person"));
		assertEquals("Admin.User@Example.com", scalarInSchema(schema, "SELECT email FROM person"));
		assertEquals("1", scalarInSchema(schema, """
				SELECT count(*)::text FROM person_assignment
				WHERE employment_type_code = 'REGULAR' AND assignment_status_code = 'ACTIVE'
				  AND primary_yn = 'Y' AND dept_head_yn = 'N' AND start_date = CURRENT_DATE
				"""));
		assertEquals("admin.user@example.com", scalarInSchema(schema, "SELECT login_id FROM user_account"));
		assertEquals("Y", scalarInSchema(schema, "SELECT must_change_password_yn FROM user_account"));
		assertEquals("NORMAL", scalarInSchema(schema, "SELECT account_type_code FROM user_account"));
		assertEquals("ACTIVE", scalarInSchema(schema, "SELECT user_status_code FROM user_account"));
		assertEquals("ko-KR", scalarInSchema(schema, "SELECT support_language_code FROM user_account"));
		assertEquals("3", scalarInSchema(schema,
				"SELECT (password_expire_datetime::date - password_changed_datetime::date)::text FROM user_account"));
		assertEquals("0", scalarInSchema(schema, """
				SELECT (
				    (SELECT count(*) FROM company WHERE create_user_pk IS NOT NULL)
				    + (SELECT count(*) FROM person WHERE create_user_pk IS NOT NULL)
				    + (SELECT count(*) FROM person_assignment WHERE create_user_pk IS NOT NULL)
				    + (SELECT count(*) FROM user_account WHERE create_user_pk IS NOT NULL)
				    + (SELECT count(*) FROM user_role WHERE create_user_pk IS NOT NULL)
				)::text
				"""));
		assertEquals("1", scalarInSchema(schema, """
				SELECT count(*)::text FROM user_password_history WHERE change_type_code = 'INITIAL'
				"""));
		assertEquals("1", scalarInSchema(schema, """
				SELECT count(*)::text FROM user_role ur
				JOIN role r ON r.role_pk = ur.role_pk
				WHERE r.role_code = 'TENANT_ADMIN'
				"""));
		rewindToSeeded(code);
		ProvisioningResult existed = fullService(dataSource, hasher).provision(adminCommand(code));
		assertTrue(existed.adminAlreadyPresent());
		assertTrue(existed.temporaryPassword() == null);
		assertEquals("admin.user@example.com", existed.adminLoginId());
		assertEquals("ACTIVE", existed.tenantStatus());
		assertEquals("1", scalarInSchema(schema, "SELECT count(*)::text FROM user_account"));
		assertEquals("1", scalarInSchema(schema, """
				SELECT count(*)::text FROM user_password_history WHERE change_type_code = 'INITIAL'
				"""));
		ApiException alreadyActive = assertThrows(ApiException.class,
				() -> fullService(dataSource, hasher).provision(adminCommand(code)));
		assertEquals("TENANT.ALREADY_ACTIVE", alreadyActive.errorCode());
		executeInSchema(schema, """
				UPDATE user_account
				SET user_status_code = 'LOCKED', login_fail_count = 4, locked_datetime = CURRENT_TIMESTAMP
				""");
		String previousHash = passwordHash(schema);
		ReissuedPassword reissued = new TenantAdminPasswordService(new PgTenantAdvisoryLock(dataSource), journal(dataSource),
				adminWriter(dataSource, hasher)).reissue(code, 15L);
		String nextPlain = reissued.temporaryPassword().reveal();
		assertFalse(reissued.toString().contains(nextPlain), "재발급 결과 문자열에 평문이 있습니다.");
		assertFalse(hasher.matches(plain, passwordHash(schema)));
		assertTrue(hasher.matches(nextPlain, passwordHash(schema)));
		assertFalse(previousHash.equals(passwordHash(schema)));
		assertEquals("ACTIVE", scalarInSchema(schema, "SELECT user_status_code FROM user_account"));
		assertEquals("0", scalarInSchema(schema, "SELECT login_fail_count::text FROM user_account"));
		assertEquals("Y", scalarInSchema(schema, "SELECT must_change_password_yn FROM user_account"));
		assertEquals("1", scalarInSchema(schema, """
				SELECT count(*)::text FROM user_password_history
				WHERE change_type_code = 'RESET' AND changed_user_pk = 15
				"""));
		try (Connection connection = openConnection()) {
			IntegrationDatabase.requirePmis(connection);
			try (PreparedStatement statement = connection.prepareStatement(
					"UPDATE pmis.tenant SET tenant_status_code = 'SUSPENDED' WHERE tenant_code = ?")) {
				statement.setString(1, code);
				statement.executeUpdate();
			}
		}
		String hashAfterReissue = passwordHash(schema);
		ApiException inactive = assertThrows(ApiException.class,
				() -> new TenantAdminPasswordService(new PgTenantAdvisoryLock(dataSource), journal(dataSource),
						adminWriter(dataSource, hasher)).reissue(code, 15L));
		assertEquals("TENANT.INACTIVE", inactive.errorCode());
		assertEquals(hashAfterReissue, passwordHash(schema));
	}

	@Test
	void missingPolicyRollsBackAndRetryCreatesAdmin() throws Exception {
		org.junit.jupiter.api.Assumptions.assumeTrue(TestcontainersConfiguration.isConfigured());
		String suffix = suffix();
		String code = "it-" + suffix;
		String schema = "t_it_" + suffix;
		track(code, schema);
		DataSource dataSource = pool();
		PasswordHasher hasher = new BcryptPasswordHasher(4);
		assertPausedAtMigrated(service(dataSource, new FlywayTenantSchemaMigrator(dataSource)).provision(adminCommand(code)), schema);
		executeInSchema(schema, """
				DELETE FROM common_code_lang
				WHERE code_pk IN (
				    SELECT c.code_pk FROM common_code c
				    JOIN code_group g ON g.code_group_pk = c.code_group_pk
				    WHERE g.code_group_code = 'SECURITY_POLICY' AND c.code_value = 'TEMP_PASSWORD_VALID_DAYS')
				""");
		executeInSchema(schema, """
				DELETE FROM common_code c
				USING code_group g
				WHERE g.code_group_pk = c.code_group_pk
				  AND g.code_group_code = 'SECURITY_POLICY'
				  AND c.code_value = 'TEMP_PASSWORD_VALID_DAYS'
				""");
		ApiException missing = assertThrows(ApiException.class,
				() -> fullService(dataSource, hasher).provision(adminCommand(code)));
		assertEquals("TENANT.SECURITY_POLICY_MISSING", missing.errorCode());
		assertEquals("FAILED", tenantStatus(code));
		assertEquals("SEEDED", currentStep(code));
		assertEquals("0", scalarInSchema(schema, "SELECT count(*)::text FROM company"));
		assertEquals("0", scalarInSchema(schema, "SELECT count(*)::text FROM person"));
		assertEquals("0", scalarInSchema(schema, "SELECT count(*)::text FROM user_account"));
		new TenantSeedRunner(dataSource).run(schema);
		ProvisioningResult retried = fullService(dataSource, hasher).provision(adminCommand(code));
		assertEquals("ACTIVE", retried.tenantStatus());
		assertEquals(ProvisioningStepCode.COMPLETED, retried.currentStep());
		assertFalse(retried.adminAlreadyPresent());
		assertTrue(hasher.matches(retried.temporaryPassword().reveal(), passwordHash(schema)));
		assertEquals("1", scalarInSchema(schema, "SELECT count(*)::text FROM user_account"));
	}

	private static void assertPausedAtMigrated(ProvisioningResult result, String schema) {
		assertEquals("PROVISIONING", result.tenantStatus());
		assertEquals(ProvisioningStepCode.SEEDED, result.currentStep());
		assertEquals("RUNNING", result.executionStatus());
		assertEquals(schema, result.schemaName());
		assertTrue(result.temporaryPassword() == null);
	}

	private static TenantAdminWriter adminWriter(DataSource dataSource, PasswordHasher hasher) {
		return new TenantAdminWriter(dataSource, hasher,
				new com.tommy.common.global.security.AccountStatusCache(org.mockito.Mockito.mock(jakarta.persistence.EntityManager.class)));
	}

	private TenantProvisioningService fullService(DataSource dataSource, PasswordHasher hasher) {
		ProvisioningJournal journal = journal(dataSource);
		TenantSchemaMigrator migrator = new FlywayTenantSchemaMigrator(dataSource);
		TenantAdminWriter writer = adminWriter(dataSource, hasher);
		List<ProvisioningStep> steps = List.of(
				new RequestedProvisioningStep(journal),
				new SchemaCreatedProvisioningStep(journal),
				new MigratedProvisioningStep(migrator, journal),
				new SeededProvisioningStep(new TenantSeedRunner(dataSource)),
				new AdminCreatedProvisioningStep(writer),
				new CompletedProvisioningStep(migrator, journal));
		return new TenantProvisioningService(new PgTenantAdvisoryLock(dataSource), journal, directory(), steps);
	}

	private TenantProvisioningService service(DataSource dataSource, TenantSchemaMigrator migrator) {
		ProvisioningJournal journal = journal(dataSource);
		List<ProvisioningStep> steps = List.of(
				new RequestedProvisioningStep(journal),
				new SchemaCreatedProvisioningStep(journal),
				new MigratedProvisioningStep(migrator, journal),
				new com.tommy.common.tenant.provision.SeededProvisioningStep(
						new com.tommy.common.tenant.provision.TenantSeedRunner(dataSource)));
		return new TenantProvisioningService(new PgTenantAdvisoryLock(dataSource), journal, directory(), steps);
	}

	private static ProvisioningJournal journal(DataSource dataSource) {
		return new ProvisioningJournal(new JdbcTemplate(dataSource), new DataSourceTransactionManager(dataSource));
	}

	private static TenantDirectory directory() {
		return new TenantDirectory() {
			@Override
			public Optional<TenantSnapshot> findByTenantCode(String tenantCode) {
				return Optional.empty();
			}

			@Override
			public Optional<TenantSnapshot> findByDomain(String domainName) {
				return Optional.empty();
			}

			@Override
			public void evict(String tenantCode) {
			}
		};
	}

	private DataSource pool() {
		if (pool == null) {
			HikariDataSource dataSource = new HikariDataSource();
			dataSource.setJdbcUrl(System.getenv("MDM_TEST_DB_URL"));
			dataSource.setUsername(System.getenv("MDM_TEST_DB_USERNAME"));
			dataSource.setPassword(System.getenv("MDM_TEST_DB_PASSWORD"));
			dataSource.setMaximumPoolSize(5);
			dataSource.setPoolName("tenant-provision-it");
			pool = dataSource;
		}
		return pool;
	}

	private static ProvisionTenantCommand command(String tenantCode) {
		return new ProvisionTenantCommand(tenantCode, "IT", null, "ko", "Admin", "it-admin@example.com", null);
	}

	private static ProvisionTenantCommand adminCommand(String tenantCode) {
		return new ProvisionTenantCommand(tenantCode, "아이티", null, "ko-KR", "관리자", "Admin.User@Example.com", null);
	}

	private void track(String tenantCode, String schema) {
		assertTrue(schema.matches(SCHEMA_PATTERN));
		tenantCodes.add(tenantCode);
		schemas.add(schema);
	}

	private ProvisioningResult read(String tenantCode) throws Exception {
		try (Connection connection = openConnection();
				PreparedStatement statement = connection.prepareStatement("""
						SELECT t.schema_name, t.tenant_status_code, p.current_step_code, p.execution_status_code
						FROM pmis.tenant t
						JOIN pmis.tenant_provisioning p ON p.tenant_pk = t.tenant_pk
						WHERE t.tenant_code = ?
						""")) {
			statement.setString(1, tenantCode);
			try (ResultSet resultSet = statement.executeQuery()) {
				assertTrue(resultSet.next());
				return new ProvisioningResult(tenantCode, resultSet.getString(1), resultSet.getString(2),
						ProvisioningStepCode.valueOf(resultSet.getString(3)), resultSet.getString(4), null, false, null);
			}
		}
	}

	private String tenantStatus(String tenantCode) throws Exception {
		return scalar("SELECT tenant_status_code FROM pmis.tenant WHERE tenant_code = ?", tenantCode);
	}

	private String currentStep(String tenantCode) throws Exception {
		return scalar("""
				SELECT p.current_step_code
				FROM pmis.tenant_provisioning p
				JOIN pmis.tenant t ON t.tenant_pk = p.tenant_pk
				WHERE t.tenant_code = ?
				""", tenantCode);
	}

	private int retryCount(String tenantCode) throws Exception {
		return Integer.parseInt(scalar("""
				SELECT p.retry_count::text
				FROM pmis.tenant_provisioning p
				JOIN pmis.tenant t ON t.tenant_pk = p.tenant_pk
				WHERE t.tenant_code = ?
				""", tenantCode));
	}

	private String errorMessage(String tenantCode) throws Exception {
		return scalar("""
				SELECT COALESCE(p.error_message, '')
				FROM pmis.tenant_provisioning p
				JOIN pmis.tenant t ON t.tenant_pk = p.tenant_pk
				WHERE t.tenant_code = ?
				""", tenantCode);
	}

	private String schemaVersion(String tenantCode) throws Exception {
		return scalar("SELECT schema_version FROM pmis.tenant WHERE tenant_code = ?", tenantCode);
	}

	private int migrationCount(String tenantCode, String migrationType) throws Exception {
		try (Connection connection = openConnection();
				PreparedStatement statement = connection.prepareStatement("""
						SELECT count(*)
						FROM pmis.tenant_schema_migration m
						JOIN pmis.tenant t ON t.tenant_pk = m.tenant_pk
						WHERE t.tenant_code = ? AND m.migration_type_code = ?
						""")) {
			statement.setString(1, tenantCode);
			statement.setString(2, migrationType);
			try (ResultSet resultSet = statement.executeQuery()) {
				assertTrue(resultSet.next());
				return resultSet.getInt(1);
			}
		}
	}

	private int count(String schema, String table) throws Exception {
		return Integer.parseInt(scalarInSchema(schema, "SELECT count(*)::text FROM " + table));
	}

	private String scalarInSchema(String schema, String sql) throws Exception {
		if (!schema.matches(SCHEMA_PATTERN)) {
			throw new IllegalArgumentException("테스트 스키마명이 아닙니다.");
		}
		try (Connection connection = openConnection();
				Statement statement = connection.createStatement()) {
			statement.execute("SET search_path TO " + schema);
			try (ResultSet resultSet = statement.executeQuery(sql)) {
				assertTrue(resultSet.next());
				return resultSet.getString(1);
			}
		}
	}

	private int tableCount(String schema) throws Exception {
		try (Connection connection = openConnection();
				PreparedStatement statement = connection.prepareStatement("""
						SELECT count(*)
						FROM pg_class c
						JOIN pg_namespace n ON n.oid = c.relnamespace
						WHERE n.nspname = ? AND c.relkind = 'r' AND c.relname <> 'flyway_schema_history'
						""")) {
			statement.setString(1, schema);
			try (ResultSet resultSet = statement.executeQuery()) {
				assertTrue(resultSet.next());
				return resultSet.getInt(1);
			}
		}
	}

	private boolean flywayHistoryExists(String schema) throws Exception {
		try (Connection connection = openConnection();
				PreparedStatement statement = connection.prepareStatement("SELECT to_regclass(?)")) {
			statement.setString(1, schema + ".flyway_schema_history");
			try (ResultSet resultSet = statement.executeQuery()) {
				return resultSet.next() && resultSet.getString(1) != null;
			}
		}
	}

	private String searchPath() throws Exception {
		try (Connection connection = pool.getConnection();
				Statement statement = connection.createStatement();
				ResultSet resultSet = statement.executeQuery("SHOW search_path")) {
			assertTrue(resultSet.next());
			return resultSet.getString(1);
		}
	}

	private String scalar(String sql, String tenantCode) throws Exception {
		try (Connection connection = openConnection();
				PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setString(1, tenantCode);
			try (ResultSet resultSet = statement.executeQuery()) {
				assertTrue(resultSet.next());
				return resultSet.getString(1);
			}
		}
	}

	private static String statusOf(TenantUpgradeService.UpgradeBatchResult result, String tenantCode) {
		return result.results().stream()
				.filter(item -> tenantCode.equals(item.tenantCode()))
				.findFirst()
				.orElseThrow()
				.executionStatus();
	}

	private static void insertTenant(Connection connection, String tenantCode, String schema, String status) throws Exception {
		try (PreparedStatement statement = connection.prepareStatement("""
				INSERT INTO pmis.tenant (
				    tenant_code, tenant_name, schema_name, tenant_status_code, default_language_code)
				VALUES (?, 'upgrade', ?, ?, 'ko')
				""")) {
			statement.setString(1, tenantCode);
			statement.setString(2, schema);
			statement.setString(3, status);
			statement.executeUpdate();
		}
	}

	@Test
	void leavesANonTestActiveTenantUntouched() throws Exception {
		org.junit.jupiter.api.Assumptions.assumeTrue(TestcontainersConfiguration.isConfigured());
		String keepSuffix = suffix();
		String keepCode = "zz" + keepSuffix;
		String keepSchema = "t_zz_" + keepSuffix;
		String suffix = suffix();
		String code = "it-" + suffix;
		String schema = "t_it_" + suffix;
		track(code, schema);
		try (Connection connection = openConnection()) {
			IntegrationDatabase.requirePmis(connection);
			createNamedSchema(connection, keepSchema, "^t_zz_[a-z0-9]{16}$");
			createSchema(connection, schema);
			insertTenant(connection, keepCode, keepSchema, "ACTIVE");
			insertTenant(connection, code, schema, "ACTIVE");
		}
		String before = tenantSnapshot(keepCode);
		DataSource dataSource = pool();
		TenantUpgradeService service = new TenantUpgradeService(new PgTenantAdvisoryLock(dataSource), journal(dataSource),
				new FlywayTenantSchemaMigrator(dataSource), new com.tommy.common.tenant.provision.TenantSeedRunner(dataSource), directory());
		try {
			service.upgradeTenants(List.of(code), null);
			assertEquals(before, tenantSnapshot(keepCode));
			assertEquals(0, migrationCount(keepCode, "UPGRADE"));
			assertEquals("ACTIVE", tenantStatus(keepCode));
		}
		finally {
			try (Connection connection = openConnection()) {
				IntegrationDatabase.requirePmis(connection);
				deleteTenant(connection, keepCode);
				dropNamedSchema(connection, keepSchema, "^t_zz_[a-z0-9]{16}$");
			}
		}
	}

	private static void createSchema(Connection connection, String schema) throws Exception {
		if (!schema.matches(SCHEMA_PATTERN)) {
			throw new IllegalArgumentException("테스트 스키마명이 아닙니다.");
		}
		try (Statement statement = connection.createStatement()) {
			statement.execute("CREATE SCHEMA " + schema);
		}
	}

	private static void deleteTenant(Connection connection, String tenantCode) throws Exception {
		Long tenantPk = null;
		try (PreparedStatement statement = connection.prepareStatement(
				"SELECT tenant_pk FROM pmis.tenant WHERE tenant_code = ?")) {
			statement.setString(1, tenantCode);
			try (ResultSet resultSet = statement.executeQuery()) {
				if (resultSet.next()) {
					tenantPk = resultSet.getLong(1);
				}
			}
		}
		if (tenantPk == null) {
			return;
		}
		deleteByTenant(connection, "tenant_schema_migration", tenantPk);
		deleteByTenant(connection, "tenant_status_history", tenantPk);
		deleteByTenant(connection, "tenant_provisioning", tenantPk);
		deleteByTenant(connection, "tenant_domain", tenantPk);
		try (PreparedStatement statement = connection.prepareStatement("DELETE FROM pmis.tenant WHERE tenant_pk = ?")) {
			statement.setLong(1, tenantPk);
			statement.executeUpdate();
		}
	}

	private static void deleteByTenant(Connection connection, String table, long tenantPk) throws Exception {
		try (PreparedStatement statement = connection.prepareStatement(
				"DELETE FROM pmis." + table + " WHERE tenant_pk = ?")) {
			statement.setLong(1, tenantPk);
			statement.executeUpdate();
		}
	}

	private static void createNamedSchema(Connection connection, String schema, String pattern) throws Exception {
		if (!schema.matches(pattern)) {
			throw new IllegalArgumentException("테스트 스키마명이 아닙니다.");
		}
		try (Statement statement = connection.createStatement()) {
			statement.execute("CREATE SCHEMA " + schema);
		}
	}

	private static void dropNamedSchema(Connection connection, String schema, String pattern) throws Exception {
		if (!schema.matches(pattern)) {
			throw new IllegalArgumentException("테스트 스키마명이 아닙니다.");
		}
		try (Statement statement = connection.createStatement()) {
			statement.execute("DROP SCHEMA IF EXISTS " + schema + " CASCADE");
		}
	}

	private static void removeItPrefixed(Connection connection) throws Exception {
		List<String> codes = new ArrayList<>();
		List<String> schemas = new ArrayList<>();
		try (PreparedStatement statement = connection.prepareStatement("""
				SELECT tenant_code, schema_name FROM pmis.tenant
				WHERE tenant_code ~ '^it-[a-z0-9]{16}$' AND schema_name ~ '^t_it_[a-z0-9]{16}$'
				""")) {
			try (ResultSet resultSet = statement.executeQuery()) {
				while (resultSet.next()) {
					codes.add(resultSet.getString(1));
					schemas.add(resultSet.getString(2));
				}
			}
		}
		for (String tenantCode : codes) {
			deleteTenant(connection, tenantCode);
		}
		for (String schema : schemas) {
			dropSchema(connection, schema);
		}
		try (Statement statement = connection.createStatement();
				ResultSet resultSet = statement.executeQuery("""
						SELECT nspname FROM pg_namespace WHERE nspname ~ '^t_it_[a-z0-9]{16}$'
						""")) {
			List<String> leftover = new ArrayList<>();
			while (resultSet.next()) {
				leftover.add(resultSet.getString(1));
			}
			for (String schema : leftover) {
				dropSchema(connection, schema);
			}
		}
	}

	private String tenantSnapshot(String tenantCode) throws Exception {
		try (Connection connection = openConnection();
				PreparedStatement statement = connection.prepareStatement("""
						SELECT tenant_status_code, schema_name, COALESCE(schema_version, ''), tenant_name
						FROM pmis.tenant WHERE tenant_code = ?
						""")) {
			statement.setString(1, tenantCode);
			try (ResultSet resultSet = statement.executeQuery()) {
				assertTrue(resultSet.next());
				return resultSet.getString(1) + "|" + resultSet.getString(2) + "|" + resultSet.getString(3) + "|"
						+ resultSet.getString(4);
			}
		}
	}

	private static void dropSchema(Connection connection, String schema) throws Exception {
		if (!schema.matches(SCHEMA_PATTERN)) {
			throw new IllegalArgumentException("테스트 스키마명이 아닙니다.");
		}
		try (Statement statement = connection.createStatement()) {
			statement.execute("DROP SCHEMA IF EXISTS " + schema + " CASCADE");
		}
	}

	private static Connection openConnection() throws Exception {
		return DriverManager.getConnection(
				System.getenv("MDM_TEST_DB_URL"),
				System.getenv("MDM_TEST_DB_USERNAME"),
				System.getenv("MDM_TEST_DB_PASSWORD"));
	}

	private static void await(CountDownLatch latch) {
		try {
			if (!latch.await(30, TimeUnit.SECONDS)) {
				throw new IllegalStateException("대기가 끝나지 않았습니다.");
			}
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("대기가 중단되었습니다.");
		}
	}

	private String policyDays(String schema) throws Exception {
		return scalarInSchema(schema, """
				SELECT c.ext_attr1
				FROM common_code c
				JOIN code_group g ON g.code_group_pk = c.code_group_pk
				WHERE g.code_group_code = 'SECURITY_POLICY' AND c.code_value = 'TEMP_PASSWORD_VALID_DAYS'
				""");
	}

	private void executeInSchema(String schema, String sql) throws Exception {
		if (!schema.matches(SCHEMA_PATTERN)) {
			throw new IllegalArgumentException("테스트 스키마명이 아닙니다.");
		}
		try (Connection connection = openConnection()) {
			IntegrationDatabase.requirePmis(connection);
			try (Statement statement = connection.createStatement()) {
				statement.execute("SET search_path TO " + schema);
				statement.execute(sql);
			}
		}
	}

	private String passwordHash(String schema) throws Exception {
		return scalarInSchema(schema, "SELECT password_hash FROM user_account");
	}

	private String plaintextHits(String schema, String plain) throws Exception {
		if (!schema.matches(SCHEMA_PATTERN)) {
			throw new IllegalArgumentException("테스트 스키마명이 아닙니다.");
		}
		int found = 0;
		try (Connection connection = openConnection()) {
			IntegrationDatabase.requirePmis(connection);
			try (Statement statement = connection.createStatement()) {
				statement.execute("SET search_path TO " + schema);
			}
			for (String sql : List.of(
					"SELECT count(*) FROM user_account WHERE position(? in coalesce(password_hash, '')) > 0",
					"SELECT count(*) FROM user_password_history WHERE position(? in password_hash) > 0",
					"""
							SELECT count(*) FROM pmis.tenant_provisioning
							WHERE tenant_pk = (SELECT tenant_pk FROM pmis.tenant WHERE schema_name = ?)
							  AND position(? in coalesce(error_message, '')) > 0
							""")) {
				try (PreparedStatement statement = connection.prepareStatement(sql)) {
					if (sql.contains("schema_name")) {
						statement.setString(1, schema);
						statement.setString(2, plain);
					}
					else {
						statement.setString(1, plain);
					}
				try (ResultSet resultSet = statement.executeQuery()) {
					assertTrue(resultSet.next());
					found += resultSet.getInt(1);
				}
				}
			}
		}
		return Integer.toString(found);
	}

	private void rewindToSeeded(String tenantCode) throws Exception {
		try (Connection connection = openConnection()) {
			IntegrationDatabase.requirePmis(connection);
			try (PreparedStatement statement = connection.prepareStatement("""
					UPDATE pmis.tenant SET tenant_status_code = 'PROVISIONING' WHERE tenant_code = ?
					""")) {
				statement.setString(1, tenantCode);
				statement.executeUpdate();
			}
			try (PreparedStatement statement = connection.prepareStatement("""
					UPDATE pmis.tenant_provisioning
					SET current_step_code = 'SEEDED', execution_status_code = 'RUNNING', completed_datetime = NULL
					WHERE tenant_pk = (SELECT tenant_pk FROM pmis.tenant WHERE tenant_code = ?)
					""")) {
				statement.setString(1, tenantCode);
				statement.executeUpdate();
			}
		}
	}

	private static String suffix() {
		return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
	}

}
