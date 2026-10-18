package com.tommy.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.springframework.test.web.servlet.MvcResult;

import com.tommy.common.auth.RefreshTokenValues;
import com.tommy.common.global.multitenancy.HeaderTenantResolver;
import com.tommy.common.tenant.provision.ProvisionTenantCommand;
import com.tommy.common.tenant.provision.TenantAdminPasswordService;
import com.tommy.common.tenant.provision.TenantProvisioningService;
import com.tommy.common.tenant.provision.TenantUpgradeService;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.servlet.http.Cookie;

@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class AuthApiIntegrationTest {

	private static final String NEXT = "N3w-Policy-Pass1";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private TenantProvisioningService provisioningService;

	@Autowired
	private TenantAdminPasswordService passwordService;

	@Autowired
	private TenantUpgradeService upgradeService;

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
	void signsInRotatesRefreshAndKeepsTenantsApart() throws Exception {
		String codeA = provision();
		String schemaA = schema(codeA);
		String temporary = provisioningService.provision(command(codeA)).temporaryPassword().reveal();
		Logger root = (Logger) org.slf4j.LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
		ListAppender<ILoggingEvent> logs = new ListAppender<>();
		logs.start();
		root.addAppender(logs);
		MvcResult login;
		try {
			login = mockMvc.perform(post("/api/mdm/v1/auth/login").header(HeaderTenantResolver.HEADER, codeA)
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"loginId\":\"Admin@Example.com\",\"password\":\"" + temporary + "\"}"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.data.passwordChangeRequired").value(true))
					.andReturn();
		}
		finally {
			root.detachAppender(logs);
		}
		String access = json(login, "accessToken");
		String refresh = cookie(login);
		assertFalse(logText(logs).contains(temporary));
		assertFalse(logText(logs).contains(access));
		assertFalse(logText(logs).contains(refresh));
		mockMvc.perform(get("/api/mdm/v1/code-groups").header(HeaderTenantResolver.HEADER, codeA).header("Authorization", "Bearer " + access))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.errorCode").value("AUTH.PASSWORD_CHANGE_REQUIRED"));
		mockMvc.perform(get("/api/mdm/v1/auth/me").header(HeaderTenantResolver.HEADER, codeA).header("Authorization", "Bearer " + access))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.loginId").value("admin@example.com"))
				.andExpect(jsonPath("$.data.roleCodes[0]").value("TENANT_ADMIN"))
				.andExpect(jsonPath("$.data.passwordChangeRequired").value(true));
		MvcResult changed = mockMvc.perform(post("/api/mdm/v1/auth/password/change").header(HeaderTenantResolver.HEADER, codeA)
				.header("Authorization", "Bearer " + access).contentType(MediaType.APPLICATION_JSON)
				.content("{\"currentPassword\":\"" + temporary + "\",\"newPassword\":\"" + NEXT + "\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.passwordChangeRequired").value(false))
				.andReturn();
		String nextAccess = json(changed, "accessToken");
		String nextRefresh = cookie(changed);
		assertEquals("PASSWORD_CHANGED", reason(schemaA, RefreshTokenValues.hash(refresh)));
		assertEquals("CHANGE", scalar(schemaA, "SELECT change_type_code FROM user_password_history ORDER BY password_history_pk DESC LIMIT 1"));
		mockMvc.perform(get("/api/mdm/v1/code-groups").header(HeaderTenantResolver.HEADER, codeA)
				.header("Authorization", "Bearer " + nextAccess)).andExpect(status().isOk());
		MvcResult rotated = mockMvc.perform(post("/api/mdm/v1/auth/refresh").header(HeaderTenantResolver.HEADER, codeA)
				.header("X-MDM-Request", "1").cookie(new Cookie("mdm_refresh", nextRefresh)))
				.andExpect(status().isOk()).andReturn();
		String rotatedRefresh = cookie(rotated);
		mockMvc.perform(post("/api/mdm/v1/auth/refresh").header(HeaderTenantResolver.HEADER, codeA)
				.header("X-MDM-Request", "1").cookie(new Cookie("mdm_refresh", nextRefresh)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.errorCode").value("AUTH.REFRESH_IN_PROGRESS"));
		assertEquals("ROTATED", reason(schemaA, RefreshTokenValues.hash(nextRefresh)));
		assertEquals("0", scalar(schemaA, "SELECT count(*)::text FROM user_refresh_token WHERE revoke_reason_code = 'REUSE_DETECTED'"));
		execute(schemaA, "UPDATE user_refresh_token SET revoked_datetime = TIMESTAMP '2020-01-01 00:00:00' WHERE token_hash = '"
				+ RefreshTokenValues.hash(nextRefresh) + "'");
		assertEquals("2020", scalar(schemaA, "SELECT to_char(revoked_datetime, 'YYYY') FROM user_refresh_token WHERE token_hash = '"
				+ RefreshTokenValues.hash(nextRefresh) + "'"));
		mockMvc.perform(post("/api/mdm/v1/auth/refresh").header(HeaderTenantResolver.HEADER, codeA)
				.header("X-MDM-Request", "1").cookie(new Cookie("mdm_refresh", nextRefresh)))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.errorCode").value("AUTH.TOKEN_REUSED"));
		assertEquals("REUSE_DETECTED", reason(schemaA, RefreshTokenValues.hash(rotatedRefresh)));
		mockMvc.perform(post("/api/mdm/v1/auth/refresh").header(HeaderTenantResolver.HEADER, codeA)
				.cookie(new Cookie("mdm_refresh", rotatedRefresh)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.errorCode").value("AUTH.CSRF"));
		MvcResult again = login(codeA, NEXT);
		String liveAccess = json(again, "accessToken");
		String liveRefresh = cookie(again);
		mockMvc.perform(post("/api/mdm/v1/auth/logout").header(HeaderTenantResolver.HEADER, codeA)
				.header("Authorization", "Bearer " + liveAccess).header("X-MDM-Request", "1")
				.cookie(new Cookie("mdm_refresh", liveRefresh)))
				.andExpect(status().isOk())
				.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().string("Set-Cookie",
						org.hamcrest.Matchers.containsString("Max-Age=0")));
		mockMvc.perform(post("/api/mdm/v1/auth/refresh").header(HeaderTenantResolver.HEADER, codeA)
				.header("X-MDM-Request", "1").cookie(new Cookie("mdm_refresh", liveRefresh)))
				.andExpect(status().isUnauthorized());
		MvcResult unknown = mockMvc.perform(post("/api/mdm/v1/auth/login").header(HeaderTenantResolver.HEADER, codeA)
				.contentType(MediaType.APPLICATION_JSON).content("{\"loginId\":\"missing@example.com\",\"password\":\"wrong-pass-1\"}"))
				.andExpect(status().isUnauthorized()).andReturn();
		MvcResult wrong = mockMvc.perform(post("/api/mdm/v1/auth/login").header(HeaderTenantResolver.HEADER, codeA)
				.contentType(MediaType.APPLICATION_JSON).content("{\"loginId\":\"admin@example.com\",\"password\":\"wrong-pass-1\"}"))
				.andExpect(status().isUnauthorized()).andReturn();
		assertEquals(json(unknown, "errorCode"), json(wrong, "errorCode"));
		assertEquals(json(unknown, "errorMessage"), json(wrong, "errorMessage"));
		execute(schemaA, """
				UPDATE common_code SET ext_attr1 = '2'
				WHERE code_value = 'MAX_LOGIN_FAIL_COUNT'
				""");
		mockMvc.perform(post("/api/mdm/v1/auth/login").header(HeaderTenantResolver.HEADER, codeA).contentType(MediaType.APPLICATION_JSON)
				.content("{\"loginId\":\"admin@example.com\",\"password\":\"wrong-pass-1\"}")).andExpect(status().isUnauthorized());
		mockMvc.perform(post("/api/mdm/v1/auth/login").header(HeaderTenantResolver.HEADER, codeA).contentType(MediaType.APPLICATION_JSON)
				.content("{\"loginId\":\"admin@example.com\",\"password\":\"wrong-pass-1\"}")).andExpect(status().isUnauthorized());
		assertEquals("LOCKED", scalar(schemaA, "SELECT user_status_code FROM user_account WHERE login_id = 'admin@example.com'"));
		mockMvc.perform(post("/api/mdm/v1/auth/login").header(HeaderTenantResolver.HEADER, codeA).contentType(MediaType.APPLICATION_JSON)
				.content("{\"loginId\":\"admin@example.com\",\"password\":\"" + NEXT + "\"}")).andExpect(status().isUnauthorized());
		execute(schemaA, """
				UPDATE user_account SET locked_datetime = TIMESTAMP '2020-01-01 00:00:00'
				WHERE login_id = 'admin@example.com'
				""");
		mockMvc.perform(post("/api/mdm/v1/auth/login").header(HeaderTenantResolver.HEADER, codeA).contentType(MediaType.APPLICATION_JSON)
				.content("{\"loginId\":\"admin@example.com\",\"password\":\"" + NEXT + "\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.passwordChangeRequired").value(false));
		assertEquals("ACTIVE", scalar(schemaA, "SELECT user_status_code FROM user_account WHERE login_id = 'admin@example.com'"));
		String codeB = provision();
		provisioningService.provision(command(codeB));
		MvcResult tenantA = login(codeA, NEXT);
		mockMvc.perform(get("/api/mdm/v1/auth/me").header(HeaderTenantResolver.HEADER, codeB)
				.header("Authorization", "Bearer " + json(tenantA, "accessToken")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errorCode").value("TENANT.HOST_MISMATCH"));
		mockMvc.perform(post("/api/mdm/v1/auth/login").header(HeaderTenantResolver.HEADER, codeB).contentType(MediaType.APPLICATION_JSON)
				.content("{\"loginId\":\"admin@example.com\",\"password\":\"" + NEXT + "\"}")).andExpect(status().isUnauthorized());
		MvcResult beforeReissue = login(codeA, NEXT);
		var reissued = passwordService.reissue(codeA, null);
		mockMvc.perform(post("/api/mdm/v1/auth/login").header(HeaderTenantResolver.HEADER, codeA).contentType(MediaType.APPLICATION_JSON)
				.content("{\"loginId\":\"admin@example.com\",\"password\":\"" + NEXT + "\"}")).andExpect(status().isUnauthorized());
		mockMvc.perform(post("/api/mdm/v1/auth/refresh").header(HeaderTenantResolver.HEADER, codeA).header("X-MDM-Request", "1")
				.cookie(new Cookie("mdm_refresh", cookie(beforeReissue)))).andExpect(status().isUnauthorized());
		MvcResult reissuedLogin = mockMvc.perform(post("/api/mdm/v1/auth/login").header(HeaderTenantResolver.HEADER, codeA)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"loginId\":\"admin@example.com\",\"password\":\"" + reissued.temporaryPassword().reveal() + "\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.passwordChangeRequired").value(true))
				.andReturn();
		execute(schemaA, "UPDATE user_account SET use_yn = 'N' WHERE login_id = 'admin@example.com'");
		mockMvc.perform(post("/api/mdm/v1/auth/refresh").header(HeaderTenantResolver.HEADER, codeA).header("X-MDM-Request", "1")
				.cookie(new Cookie("mdm_refresh", cookie(reissuedLogin))))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.errorCode").value("AUTH.ACCOUNT_DISABLED"));
		mockMvc.perform(post("/api/mdm/v1/auth/login").header(HeaderTenantResolver.HEADER, codeA).contentType(MediaType.APPLICATION_JSON)
				.content("{\"loginId\":\"admin@example.com\",\"password\":\"" + reissued.temporaryPassword().reveal() + "\"}"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.errorCode").value("AUTH.INVALID_CREDENTIALS"));
	}

	@Test
	void upgradeAddsPolicyKeysWithoutOverwritingValues() throws Exception {
		String code = provision();
		String schema = schema(code);
		provisioningService.provision(command(code));
		execute(schema, """
				DELETE FROM common_code_lang WHERE code_pk IN (
				    SELECT c.code_pk FROM common_code c WHERE c.code_value = 'PASSWORD_EXPIRE_DAYS')
				""");
		execute(schema, "DELETE FROM common_code WHERE code_value = 'PASSWORD_EXPIRE_DAYS'");
		execute(schema, "UPDATE common_code SET ext_attr1 = '12' WHERE code_value = 'PASSWORD_MIN_LENGTH'");
		assertEquals("SUCCESS", upgradeService.upgradeTenants(List.of(code), null).results().stream()
				.filter(result -> code.equals(result.tenantCode())).findFirst().orElseThrow().executionStatus());
		assertEquals("90", scalar(schema, "SELECT ext_attr1 FROM common_code WHERE code_value = 'PASSWORD_EXPIRE_DAYS'"));
		assertEquals("12", scalar(schema, "SELECT ext_attr1 FROM common_code WHERE code_value = 'PASSWORD_MIN_LENGTH'"));
		assertEquals("로그인", scalar(schema, """
				SELECT default_message FROM i18n_message m
				JOIN i18n_namespace n ON n.namespace_pk = m.namespace_pk
				WHERE n.namespace_code = 'auth' AND m.message_key = 'auth.login.title'
				"""));
	}

	private MvcResult login(String tenantCode, String password) throws Exception {
		return mockMvc.perform(post("/api/mdm/v1/auth/login").header(HeaderTenantResolver.HEADER, tenantCode)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"loginId\":\"admin@example.com\",\"password\":\"" + password + "\"}"))
				.andExpect(status().isOk()).andReturn();
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

	private static String cookie(MvcResult result) {
		String header = result.getResponse().getHeader("Set-Cookie");
		int start = header.indexOf("mdm_refresh=") + "mdm_refresh=".length();
		return header.substring(start, header.indexOf(';', start));
	}

	private static String json(MvcResult result, String field) throws Exception {
		var node = tools.jackson.databind.json.JsonMapper.builder().build().readTree(result.getResponse().getContentAsString());
		var data = node.get("data");
		if (data != null && data.get(field) != null) {
			return data.get(field).asString();
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

	private String reason(String schema, String hash) throws Exception {
		return scalar(schema, "SELECT revoke_reason_code FROM user_refresh_token WHERE token_hash = '" + hash + "'");
	}

	private String scalar(String schema, String sql) throws Exception {
		try (Connection connection = dataSource.getConnection()) {
			IntegrationDatabase.requirePmis(connection);
			try (Statement statement = connection.createStatement()) {
				statement.execute("SET search_path TO " + schema);
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
				statement.execute("SET search_path TO " + schema);
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
