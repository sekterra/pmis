package com.tommy.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.tommy.common.devadmin.DevSetAdminRules;
import com.tommy.common.devadmin.DevSetAdminService;
import com.tommy.common.global.multitenancy.HeaderTenantResolver;
import com.tommy.common.global.multitenancy.TenantContext;
import com.tommy.common.global.security.PasswordHasher;
import com.tommy.common.tenant.provision.ProvisionTenantCommand;
import com.tommy.common.tenant.provision.TenantProvisioningService;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class DevSetAdminIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private TenantProvisioningService provisioningService;

	@Autowired
	private DevSetAdminService devSetAdminService;

	@Autowired
	private PasswordHasher passwordHasher;

	@Autowired
	private DataSource dataSource;

	private final List<String> tenantCodes = new ArrayList<>();
	private final List<String> schemas = new ArrayList<>();

	@DynamicPropertySource
	static void datasource(DynamicPropertyRegistry registry) {
		TestcontainersConfiguration.registerDatasource(registry);
		TestJwtKey.register(registry);
	}

	@AfterEach
	void cleanup() throws Exception {
		TenantContext.clear();
		try (Connection connection = dataSource.getConnection()) {
			IntegrationDatabase.requirePmis(connection);
			for (String tenantCode : tenantCodes) {
				deleteTenant(connection, tenantCode);
			}
			for (String schema : schemas) {
				if (schema.matches("^t_it_[a-z0-9]{16}$")) {
					try (Statement statement = connection.createStatement()) {
						statement.execute("DROP SCHEMA IF EXISTS " + schema + " CASCADE");
					}
				}
			}
		}
	}

	@Test
	void createsShortPasswordAccountAndUpdatesWithoutGrowingTheCount() throws Exception {
		String outside = outsideTenants();
		String platformAdmins = scalar(null, "SELECT count(*)::text FROM pmis.platform_admin");
		String code = provision();
		provisioningService.provision(command(code));
		String schema = schema(code);
		String login = login();
		String password = letters(6);
		int before = Integer.parseInt(scalar(schema, "SELECT count(*)::text FROM user_account"));
		ListAppender<ILoggingEvent> logs = attach();
		String line;
		try {
			line = devSetAdminService.apply(DevSetAdminRules.parse("tenant", code, login, password));
		}
		finally {
			detach(logs);
		}
		assertFalse(line.contains(password));
		assertFalse(logText(logs).contains(password));
		assertNull(TenantContext.schema());
		assertFalse(TenantContext.inRequest());
		assertEquals(before + 1, Integer.parseInt(scalar(schema, "SELECT count(*)::text FROM user_account")));
		assertEquals("NORMAL", scalar(schema, "SELECT account_type_code FROM user_account WHERE login_id = '" + login + "'"));
		assertEquals("Y", scalar(schema, "SELECT CASE WHEN person_pk IS NULL THEN 'Y' ELSE 'N' END FROM user_account WHERE login_id = '" + login + "'"));
		assertEquals("N", scalar(schema, "SELECT btrim(must_change_password_yn) FROM user_account WHERE login_id = '" + login + "'"));
		assertEquals("RESET", scalar(schema, "SELECT change_type_code FROM user_password_history h JOIN user_account u ON u.user_pk = h.user_pk WHERE u.login_id = '"
				+ login + "' ORDER BY h.password_history_pk DESC LIMIT 1"));
		assertTrue(passwordHasher.matches(password, scalar(schema, "SELECT password_hash FROM user_account WHERE login_id = '" + login + "'")));
		mockMvc.perform(post("/api/mdm/v1/auth/login").header(HeaderTenantResolver.HEADER, code)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"loginId\":\"" + login + "\",\"password\":\"" + password + "\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.passwordChangeRequired").value(false));
		execute(schema, """
				UPDATE user_account
				SET user_status_code = 'LOCKED', login_fail_count = 4, locked_datetime = CURRENT_TIMESTAMP, use_yn = 'N'
				WHERE login_id = '%s'
				""".formatted(login));
		execute(schema, """
				UPDATE user_role
				SET use_yn = 'N', valid_to_date = CURRENT_DATE - 1
				WHERE user_pk = (SELECT user_pk FROM user_account WHERE login_id = '%s')
				""".formatted(login));
		execute(schema, """
				INSERT INTO user_refresh_token (user_pk, token_hash, token_family_id, issued_datetime, expire_datetime)
				SELECT user_pk, 'it-refresh', 'it-family', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '1 day'
				FROM user_account WHERE login_id = '%s'
				""".formatted(login));
		String next = letters(6);
		int lockedCount = Integer.parseInt(scalar(schema, "SELECT count(*)::text FROM user_account"));
		ListAppender<ILoggingEvent> again = attach();
		String updated;
		try {
			updated = devSetAdminService.apply(DevSetAdminRules.parse("tenant", code, login.toUpperCase(), next));
		}
		finally {
			detach(again);
		}
		assertFalse(updated.contains(next));
		assertFalse(logText(again).contains(next));
		assertEquals(lockedCount, Integer.parseInt(scalar(schema, "SELECT count(*)::text FROM user_account")));
		assertEquals("ACTIVE", scalar(schema, "SELECT user_status_code FROM user_account WHERE login_id = '" + login + "'"));
		assertEquals("0", scalar(schema, "SELECT login_fail_count::text FROM user_account WHERE login_id = '" + login + "'"));
		assertEquals("Y", scalar(schema, "SELECT btrim(use_yn) FROM user_account WHERE login_id = '" + login + "'"));
		assertEquals("N", scalar(schema, "SELECT CASE WHEN locked_datetime IS NULL THEN 'N' ELSE 'Y' END FROM user_account WHERE login_id = '" + login + "'"));
		assertEquals("Y", scalar(schema, """
				SELECT btrim(ur.use_yn)
				FROM user_role ur
				JOIN role r ON r.role_pk = ur.role_pk
				JOIN user_account u ON u.user_pk = ur.user_pk
				WHERE r.role_code = 'TENANT_ADMIN' AND u.login_id = '%s'
				""".formatted(login)));
		assertEquals("Y", scalar(schema, """
				SELECT CASE WHEN ur.valid_to_date IS NULL THEN 'Y' ELSE 'N' END
				FROM user_role ur
				JOIN role r ON r.role_pk = ur.role_pk
				JOIN user_account u ON u.user_pk = ur.user_pk
				WHERE r.role_code = 'TENANT_ADMIN' AND u.login_id = '%s'
				""".formatted(login)));
		assertEquals("ADMIN", scalar(schema, "SELECT revoke_reason_code FROM user_refresh_token WHERE token_hash = 'it-refresh'"));
		assertTrue(passwordHasher.matches(next, scalar(schema, "SELECT password_hash FROM user_account WHERE login_id = '" + login + "'")));
		mockMvc.perform(post("/api/mdm/v1/auth/login").header(HeaderTenantResolver.HEADER, code)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"loginId\":\"" + login + "\",\"password\":\"" + next + "\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.passwordChangeRequired").value(false));
		assertEquals(outside, outsideTenants());
		assertEquals(platformAdmins, scalar(null, "SELECT count(*)::text FROM pmis.platform_admin"));
	}

	@Test
	void rejectsOmittedTenantCodeUnlessExactlyOneActiveTenant() throws Exception {
		String outside = outsideTenants();
		String platformAdmins = scalar(null, "SELECT count(*)::text FROM pmis.platform_admin");
		int active = Integer.parseInt(scalar(null, """
				SELECT count(*)::text FROM pmis.tenant
				WHERE tenant_status_code = 'ACTIVE' AND btrim(use_yn) = 'Y'
				"""));
		if (active == 1) {
			assertEquals(outside, outsideTenants());
			return;
		}
		String password = letters(6);
		IllegalStateException rejected = assertThrows(IllegalStateException.class,
				() -> devSetAdminService.apply(DevSetAdminRules.parse("tenant", " ", login(), password)));
		assertFalse(rejected.getMessage().contains(password));
		if (active == 0) {
			assertEquals("ACTIVE 테넌트가 없습니다.", rejected.getMessage());
		}
		else {
			assertTrue(rejected.getMessage().startsWith("ACTIVE 테넌트가 " + active + "개입니다:"));
		}
		assertNull(TenantContext.schema());
		assertEquals(outside, outsideTenants());
		assertEquals(platformAdmins, scalar(null, "SELECT count(*)::text FROM pmis.platform_admin"));
	}

	@Test
	void rejectsUnknownAndInactiveTenantsWithoutChanges() throws Exception {
		String outside = outsideTenants();
		String missing = "it-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
		String password = letters(6);
		assertThrows(IllegalStateException.class,
				() -> devSetAdminService.apply(DevSetAdminRules.parse("tenant", missing, login(), password)));
		assertNull(TenantContext.schema());
		assertEquals("0", scalar(null, "SELECT count(*)::text FROM pmis.tenant WHERE tenant_code = '" + missing + "'"));
		String code = provision();
		provisioningService.provision(command(code));
		String schema = schema(code);
		String hash = scalar(schema, "SELECT password_hash FROM user_account WHERE login_id = 'admin@example.com'");
		String users = scalar(schema, "SELECT count(*)::text FROM user_account");
		execute(null, "UPDATE pmis.tenant SET tenant_status_code = 'SUSPENDED' WHERE tenant_code = '" + code + "'");
		IllegalStateException rejected = assertThrows(IllegalStateException.class,
				() -> devSetAdminService.apply(DevSetAdminRules.parse("tenant", code, login(), password)));
		assertFalse(rejected.getMessage().contains(password));
		assertNull(TenantContext.schema());
		assertEquals(users, scalar(schema, "SELECT count(*)::text FROM user_account"));
		assertEquals(hash, scalar(schema, "SELECT password_hash FROM user_account WHERE login_id = 'admin@example.com'"));
		assertEquals(outside, outsideTenants());
	}

	private String provision() {
		String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
		String tenantCode = "it-" + suffix;
		tenantCodes.add(tenantCode);
		schemas.add(schema(tenantCode));
		return tenantCode;
	}

	private static ProvisionTenantCommand command(String tenantCode) {
		return new ProvisionTenantCommand(tenantCode, "아이티", null, "ko-KR", "관리자", "admin@example.com", null);
	}

	private static String schema(String tenantCode) {
		return "t_" + tenantCode.replace('-', '_');
	}

	private static String login() {
		return "it" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
	}

	private static String letters(int length) {
		SecureRandom random = new SecureRandom();
		char[] chars = new char[length];
		for (int index = 0; index < length; index++) {
			chars[index] = (char) ('g' + random.nextInt(20));
		}
		return new String(chars);
	}

	private static ListAppender<ILoggingEvent> attach() {
		Logger root = (Logger) org.slf4j.LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
		ListAppender<ILoggingEvent> logs = new ListAppender<>();
		logs.start();
		root.addAppender(logs);
		return logs;
	}

	private static void detach(ListAppender<ILoggingEvent> logs) {
		Logger root = (Logger) org.slf4j.LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
		root.detachAppender(logs);
	}

	private static String logText(ListAppender<ILoggingEvent> logs) {
		StringBuilder text = new StringBuilder();
		for (ILoggingEvent event : logs.list) {
			text.append(event.getFormattedMessage());
		}
		return text.toString();
	}

	private String outsideTenants() throws Exception {
		return scalar(null, """
				SELECT coalesce(md5(string_agg(tenant_pk::text || tenant_status_code || schema_name || btrim(use_yn), ',' ORDER BY tenant_pk)), '')
				FROM pmis.tenant
				WHERE tenant_code NOT LIKE 'it-%'
				""");
	}

	private String scalar(String schema, String sql) throws Exception {
		try (Connection connection = dataSource.getConnection()) {
			IntegrationDatabase.requirePmis(connection);
			try (Statement statement = connection.createStatement()) {
				if (schema != null) {
					if (!schema.matches("^t_it_[a-z0-9]{16}$")) {
						throw new IllegalStateException("테스트 스키마가 아닙니다.");
					}
					statement.execute("SET search_path TO " + schema);
				}
				try (ResultSet resultSet = statement.executeQuery(sql)) {
					resultSet.next();
					return resultSet.getString(1);
				}
			}
		}
	}

	private void execute(String schema, String sql) throws Exception {
		try (Connection connection = dataSource.getConnection()) {
			IntegrationDatabase.requirePmis(connection);
			try (Statement statement = connection.createStatement()) {
				if (schema != null) {
					if (!schema.matches("^t_it_[a-z0-9]{16}$")) {
						throw new IllegalStateException("테스트 스키마가 아닙니다.");
					}
					statement.execute("SET search_path TO " + schema);
				}
				statement.execute(sql);
			}
		}
	}

	private void deleteTenant(Connection connection, String tenantCode) throws Exception {
		Long tenantPk = null;
		try (PreparedStatement statement = connection.prepareStatement("SELECT tenant_pk FROM pmis.tenant WHERE tenant_code = ?")) {
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
		for (String table : List.of("tenant_schema_migration", "tenant_status_history", "tenant_provisioning", "tenant_domain")) {
			try (PreparedStatement statement = connection.prepareStatement("DELETE FROM pmis." + table + " WHERE tenant_pk = ?")) {
				statement.setLong(1, tenantPk);
				statement.executeUpdate();
			}
		}
		try (PreparedStatement statement = connection.prepareStatement("DELETE FROM pmis.tenant WHERE tenant_pk = ?")) {
			statement.setLong(1, tenantPk);
			statement.executeUpdate();
		}
	}

}
