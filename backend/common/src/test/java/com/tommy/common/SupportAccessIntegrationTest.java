package com.tommy.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.tommy.common.global.multitenancy.HeaderTenantResolver;
import com.tommy.common.global.security.PasswordHasher;
import com.tommy.common.supportaccess.SupportAccounts;
import com.tommy.common.supportaccess.SupportAuditStore;
import com.tommy.common.tenant.provision.ProvisionTenantCommand;
import com.tommy.common.tenant.provision.TenantProvisioningService;
import com.tommy.common.tenant.provision.TenantUpgradeService;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class SupportAccessIntegrationTest {

	private static final String PASSWORD = "N3w-Policy-Pass1";

	private static final JsonMapper JSON = JsonMapper.builder().build();

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private DataSource dataSource;

	@Autowired
	private PasswordHasher passwordHasher;

	@Autowired
	private TenantProvisioningService provisioningService;

	@Autowired
	private TenantUpgradeService upgradeService;

	@Autowired
	private SupportAuditStore auditStore;

	private final List<String> tenantCodes = new ArrayList<>();

	private final List<String> adminLogins = new ArrayList<>();

	@DynamicPropertySource
	static void datasource(DynamicPropertyRegistry registry) {
		TestcontainersConfiguration.registerDatasource(registry);
		TestJwtKey.register(registry);
	}

	@BeforeEach
	void tables() throws Exception {
		ensureSupportTables();
	}

	@AfterEach
	void cleanup() throws Exception {
		try (Connection connection = dataSource.getConnection()) {
			IntegrationDatabase.requirePmis(connection);
			for (String tenantCode : tenantCodes) {
				deleteSupport(connection, tenantCode);
				deleteTenant(connection, tenantCode);
			}
			for (String login : adminLogins) {
				deleteAdmin(connection, login);
			}
			for (String tenantCode : tenantCodes) {
				String schema = schema(tenantCode);
				if (schema.matches("^t_it_[a-z0-9]{16}$")) {
					try (Statement statement = connection.createStatement()) {
						statement.execute("DROP SCHEMA IF EXISTS " + schema + " CASCADE");
					}
				}
			}
		}
	}

	@Test
	void supportSessionCanAdministerAndWritesAnAudit() throws Exception {
		String code = provision();
		String other = provision();
		provisioningService.provision(command(code));
		provisioningService.provision(command(other));
		String login = insertAdmin();
		Logger root = (Logger) org.slf4j.LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
		ListAppender<ILoggingEvent> logs = new ListAppender<>();
		logs.start();
		root.addAppender(logs);
		String ticket;
		String access;
		try {
			MvcResult issued = issue(login, code, "{\"reason\":\"점검\"}");
			String url = json(issued).get("data").get("accessUrl").asString();
			assertTrue(url.startsWith("http://localhost:8000/support-access?tenant=" + code + "#ticket="));
			ticket = URLDecoder.decode(url.substring(url.indexOf("#ticket=") + "#ticket=".length()), StandardCharsets.UTF_8);
			mockMvc.perform(post("/api/mdm/v1/auth/support-access/exchange").header(HeaderTenantResolver.HEADER, code)
					.contentType(MediaType.APPLICATION_JSON).content("{\"ticket\":\"" + ticket + "\"}"))
					.andExpect(status().isForbidden()).andExpect(jsonPath("$.errorCode").value("AUTH.CSRF"));
			MvcResult exchanged = mockMvc.perform(post("/api/mdm/v1/auth/support-access/exchange").header(HeaderTenantResolver.HEADER, code)
					.header("X-MDM-Request", "1").contentType(MediaType.APPLICATION_JSON).content("{\"ticket\":\"" + ticket + "\"}"))
					.andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
					.andExpect(jsonPath("$.data.expiresInSeconds").value(1800)).andReturn();
			assertTrue(exchanged.getResponse().getHeader("Set-Cookie") == null
					|| !exchanged.getResponse().getHeader("Set-Cookie").contains("mdm_refresh="));
			access = json(exchanged).get("data").get("accessToken").asString();
			mockMvc.perform(get("/api/mdm/v1/auth/me").headers(tenant(code, access)))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.data.displayName").value("플랫폼 지원 (" + login + ")"))
					.andExpect(jsonPath("$.data.support.operatorLoginId").value(login))
					.andExpect(jsonPath("$.data.passwordChangeRequired").value(false));
		}
		finally {
			root.detachAppender(logs);
		}
		assertFalse(logText(logs).contains(ticket));
		assertFalse(logText(logs).contains(access));
		HttpHeaders support = tenant(code, access);
		long systemPk = Long.parseLong(scalar(schema(code),
				"SELECT user_pk::text FROM user_account WHERE login_id = '" + SupportAccounts.LOGIN_ID + "'"));
		MvcResult created = mockMvc.perform(post("/api/mdm/v1/users").headers(support).contentType(MediaType.APPLICATION_JSON)
				.content("{\"loginId\":\"staff.user@example.com\",\"roleCodes\":[\"USER\"]}"))
				.andExpect(status().isOk()).andReturn();
		long userPk = json(created).get("data").get("userPk").asLong();
		assertEquals(Long.toString(systemPk), scalar(schema(code), "SELECT create_user_pk::text FROM user_account WHERE user_pk = " + userPk));
		mockMvc.perform(get("/api/mdm/v1/users").headers(support)).andExpect(status().isOk());
		mockMvc.perform(put("/api/mdm/v1/users/" + userPk + "/roles").headers(support).contentType(MediaType.APPLICATION_JSON)
				.content("[{\"roleCode\":\"USER\"}]")).andExpect(status().isOk());
		long accessId = json(mockMvc.perform(get("/api/mdm/v1/support-accesses").headers(support)).andExpect(status().isOk())
				.andExpect(jsonPath("$.data.items[0].operatorLoginId").value(login))
				.andExpect(jsonPath("$.data.items[0].writeCount").value(2)).andReturn()).get("data").get("items").get(0)
						.get("supportAccessId").asLong();
		mockMvc.perform(get("/api/mdm/v1/support-accesses/" + accessId + "/actions").headers(support))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.total").value(2))
				.andExpect(jsonPath("$.data.items[0].path").value("/api/mdm/v1/users"))
				.andExpect(jsonPath("$.data.items[0].sensitive").value(true))
				.andExpect(jsonPath("$.data.items[1].path").value("/api/mdm/v1/users/" + userPk + "/roles"))
				.andExpect(jsonPath("$.data.items[1].sensitive").value(true));
		String otherAccess = exchange(other, ticket(issue(login, other, "{\"reason\":\"다른\"}")));
		mockMvc.perform(get("/api/mdm/v1/support-accesses/" + accessId + "/actions").headers(tenant(other, otherAccess)))
				.andExpect(status().isNotFound()).andExpect(jsonPath("$.errorCode").value("SUPPORT.NOT_FOUND"));
	}

	@Test
	void rejectsReusedExpiredForeignAndTamperedTicketsTheSameWay() throws Exception {
		String code = provision();
		String other = provision();
		provisioningService.provision(command(code));
		provisioningService.provision(command(other));
		String login = insertAdmin();
		String ticket = ticket(issue(login, code, "{}"));
		exchange(code, ticket);
		mockMvc.perform(post("/api/mdm/v1/auth/support-access/exchange").header(HeaderTenantResolver.HEADER, code)
				.header("X-MDM-Request", "1").contentType(MediaType.APPLICATION_JSON).content("{\"ticket\":\"" + ticket + "\"}"))
				.andExpect(status().isUnauthorized()).andExpect(jsonPath("$.errorCode").value("AUTH.SUPPORT_TICKET_INVALID"));
		String expired = ticket(issue(login, code, "{\"reason\":\"만료\"}"));
		executePublic("UPDATE pmis.tenant_support_access SET ticket_expire_datetime = CURRENT_TIMESTAMP - INTERVAL '1 second' WHERE status_code = 'ISSUED'");
		mockMvc.perform(post("/api/mdm/v1/auth/support-access/exchange").header(HeaderTenantResolver.HEADER, code)
				.header("X-MDM-Request", "1").contentType(MediaType.APPLICATION_JSON).content("{\"ticket\":\"" + expired + "\"}"))
				.andExpect(status().isUnauthorized()).andExpect(jsonPath("$.errorCode").value("AUTH.SUPPORT_TICKET_INVALID"));
		String foreign = ticket(issue(login, code, "{\"reason\":\"다른테넌트\"}"));
		mockMvc.perform(post("/api/mdm/v1/auth/support-access/exchange").header(HeaderTenantResolver.HEADER, other)
				.header("X-MDM-Request", "1").contentType(MediaType.APPLICATION_JSON).content("{\"ticket\":\"" + foreign + "\"}"))
				.andExpect(status().isUnauthorized()).andExpect(jsonPath("$.errorCode").value("AUTH.SUPPORT_TICKET_INVALID"));
		exchange(code, foreign);
		mockMvc.perform(post("/api/mdm/v1/auth/support-access/exchange").header(HeaderTenantResolver.HEADER, code)
				.header("X-MDM-Request", "1").contentType(MediaType.APPLICATION_JSON).content("{\"ticket\":\"tampered-ticket\"}"))
				.andExpect(status().isUnauthorized()).andExpect(jsonPath("$.errorCode").value("AUTH.SUPPORT_TICKET_INVALID"));
	}

	@Test
	void endsExpiresAndRevokesTheSession() throws Exception {
		String code = provision();
		provisioningService.provision(command(code));
		String login = insertAdmin();
		String ended = exchange(code, ticket(issue(login, code, "{\"reason\":\"종료\"}")));
		mockMvc.perform(post("/api/mdm/v1/auth/support-access/end").headers(tenant(code, ended)).header("X-MDM-Request", "1"))
				.andExpect(status().isOk());
		mockMvc.perform(post("/api/mdm/v1/auth/support-access/end").headers(tenant(code, ended)).header("X-MDM-Request", "1"))
				.andExpect(status().isOk());
		mockMvc.perform(get("/api/mdm/v1/auth/me").headers(tenant(code, ended)))
				.andExpect(status().isUnauthorized()).andExpect(jsonPath("$.errorCode").value("AUTH.SUPPORT_ENDED"));

		String expired = exchange(code, ticket(issue(login, code, "{\"reason\":\"시간\"}")));
		executePublic("""
				UPDATE pmis.tenant_support_access
				SET expire_datetime = CURRENT_TIMESTAMP - INTERVAL '1 minute'
				WHERE status_code = 'ACTIVE'
				""");
		assertEquals("past", scalarPublic("""
				SELECT CASE WHEN expire_datetime < CURRENT_TIMESTAMP THEN 'past' ELSE 'future' END
				FROM pmis.tenant_support_access
				WHERE status_code = 'ACTIVE'
				ORDER BY support_access_pk DESC
				"""));
		mockMvc.perform(get("/api/mdm/v1/auth/me").headers(tenant(code, expired)))
				.andExpect(status().isUnauthorized()).andExpect(jsonPath("$.errorCode").value("AUTH.SUPPORT_ENDED"));
		assertEquals("EXPIRED", scalarPublic("SELECT status_code FROM pmis.tenant_support_access WHERE end_reason_code = 'EXPIRED'"));

		String revoked = exchange(code, ticket(issue(login, code, "{\"reason\":\"중지\"}")));
		executePublic("UPDATE pmis.platform_admin SET use_yn = 'N' WHERE login_id = '" + login + "'");
		mockMvc.perform(get("/api/mdm/v1/auth/me").headers(tenant(code, revoked)))
				.andExpect(status().isUnauthorized()).andExpect(jsonPath("$.errorCode").value("AUTH.SUPPORT_ENDED"));
		assertEquals("REVOKED", scalarPublic(
				"SELECT status_code FROM pmis.tenant_support_access WHERE platform_login_id = '" + login + "' AND end_reason_code = 'OPERATOR_DISABLED'"));
	}

	@Test
	void supportTokenCannotChangePasswordOrCallPlatformApis() throws Exception {
		String code = provision();
		provisioningService.provision(command(code));
		String login = insertAdmin();
		String access = exchange(code, ticket(issue(login, code, "{\"reason\":\"제한\"}")));
		HttpHeaders support = tenant(code, access);
		mockMvc.perform(post("/api/mdm/v1/auth/password/change").headers(support).contentType(MediaType.APPLICATION_JSON)
				.content("{\"currentPassword\":\"x\",\"newPassword\":\"N3w-Policy-Pass1\"}"))
				.andExpect(status().isForbidden()).andExpect(jsonPath("$.errorCode").value("AUTH.SUPPORT_FORBIDDEN"));
		mockMvc.perform(post("/api/platform/v1/tenants/" + code + "/support-access").header("Authorization", "Bearer " + access)
				.contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(get("/api/platform/v1/support-accesses").header("Authorization", "Bearer " + access))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void hidesTheSystemUserAndStillProtectsTheLastAdministrator() throws Exception {
		String code = provision();
		String temporary = provisioningService.provision(command(code)).temporaryPassword().reveal();
		String schema = schema(code);
		long systemPk = Long.parseLong(scalar(schema, "SELECT user_pk::text FROM user_account WHERE login_id = '" + SupportAccounts.LOGIN_ID + "'"));
		long adminPk = Long.parseLong(scalar(schema, "SELECT user_pk::text FROM user_account WHERE login_id = 'admin@example.com'"));
		mockMvc.perform(post("/api/mdm/v1/auth/login").header(HeaderTenantResolver.HEADER, code).contentType(MediaType.APPLICATION_JSON)
				.content("{\"loginId\":\"" + SupportAccounts.LOGIN_ID + "\",\"password\":\"wrong-password\"}"))
				.andExpect(status().isUnauthorized()).andExpect(jsonPath("$.errorCode").value("AUTH.INVALID_CREDENTIALS"));
		assertEquals("0", scalar(schema, "SELECT login_fail_count::text FROM user_account WHERE user_pk = " + systemPk));
		MvcResult changed = mockMvc.perform(post("/api/mdm/v1/auth/password/change").header(HeaderTenantResolver.HEADER, code)
				.header("Authorization", "Bearer " + json(login(code, "admin@example.com", temporary), "accessToken"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"currentPassword\":\"" + temporary + "\",\"newPassword\":\"" + PASSWORD + "\"}"))
				.andExpect(status().isOk()).andReturn();
		HttpHeaders admin = tenant(code, json(changed, "accessToken"));
		mockMvc.perform(get("/api/mdm/v1/users").headers(admin).param("keyword", "platform-support"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(0));
		mockMvc.perform(get("/api/mdm/v1/users/" + systemPk).headers(admin))
				.andExpect(status().isConflict()).andExpect(jsonPath("$.errorCode").value("USER.SYSTEM_ACCOUNT"));
		mockMvc.perform(post("/api/mdm/v1/users/" + systemPk + "/password/reset").headers(admin))
				.andExpect(status().isConflict()).andExpect(jsonPath("$.errorCode").value("USER.SYSTEM_ACCOUNT"));
		mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/mdm/v1/users/" + adminPk).headers(admin))
				.andExpect(status().isConflict()).andExpect(jsonPath("$.errorCode").value("USER.SELF_OPERATION_FORBIDDEN"));
		String login = insertAdmin();
		HttpHeaders support = tenant(code, exchange(code, ticket(issue(login, code, "{\"reason\":\"마지막\"}"))));
		mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/mdm/v1/users/" + adminPk).headers(support))
				.andExpect(status().isConflict()).andExpect(jsonPath("$.errorCode").value("USER.LAST_ADMIN"));
		mockMvc.perform(get("/api/mdm/v1/roles").headers(admin).param("keyword", "TENANT_ADMIN"))
				.andExpect(jsonPath("$.data.items[0].userCount").value(1));
	}

	@Test
	void listsOnlyTheCurrentTenantAndFiltersPlatformHistory() throws Exception {
		String code = provision();
		String other = provision();
		provisioningService.provision(command(code));
		provisioningService.provision(command(other));
		String login = insertAdmin();
		String second = insertAdmin("it.ops_a" + UUID.randomUUID().toString().replace("-", "").substring(0, 8) + "@example.com");
		issue(login, code, "{\"reason\":\"하나\"}");
		issue(second, other, "{\"reason\":\"둘\"}");
		String platform = platformToken(login);
		mockMvc.perform(get("/api/platform/v1/support-accesses").header("Authorization", "Bearer " + platform)
				.param("tenantCode", code)).andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(1))
				.andExpect(jsonPath("$.data.items[0].tenantCode").value(code));
		mockMvc.perform(get("/api/platform/v1/support-accesses").header("Authorization", "Bearer " + platform)
				.param("platformLoginId", "ops_")).andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(1))
				.andExpect(jsonPath("$.data.items[0].operatorLoginId").value(second));
		mockMvc.perform(get("/api/platform/v1/support-accesses").header("Authorization", "Bearer " + platform).param("sort", "password,asc"))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.errorCode").value("COMMON.INVALID_SORT"));
		executePublic("""
				UPDATE pmis.tenant_support_access
				SET status_code = 'ACTIVE', started_datetime = CURRENT_TIMESTAMP, expire_datetime = CURRENT_TIMESTAMP - INTERVAL '1 minute'
				WHERE platform_login_id = '%s'
				""".formatted(second));
		mockMvc.perform(get("/api/platform/v1/support-accesses").header("Authorization", "Bearer " + platform).param("status", "EXPIRED"))
				.andExpect(jsonPath("$.data.items[?(@.operatorLoginId == '" + second + "')].status").value("EXPIRED"));
		assertEquals("ACTIVE", scalarPublic("SELECT status_code FROM pmis.tenant_support_access WHERE platform_login_id = '" + second + "'"));
		String user = userToken(code);
		mockMvc.perform(get("/api/mdm/v1/support-accesses").headers(tenant(code, user)))
				.andExpect(status().isForbidden()).andExpect(jsonPath("$.errorCode").value("AUTH.FORBIDDEN"));
		mockMvc.perform(post("/api/platform/v1/tenants/" + code + "/support-access").header("Authorization", "Bearer " + platform)
				.contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"" + "가".repeat(201) + "\"}"))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.errorCode").value("COMMON.VALIDATION_FAILED"));
		MvcResult blank = issue(login, code, "{}");
		assertTrue(json(blank).get("data").get("accessUrl").asString().contains("#ticket="));
		mockMvc.perform(get("/api/platform/v1/support-accesses").header("Authorization", "Bearer " + platform).param("status", "ISSUED")
				.param("platformLoginId", login.substring(0, 6)))
				.andExpect(jsonPath("$.data.items[?(@.reasonDefault == true)]").isNotEmpty());
	}

	@Test
	void refusesTheRequestWhenTheAuditRowCannotBeWritten() throws Exception {
		String code = provision();
		provisioningService.provision(command(code));
		String login = insertAdmin();
		HttpHeaders support = tenant(code, exchange(code, ticket(issue(login, code, "{\"reason\":\"감사\"}"))));
		int before = Integer.parseInt(scalar(schema(code), "SELECT count(*)::text FROM user_account"));
		auditStore.failNextInsert();
		mockMvc.perform(post("/api/mdm/v1/users").headers(support).contentType(MediaType.APPLICATION_JSON)
				.content("{\"loginId\":\"blocked.user@example.com\"}"))
				.andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.errorCode").value("SUPPORT.AUDIT_UNAVAILABLE"));
		assertEquals(before, Integer.parseInt(scalar(schema(code), "SELECT count(*)::text FROM user_account")));
	}

	@Test
	void upgradeAddsTheSystemUserAndSupportMenu() throws Exception {
		String code = provision();
		provisioningService.provision(command(code));
		String schema = schema(code);
		execute(schema, "DELETE FROM user_role WHERE user_pk IN (SELECT user_pk FROM user_account WHERE login_id = '" + SupportAccounts.LOGIN_ID + "')");
		execute(schema, "DELETE FROM user_account WHERE login_id = '" + SupportAccounts.LOGIN_ID + "'");
		execute(schema, "DELETE FROM role_permission WHERE permission_pk IN (SELECT permission_pk FROM permission WHERE permission_code = 'SUPPORT_ACCESS:READ')");
		execute(schema, "DELETE FROM permission WHERE permission_code = 'SUPPORT_ACCESS:READ'");
		execute(schema, "DELETE FROM menu WHERE menu_code = 'SUPPORT_ACCESS'");
		assertEquals("SUCCESS", upgradeService.upgradeTenants(List.of(code), null).results().get(0).executionStatus());
		assertEquals("1", scalar(schema, "SELECT count(*)::text FROM user_account WHERE login_id = '" + SupportAccounts.LOGIN_ID + "' AND password_hash IS NULL"));
		assertEquals("1", scalar(schema, """
				SELECT count(*)::text FROM user_role ur
				JOIN user_account u ON u.user_pk = ur.user_pk
				JOIN role r ON r.role_pk = ur.role_pk
				WHERE u.login_id = '%s' AND r.role_code = 'TENANT_ADMIN'
				""".formatted(SupportAccounts.LOGIN_ID)));
		assertEquals("1", scalar(schema, "SELECT count(*)::text FROM menu WHERE menu_code = 'SUPPORT_ACCESS'"));
		assertEquals("READ", scalar(schema, """
				SELECT string_agg(p.action_code, ',' ORDER BY p.action_code)
				FROM permission p JOIN menu m ON m.menu_pk = p.menu_pk
				WHERE m.menu_code = 'SUPPORT_ACCESS'
				"""));
	}

	private MvcResult issue(String login, String tenantCode, String body) throws Exception {
		return mockMvc.perform(post("/api/platform/v1/tenants/" + tenantCode + "/support-access")
				.header("Authorization", "Bearer " + platformToken(login)).contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store")).andReturn();
	}

	private String ticket(MvcResult issued) throws Exception {
		String url = json(issued).get("data").get("accessUrl").asString();
		return URLDecoder.decode(url.substring(url.indexOf("#ticket=") + "#ticket=".length()), StandardCharsets.UTF_8);
	}

	private String exchange(String tenantCode, String ticket) throws Exception {
		MvcResult exchanged = mockMvc.perform(post("/api/mdm/v1/auth/support-access/exchange").header(HeaderTenantResolver.HEADER, tenantCode)
				.header("X-MDM-Request", "1").contentType(MediaType.APPLICATION_JSON).content("{\"ticket\":\"" + ticket + "\"}"))
				.andExpect(status().isOk()).andReturn();
		return json(exchanged).get("data").get("accessToken").asString();
	}

	private String platformToken(String login) throws Exception {
		MvcResult loggedIn = mockMvc.perform(post("/api/platform/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"loginId\":\"" + login + "\",\"password\":\"" + PASSWORD + "\"}")).andExpect(status().isOk()).andReturn();
		return json(loggedIn).get("data").get("accessToken").asString();
	}

	private String userToken(String tenantCode) throws Exception {
		String schema = schema(tenantCode);
		execute(schema, """
				INSERT INTO user_account (account_type_code, login_id, password_hash, must_change_password_yn, user_status_code, password_expire_datetime)
				SELECT 'NORMAL', 'limited@example.com', '%s', 'N', 'ACTIVE', CURRENT_TIMESTAMP + INTERVAL '30 days'
				WHERE NOT EXISTS (SELECT 1 FROM user_account WHERE login_id = 'limited@example.com')
				""".formatted(passwordHasher.hash(PASSWORD).replace("'", "''")));
		execute(schema, """
				INSERT INTO user_role (user_pk, role_pk, use_yn)
				SELECT u.user_pk, r.role_pk, 'Y'
				FROM user_account u, role r
				WHERE u.login_id = 'limited@example.com' AND r.role_code = 'USER'
				  AND NOT EXISTS (
				      SELECT 1 FROM user_role ur WHERE ur.user_pk = u.user_pk AND ur.role_pk = r.role_pk)
				""");
		return json(login(tenantCode, "limited@example.com", PASSWORD), "accessToken");
	}

	private MvcResult login(String tenantCode, String loginId, String password) throws Exception {
		return mockMvc.perform(post("/api/mdm/v1/auth/login").header(HeaderTenantResolver.HEADER, tenantCode)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"loginId\":\"" + loginId + "\",\"password\":\"" + password + "\"}")).andExpect(status().isOk()).andReturn();
	}

	private static HttpHeaders tenant(String tenantCode, String access) {
		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(access);
		headers.set(HeaderTenantResolver.HEADER, tenantCode);
		return headers;
	}

	private String provision() {
		String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
		String tenantCode = "it-" + suffix;
		tenantCodes.add(tenantCode);
		return tenantCode;
	}

	private static ProvisionTenantCommand command(String tenantCode) {
		return new ProvisionTenantCommand(tenantCode, "아이티", null, "ko-KR", "관리자", "admin@example.com", null);
	}

	private static String schema(String tenantCode) {
		return "t_" + tenantCode.replace('-', '_');
	}

	private String insertAdmin() throws Exception {
		String login = "itops" + UUID.randomUUID().toString().replace("-", "").substring(0, 12) + "@example.com";
		return insertAdmin(login);
	}

	private String insertAdmin(String login) throws Exception {
		adminLogins.add(login);
		try (Connection connection = dataSource.getConnection()) {
			IntegrationDatabase.requirePmis(connection);
			try (PreparedStatement statement = connection.prepareStatement("""
					INSERT INTO pmis.platform_admin (
					    login_id, admin_name, email, password_hash, must_change_password_yn, admin_status_code,
					    password_expire_datetime, use_yn)
					VALUES (?, '운영자', ?, ?, 'N', 'ACTIVE', CURRENT_TIMESTAMP + INTERVAL '30 days', 'Y')
					""")) {
				statement.setString(1, login);
				statement.setString(2, login);
				statement.setString(3, passwordHasher.hash(PASSWORD));
				statement.executeUpdate();
			}
		}
		return login;
	}

	private void ensureSupportTables() throws Exception {
		try (Connection connection = dataSource.getConnection()) {
			IntegrationDatabase.requirePmis(connection);
			try (Statement statement = connection.createStatement();
					ResultSet resultSet = statement.executeQuery("SELECT to_regclass('pmis.tenant_support_access')")) {
				resultSet.next();
				if (resultSet.getString(1) != null) {
					return;
				}
			}
		}
		Flyway.configure().dataSource(dataSource).locations("classpath:db/migration/public").schemas("pmis").defaultSchema("pmis")
				.load().migrate();
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

	private String scalarPublic(String sql) throws Exception {
		try (Connection connection = dataSource.getConnection()) {
			IntegrationDatabase.requirePmis(connection);
			try (Statement statement = connection.createStatement(); ResultSet resultSet = statement.executeQuery(sql)) {
				resultSet.next();
				return resultSet.getString(1);
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

	private void executePublic(String sql) throws Exception {
		try (Connection connection = dataSource.getConnection()) {
			IntegrationDatabase.requirePmis(connection);
			try (Statement statement = connection.createStatement()) {
				statement.execute(sql);
			}
		}
	}

	private static JsonNode json(MvcResult result) throws Exception {
		return JSON.readTree(result.getResponse().getContentAsString());
	}

	private static String json(MvcResult result, String field) throws Exception {
		JsonNode data = json(result).get("data");
		return data.get(field).asString();
	}

	private static String logText(ListAppender<ILoggingEvent> logs) {
		StringBuilder text = new StringBuilder();
		for (ILoggingEvent event : logs.list) {
			text.append(event.getFormattedMessage());
		}
		return text.toString();
	}

	private static void deleteSupport(Connection connection, String tenantCode) throws Exception {
		try (PreparedStatement actions = connection.prepareStatement("""
				DELETE FROM pmis.tenant_support_access_action
				WHERE support_access_pk IN (
				    SELECT a.support_access_pk FROM pmis.tenant_support_access a
				    JOIN pmis.tenant t ON t.tenant_pk = a.tenant_pk
				    WHERE t.tenant_code = ?)
				""")) {
			actions.setString(1, tenantCode);
			actions.executeUpdate();
		}
		try (PreparedStatement access = connection.prepareStatement("""
				DELETE FROM pmis.tenant_support_access
				WHERE tenant_pk = (SELECT tenant_pk FROM pmis.tenant WHERE tenant_code = ?)
				""")) {
			access.setString(1, tenantCode);
			access.executeUpdate();
		}
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
		try (PreparedStatement statement = connection.prepareStatement("""
				DELETE FROM pmis.tenant_support_access_action
				WHERE support_access_pk IN (SELECT support_access_pk FROM pmis.tenant_support_access WHERE platform_admin_pk = ?)
				""")) {
			statement.setLong(1, adminPk);
			statement.executeUpdate();
		}
		try (PreparedStatement statement = connection.prepareStatement("DELETE FROM pmis.tenant_support_access WHERE platform_admin_pk = ?")) {
			statement.setLong(1, adminPk);
			statement.executeUpdate();
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
