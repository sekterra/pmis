package com.tommy.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.sql.DataSource;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
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
import org.springframework.test.web.servlet.MvcResult;

import com.tommy.common.auth.RefreshCookies;
import com.tommy.common.auth.RefreshTokenValues;
import com.tommy.common.global.security.PasswordHasher;
import com.tommy.common.platform.PlatformAdmin;
import com.tommy.common.platform.PlatformAdminBootstrapService;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.servlet.http.Cookie;

@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class PlatformApiIntegrationTest {

	private static final String PASSWORD = "N3w-Platform-Pass1";

	private static final String NEXT = "N3w-Platform-Pass2";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private DataSource dataSource;

	@Autowired
	private PasswordHasher passwordHasher;

	@Autowired
	private PlatformAdminBootstrapService bootstrapService;

	@Autowired
	private com.tommy.common.global.security.JwtTokenService jwtTokenService;

	private final List<String> adminLogins = new ArrayList<>();

	private final List<String> tenantCodes = new ArrayList<>();

	private final List<String> schemas = new ArrayList<>();

	@DynamicPropertySource
	static void datasource(DynamicPropertyRegistry registry) {
		TestcontainersConfiguration.registerDatasource(registry);
		registry.add("mdm.security.platform.max-login-fail-count", () -> "2");
	}

	@AfterEach
	void cleanup() throws Exception {
		try (Connection connection = dataSource.getConnection()) {
			IntegrationDatabase.requirePmis(connection);
			for (String login : adminLogins) {
				deleteAdmin(connection, login);
			}
			try (PreparedStatement statement = connection.prepareStatement(
					"DELETE FROM pmis.platform_admin_login_history WHERE login_id_input = 'missing-ops@example.com'")) {
				statement.executeUpdate();
			}
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
	void signsInPlatformAndRejectsTheOtherAudience() throws Exception {
		String login = insertAdmin(PASSWORD);
		MvcResult first = login(login, PASSWORD);
		assertEquals("true", json(first, "passwordChangeRequired"));
		mockMvc.perform(get("/api/platform/v1/tenants").header("Authorization", "Bearer " + json(first, "accessToken")))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.errorCode").value("AUTH.PASSWORD_CHANGE_REQUIRED"));
		MvcResult changed = mockMvc.perform(post("/api/platform/v1/auth/password/change")
				.header("Authorization", "Bearer " + json(first, "accessToken"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"currentPassword\":\"" + PASSWORD + "\",\"newPassword\":\"" + NEXT + "\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.passwordChangeRequired").value(false))
				.andReturn();
		mockMvc.perform(get("/api/platform/v1/auth/me").header("Authorization", "Bearer " + json(changed, "accessToken")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.loginId").value(login))
				.andExpect(jsonPath("$.data.roleCodes").doesNotExist());
		MvcResult rotated = mockMvc.perform(post("/api/platform/v1/auth/refresh").header("X-MDM-Request", "1")
				.cookie(new Cookie(RefreshCookies.PLATFORM_NAME, cookie(changed))))
				.andExpect(status().isOk()).andReturn();
		mockMvc.perform(post("/api/platform/v1/auth/refresh").header("X-MDM-Request", "1")
				.cookie(new Cookie(RefreshCookies.PLATFORM_NAME, cookie(changed))))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.errorCode").value("AUTH.REFRESH_IN_PROGRESS"));
		execute("UPDATE pmis.platform_admin_refresh_token SET revoked_datetime = TIMESTAMP '2020-01-01 00:00:00' WHERE token_hash = '"
				+ RefreshTokenValues.hash(cookie(changed)) + "'");
		mockMvc.perform(post("/api/platform/v1/auth/refresh").header("X-MDM-Request", "1")
				.cookie(new Cookie(RefreshCookies.PLATFORM_NAME, cookie(changed))))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.errorCode").value("AUTH.TOKEN_REUSED"));
		mockMvc.perform(post("/api/platform/v1/auth/refresh").cookie(new Cookie(RefreshCookies.PLATFORM_NAME, cookie(rotated))))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.errorCode").value("AUTH.CSRF"));
		MvcResult again = login(login, NEXT);
		mockMvc.perform(post("/api/platform/v1/auth/logout").header("Authorization", "Bearer " + json(again, "accessToken"))
				.header("X-MDM-Request", "1").cookie(new Cookie(RefreshCookies.PLATFORM_NAME, cookie(again))))
				.andExpect(status().isOk())
				.andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("Max-Age=0")));
		mockMvc.perform(post("/api/platform/v1/auth/refresh").header("X-MDM-Request", "1")
				.cookie(new Cookie(RefreshCookies.PLATFORM_NAME, cookie(again))))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(get("/api/platform/v1/tenants").header("Authorization", "Bearer " + jwtTokenService.issue("1",
				com.tommy.common.global.security.JwtTokenService.TENANT_AUDIENCE,
				com.tommy.common.global.security.JwtTokenService.TENANT_TYPE, "it-other", false, java.time.Duration.ofMinutes(5))))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.errorCode").value("AUTH.TOKEN_REJECTED"));
		mockMvc.perform(get("/api/mdm/v1/code-groups").header("Authorization", "Bearer " + json(login(login, NEXT), "accessToken")))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.errorCode").value("AUTH.TOKEN_REJECTED"));
		mockMvc.perform(post("/api/platform/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"loginId\":\"missing-ops@example.com\",\"password\":\"wrong-pass-1\"}")).andExpect(status().isUnauthorized());
		mockMvc.perform(post("/api/platform/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"loginId\":\"" + login + "\",\"password\":\"wrong-pass-1\"}")).andExpect(status().isUnauthorized());
		mockMvc.perform(post("/api/platform/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"loginId\":\"" + login + "\",\"password\":\"wrong-pass-1\"}")).andExpect(status().isUnauthorized());
		assertEquals("LOCKED", scalar("SELECT admin_status_code FROM pmis.platform_admin WHERE login_id = '" + login + "'"));
		mockMvc.perform(post("/api/platform/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"loginId\":\"" + login + "\",\"password\":\"" + NEXT + "\"}")).andExpect(status().isUnauthorized());
		execute("UPDATE pmis.platform_admin SET locked_datetime = TIMESTAMP '2020-01-01 00:00:00' WHERE login_id = '" + login + "'");
		mockMvc.perform(post("/api/platform/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"loginId\":\"" + login + "\",\"password\":\"" + NEXT + "\"}"))
				.andExpect(status().isOk());
		validatePlatformAdmin();
	}

	@Test
	void managesTenantsAndKeepsSecretsOutOfLogs() throws Exception {
		String login = insertAdmin(NEXT);
		execute("UPDATE pmis.platform_admin SET must_change_password_yn = 'N', password_expire_datetime = CURRENT_TIMESTAMP + INTERVAL '30 days' WHERE login_id = '"
				+ login + "'");
		String access = json(login(login, NEXT), "accessToken");
		String code = "it-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
		String domain = code + ".example.test";
		tenantCodes.add(code);
		schemas.add("t_" + code.replace('-', '_'));
		Logger root = (Logger) org.slf4j.LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
		ListAppender<ILoggingEvent> logs = new ListAppender<>();
		logs.start();
		root.addAppender(logs);
		MvcResult created;
		try {
			created = mockMvc.perform(post("/api/platform/v1/tenants").header("Authorization", "Bearer " + access)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{"tenantCode":"%s","tenantName":"아이티","defaultLanguageCode":"ko-KR","timezoneId":"Asia/Seoul","contractStartDate":"2026-01-01","adminName":"관리자","adminEmail":"admin@example.com","domains":["%s"]}
							""".formatted(code, domain)))
					.andExpect(status().isOk())
					.andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
					.andExpect(jsonPath("$.data.tenantStatus").value("ACTIVE"))
					.andReturn();
		}
		finally {
			root.detachAppender(logs);
		}
		String temporary = json(created, "temporaryPassword");
		assertFalse(logText(logs).contains(temporary));
		assertFalse(logText(logs).contains(access));
		mockMvc.perform(post("/api/mdm/v1/auth/login").with(request -> {
			request.setServerName(domain);
			return request;
		}).contentType(MediaType.APPLICATION_JSON).content("{\"loginId\":\"admin@example.com\",\"password\":\"" + temporary + "\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.passwordChangeRequired").value(true));
		mockMvc.perform(post("/api/platform/v1/tenants").header("Authorization", "Bearer " + access).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"tenantCode":"%s","tenantName":"아이티","defaultLanguageCode":"ko-KR","adminName":"관리자","adminEmail":"admin@example.com","domains":["other-%s"]}
						""".formatted(code, domain)))
				.andExpect(status().isConflict());
		mockMvc.perform(post("/api/platform/v1/tenants/" + code + "/domains").header("Authorization", "Bearer " + access)
				.contentType(MediaType.APPLICATION_JSON).content("{\"domainName\":\"" + domain + "\"}"))
				.andExpect(status().isConflict());
		String extra = "extra-" + domain;
		mockMvc.perform(post("/api/platform/v1/tenants/" + code + "/domains").header("Authorization", "Bearer " + access)
				.contentType(MediaType.APPLICATION_JSON).content("{\"domainName\":\"" + extra + "\"}"))
				.andExpect(status().isOk());
		mockMvc.perform(delete("/api/platform/v1/tenants/" + code + "/domains/" + domain).header("Authorization", "Bearer " + access))
				.andExpect(status().isOk());
		assertEquals("Y", scalar("SELECT btrim(primary_yn) FROM pmis.tenant_domain WHERE domain_name = '" + extra + "'"));
		mockMvc.perform(delete("/api/platform/v1/tenants/" + code + "/domains/" + extra).header("Authorization", "Bearer " + access))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.errorCode").value("TENANT.LAST_DOMAIN"));
		mockMvc.perform(post("/api/platform/v1/tenants/" + code + "/suspend").header("Authorization", "Bearer " + access)
				.contentType(MediaType.APPLICATION_JSON).content("{\"changeReason\":\"점검\"}")).andExpect(status().isOk());
		mockMvc.perform(post("/api/mdm/v1/auth/login").with(request -> {
			request.setServerName(extra);
			return request;
		}).contentType(MediaType.APPLICATION_JSON).content("{\"loginId\":\"admin@example.com\",\"password\":\"" + temporary + "\"}"))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.errorCode").value("TENANT.INACTIVE"));
		mockMvc.perform(post("/api/platform/v1/tenants/" + code + "/resume").header("Authorization", "Bearer " + access)
				.contentType(MediaType.APPLICATION_JSON).content("{\"changeReason\":\"재개\"}")).andExpect(status().isOk());
		mockMvc.perform(post("/api/mdm/v1/auth/login").with(request -> {
			request.setServerName(extra);
			return request;
		}).contentType(MediaType.APPLICATION_JSON).content("{\"loginId\":\"admin@example.com\",\"password\":\"" + temporary + "\"}"))
				.andExpect(status().isOk());
		MvcResult reissued = mockMvc.perform(post("/api/platform/v1/tenants/" + code + "/admin-password/reissue")
				.header("Authorization", "Bearer " + access))
				.andExpect(status().isOk())
				.andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
				.andReturn();
		mockMvc.perform(post("/api/mdm/v1/auth/login").with(request -> {
			request.setServerName(extra);
			return request;
		}).contentType(MediaType.APPLICATION_JSON).content("{\"loginId\":\"admin@example.com\",\"password\":\"" + temporary + "\"}"))
				.andExpect(status().isUnauthorized());
		assertFalse(logText(logs).contains(json(reissued, "temporaryPassword")));
		execute("UPDATE pmis.tenant SET tenant_status_code = 'FAILED' WHERE tenant_code = '" + code + "'");
		execute("""
				UPDATE pmis.tenant_provisioning SET current_step_code = 'SEEDED', execution_status_code = 'FAILED'
				WHERE tenant_pk = (SELECT tenant_pk FROM pmis.tenant WHERE tenant_code = '%s')
				""".formatted(code));
		mockMvc.perform(post("/api/platform/v1/tenants/" + code + "/provisioning/retry").header("Authorization", "Bearer " + access))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.tenantStatus").value("ACTIVE"));
		mockMvc.perform(post("/api/platform/v1/tenants/" + code + "/upgrade").header("Authorization", "Bearer " + access))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.executionStatus").value("SUCCESS"));
		mockMvc.perform(post("/api/platform/v1/tenants/upgrade").header("Authorization", "Bearer " + access))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.results[0].executionStatus").value("SUCCESS"));
		mockMvc.perform(get("/api/platform/v1/tenants").header("Authorization", "Bearer " + access).param("sort", "drop_table"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errorCode").value("COMMON.INVALID_SORT"));
		mockMvc.perform(get("/api/platform/v1/tenants").header("Authorization", "Bearer " + access).param("status", "ACTIVE")
				.param("keyword", code).param("sort", "tenantCode,desc"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.items[0].tenantCode").value(code));
		mockMvc.perform(get("/api/platform/v1/tenants/" + code + "/migrations").header("Authorization", "Bearer " + access))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.total").value(org.hamcrest.Matchers.greaterThan(0)));
		assertEquals(scalar("SELECT platform_admin_pk::text FROM pmis.platform_admin WHERE login_id = '" + login + "'"),
				scalar("SELECT change_user_pk::text FROM pmis.tenant_status_history WHERE change_reason = '점검' AND tenant_pk = (SELECT tenant_pk FROM pmis.tenant WHERE tenant_code = '"
						+ code + "')"));
	}

	@Test
	void bootstrapsOnlyWhenNoAdminExists() throws Exception {
		long count = Long.parseLong(scalar("SELECT count(*)::text FROM pmis.platform_admin"));
		if (count == 0) {
			String login = "itops" + UUID.randomUUID().toString().replace("-", "").substring(0, 12) + "@example.com";
			adminLogins.add(login);
			var password = bootstrapService.createIfEmpty(login, "운영자", null);
			assertEquals(20, password.reveal().length());
			assertEquals("TemporaryPassword[masked]", password.toString());
			assertEquals("Y", scalar("SELECT btrim(must_change_password_yn) FROM pmis.platform_admin WHERE login_id = '" + login + "'"));
			assertThrows(IllegalStateException.class, () -> bootstrapService.createIfEmpty(login, "운영자", null));
		}
		else {
			assertThrows(IllegalStateException.class, () -> bootstrapService.createIfEmpty("itops-occupied@example.com", "운영자", null));
		}
	}

	@Test
	void reusesADeletedDomainName() throws Exception {
		String index = scalar("""
				SELECT indexdef FROM pg_indexes
				WHERE schemaname = 'pmis' AND indexname = 'uk_tenant_domain_domain_name'
				""");
		Assumptions.assumeTrue(index != null && index.toLowerCase().contains("use_yn"));
		String login = insertAdmin(NEXT);
		execute("UPDATE pmis.platform_admin SET must_change_password_yn = 'N', password_expire_datetime = CURRENT_TIMESTAMP + INTERVAL '30 days' WHERE login_id = '"
				+ login + "'");
		String access = json(login(login, NEXT), "accessToken");
		String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
		String codeA = "it-" + suffix + "aaaa";
		String codeB = "it-" + suffix + "bbbb";
		String domain = "reuse-" + suffix + ".example.test";
		String extra = "extra-" + suffix + ".example.test";
		String other = "keep-" + suffix + ".example.test";
		tenantCodes.add(codeA);
		tenantCodes.add(codeB);
		schemas.add("t_" + codeA.replace('-', '_'));
		schemas.add("t_" + codeB.replace('-', '_'));
		createTenant(access, codeA, domain);
		createTenant(access, codeB, other);
		mockMvc.perform(post("/api/platform/v1/tenants/" + codeA + "/domains").header("Authorization", "Bearer " + access)
				.contentType(MediaType.APPLICATION_JSON).content("{\"domainName\":\"" + extra + "\"}"))
				.andExpect(status().isOk());
		mockMvc.perform(delete("/api/platform/v1/tenants/" + codeA + "/domains/" + domain).header("Authorization", "Bearer " + access))
				.andExpect(status().isOk());
		mockMvc.perform(post("/api/platform/v1/tenants/" + codeA + "/domains").header("Authorization", "Bearer " + access)
				.contentType(MediaType.APPLICATION_JSON).content("{\"domainName\":\"" + domain + "\"}"))
				.andExpect(status().isOk());
		mockMvc.perform(post("/api/platform/v1/tenants/" + codeA + "/domains").header("Authorization", "Bearer " + access)
				.contentType(MediaType.APPLICATION_JSON).content("{\"domainName\":\"" + domain + "\"}"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.errorCode").value("TENANT.DUPLICATE"));
		mockMvc.perform(delete("/api/platform/v1/tenants/" + codeA + "/domains/" + domain).header("Authorization", "Bearer " + access))
				.andExpect(status().isOk());
		mockMvc.perform(post("/api/platform/v1/tenants/" + codeB + "/domains").header("Authorization", "Bearer " + access)
				.contentType(MediaType.APPLICATION_JSON).content("{\"domainName\":\"" + domain + "\"}"))
				.andExpect(status().isOk());
		mockMvc.perform(post("/api/platform/v1/tenants/" + codeA + "/domains").header("Authorization", "Bearer " + access)
				.contentType(MediaType.APPLICATION_JSON).content("{\"domainName\":\"" + domain + "\"}"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.errorCode").value("TENANT.DUPLICATE"));
	}

	private void createTenant(String access, String code, String domain) throws Exception {
		mockMvc.perform(post("/api/platform/v1/tenants").header("Authorization", "Bearer " + access)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"tenantCode":"%s","tenantName":"아이티","defaultLanguageCode":"ko-KR","timezoneId":"Asia/Seoul","adminName":"관리자","adminEmail":"admin@example.com","domains":["%s"]}
						""".formatted(code, domain)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.tenantStatus").value("ACTIVE"));
	}

	private String insertAdmin(String password) throws Exception {
		String login = "itops" + UUID.randomUUID().toString().replace("-", "").substring(0, 12) + "@example.com";
		adminLogins.add(login);
		try (Connection connection = dataSource.getConnection()) {
			IntegrationDatabase.requirePmis(connection);
			try (PreparedStatement statement = connection.prepareStatement("""
					INSERT INTO pmis.platform_admin (
					    login_id, admin_name, email, password_hash, must_change_password_yn, admin_status_code,
					    password_expire_datetime, use_yn)
					VALUES (?, '운영자', ?, ?, 'Y', 'ACTIVE', CURRENT_TIMESTAMP + INTERVAL '3 days', 'Y')
					""")) {
				statement.setString(1, login);
				statement.setString(2, login);
				statement.setString(3, passwordHasher.hash(password));
				statement.executeUpdate();
			}
		}
		return login;
	}

	private MvcResult login(String login, String password) throws Exception {
		return mockMvc.perform(post("/api/platform/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"loginId\":\"" + login + "\",\"password\":\"" + password + "\"}"))
				.andExpect(status().isOk())
				.andReturn();
	}

	private void validatePlatformAdmin() {
		StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
				.applySetting(AvailableSettings.DATASOURCE, dataSource)
				.applySetting(AvailableSettings.HBM2DDL_AUTO, "validate")
				.applySetting(AvailableSettings.DIALECT, "org.hibernate.dialect.PostgreSQLDialect")
				.build();
		try (org.hibernate.SessionFactory sessionFactory = new MetadataSources(registry)
				.addAnnotatedClass(PlatformAdmin.class)
				.buildMetadata()
				.buildSessionFactory()) {
			assertFalse(sessionFactory.getMetamodel().getEntities().isEmpty());
		}
		finally {
			StandardServiceRegistryBuilder.destroy(registry);
		}
	}

	private String scalar(String sql) throws Exception {
		try (Connection connection = dataSource.getConnection()) {
			IntegrationDatabase.requirePmis(connection);
			try (Statement statement = connection.createStatement(); ResultSet resultSet = statement.executeQuery(sql)) {
				resultSet.next();
				return resultSet.getString(1);
			}
		}
	}

	private void execute(String sql) throws Exception {
		try (Connection connection = dataSource.getConnection()) {
			IntegrationDatabase.requirePmis(connection);
			try (Statement statement = connection.createStatement()) {
				statement.execute(sql);
			}
		}
	}

	private static String cookie(MvcResult result) {
		String header = result.getResponse().getHeader("Set-Cookie");
		String name = RefreshCookies.PLATFORM_NAME + "=";
		int start = header.indexOf(name) + name.length();
		return header.substring(start, header.indexOf(';', start));
	}

	private static String json(MvcResult result, String field) throws Exception {
		var node = tools.jackson.databind.json.JsonMapper.builder().build().readTree(result.getResponse().getContentAsString());
		var data = node.get("data");
		if (data != null && data.get(field) != null && !data.get(field).isNull()) {
			return data.get(field).asString();
		}
		if (node.get(field) == null || node.get(field).isNull()) {
			return null;
		}
		return node.get(field).asString();
	}

	private static String logText(ListAppender<ILoggingEvent> logs) {
		StringBuilder text = new StringBuilder();
		for (ILoggingEvent event : logs.list) {
			text.append(event.getFormattedMessage());
		}
		return text.toString();
	}

	private static void deleteAdmin(Connection connection, String login) throws Exception {
		Long adminPk = null;
		try (PreparedStatement statement = connection.prepareStatement("SELECT platform_admin_pk FROM pmis.platform_admin WHERE login_id = ?")) {
			statement.setString(1, login);
			try (ResultSet resultSet = statement.executeQuery()) {
				if (resultSet.next()) {
					adminPk = resultSet.getLong(1);
				}
			}
		}
		if (adminPk == null) {
			return;
		}
		try (PreparedStatement statement = connection.prepareStatement("DELETE FROM pmis.platform_admin_refresh_token WHERE platform_admin_pk = ?")) {
			statement.setLong(1, adminPk);
			statement.executeUpdate();
		}
		try (PreparedStatement statement = connection.prepareStatement("DELETE FROM pmis.platform_admin_login_history WHERE platform_admin_pk = ?")) {
			statement.setLong(1, adminPk);
			statement.executeUpdate();
		}
		try (PreparedStatement statement = connection.prepareStatement("DELETE FROM pmis.platform_admin WHERE platform_admin_pk = ?")) {
			statement.setLong(1, adminPk);
			statement.executeUpdate();
		}
	}

	private static void deleteTenant(Connection connection, String tenantCode) throws Exception {
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
