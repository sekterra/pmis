package com.tommy.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
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

import com.tommy.common.global.multitenancy.HeaderTenantResolver;
import com.tommy.common.global.security.DbPermissionChecker;
import com.tommy.common.global.security.PasswordHasher;
import com.tommy.common.menu.entity.Menu;
import com.tommy.common.tenant.provision.ProvisionTenantCommand;
import com.tommy.common.tenant.provision.TenantProvisioningService;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class MyMenuApiIntegrationTest {

	private static final String NEXT = "N3w-Policy-Pass1";

	private static final List<String> ADMIN_MENUS = List.of("SYSTEM", "I18N", "COMMON_CODE", "COMPANY", "DEPT", "PERSON", "USER",
			"SUPPORT_ACCESS", "FILE_STORAGE", "FILE", "TEAM", "ROLE", "MENU", "PERMISSION");

	private static final List<String> LIMITED_MENUS = List.of("SYSTEM", "I18N", "COMMON_CODE", "COMPANY", "DEPT", "PERSON", "FILE",
			"TEAM");

	private static final List<String> HIDDEN_FROM_LIMITED = List.of("USER", "SUPPORT_ACCESS", "ROLE", "MENU", "PERMISSION");

	private static final List<String> ALL_ACTIONS = List.of("READ", "CREATE", "UPDATE", "DELETE", "EXPORT");

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private TenantProvisioningService provisioningService;

	@Autowired
	private DataSource dataSource;

	@Autowired
	private PasswordHasher passwordHasher;

	@Autowired
	private DbPermissionChecker permissions;

	private final List<String> tenantCodes = new ArrayList<>();

	private final List<String> schemas = new ArrayList<>();

	@DynamicPropertySource
	static void datasource(DynamicPropertyRegistry registry) {
		TestcontainersConfiguration.registerDatasource(registry);
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
	void returnsVisibleMenusForTheCurrentUser() throws Exception {
		String codeA = provision();
		String schemaA = schema(codeA);
		String temporary = provisioningService.provision(command(codeA)).temporaryPassword().reveal();
		MvcResult forced = login(codeA, "admin@example.com", temporary);
		mockMvc.perform(get("/api/mdm/v1/menus/my").header(HeaderTenantResolver.HEADER, codeA)
				.header("Authorization", "Bearer " + json(forced, "accessToken")))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.errorCode").value("AUTH.PASSWORD_CHANGE_REQUIRED"));
		MvcResult changed = mockMvc.perform(post("/api/mdm/v1/auth/password/change").header(HeaderTenantResolver.HEADER, codeA)
				.header("Authorization", "Bearer " + json(forced, "accessToken")).contentType(MediaType.APPLICATION_JSON)
				.content("{\"currentPassword\":\"" + temporary + "\",\"newPassword\":\"" + NEXT + "\"}"))
				.andExpect(status().isOk())
				.andReturn();
		execute(schemaA, """
				UPDATE i18n_message SET use_yn = 'N'
				WHERE message_key = 'FILE'
				  AND namespace_pk = (SELECT namespace_pk FROM i18n_namespace WHERE namespace_code = 'menu')
				""");
		execute(schemaA, "UPDATE menu SET menu_name = '파일원본' WHERE menu_code = 'FILE'");
		String adminAccess = json(changed, "accessToken");
		JsonNode korean = menus(codeA, adminAccess, "ko-KR", null);
		assertEquals(ADMIN_MENUS, codes(korean));
		assertTrue(menu(korean, "SYSTEM").get("parentMenuCode") == null || menu(korean, "SYSTEM").get("parentMenuCode").isNull());
		assertEquals("SYSTEM", menu(korean, "I18N").get("parentMenuCode").asString());
		assertTrue(menu(korean, "SYSTEM").get("iconName").isNull());
		assertEquals(1, menu(korean, "SYSTEM").get("levelNo").asInt());
		assertEquals(10, menu(korean, "SYSTEM").get("sortOrder").asInt());
		assertEquals(List.of("READ"), texts(menu(korean, "SYSTEM").get("actions")));
		assertEquals(ALL_ACTIONS, texts(menu(korean, "COMPANY").get("actions")));
		assertEquals("다국어", menu(korean, "I18N").get("menuName").asString());
		assertEquals("파일원본", menu(korean, "FILE").get("menuName").asString());
		JsonNode english = menus(codeA, adminAccess, null, "en-US");
		assertEquals("Languages", menu(english, "I18N").get("menuName").asString());
		assertEquals("파일원본", menu(english, "FILE").get("menuName").asString());

		long managerPk = insertUser(schemaA, "manager@example.com", "MANAGER");
		long userPk = insertUser(schemaA, "user@example.com", "USER");
		JsonNode manager = menus(codeA, json(login(codeA, "manager@example.com", NEXT), "accessToken"), "ko-KR", null);
		JsonNode user = menus(codeA, json(login(codeA, "user@example.com", NEXT), "accessToken"), "ko-KR", null);
		assertEquals(LIMITED_MENUS, codes(manager));
		assertEquals(LIMITED_MENUS, codes(user));
		for (String hidden : HIDDEN_FROM_LIMITED) {
			assertTrue(menu(manager, hidden) == null);
			assertTrue(menu(user, hidden) == null);
		}
		assertEquals(List.of("READ", "CREATE", "UPDATE", "EXPORT"), texts(menu(manager, "COMPANY").get("actions")));
		assertEquals(List.of("READ"), texts(menu(manager, "I18N").get("actions")));
		assertEquals(List.of("READ"), texts(menu(user, "COMPANY").get("actions")));
		assertEquals(List.of("READ"), texts(menu(user, "SYSTEM").get("actions")));

		execute(schemaA, "UPDATE user_role SET valid_to_date = DATE '2000-01-01' WHERE user_pk = " + userPk);
		permissions.evict(schemaA, userPk);
		assertEquals(List.of(), codes(menus(codeA, json(login(codeA, "user@example.com", NEXT), "accessToken"), null, null)));
		assertTrue(managerPk > 0);

		execute(schemaA, "UPDATE menu SET use_yn = 'N' WHERE menu_code = 'ROLE'");
		JsonNode withoutRole = menus(codeA, adminAccess, null, null);
		assertTrue(menu(withoutRole, "ROLE") == null);
		assertEquals("SYSTEM", menu(withoutRole, "SYSTEM").get("menuCode").asString());
		execute(schemaA, "UPDATE menu SET visible_yn = 'N' WHERE menu_type_code = 'PAGE'");
		assertEquals(List.of(), codes(menus(codeA, adminAccess, null, null)));

		String codeB = provision();
		String temporaryB = provisioningService.provision(command(codeB)).temporaryPassword().reveal();
		MvcResult forcedB = login(codeB, "admin@example.com", temporaryB);
		MvcResult changedB = mockMvc.perform(post("/api/mdm/v1/auth/password/change").header(HeaderTenantResolver.HEADER, codeB)
				.header("Authorization", "Bearer " + json(forcedB, "accessToken")).contentType(MediaType.APPLICATION_JSON)
				.content("{\"currentPassword\":\"" + temporaryB + "\",\"newPassword\":\"" + NEXT + "\"}"))
				.andExpect(status().isOk())
				.andReturn();
		JsonNode other = menus(codeB, json(changedB, "accessToken"), "ko-KR", null);
		assertEquals("역할", menu(other, "ROLE").get("menuName").asString());
		mockMvc.perform(get("/api/mdm/v1/menus/my").header(HeaderTenantResolver.HEADER, codeB)
				.header("Authorization", "Bearer " + adminAccess))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errorCode").value("TENANT.HOST_MISMATCH"));
		validateSchema(schemaA);
	}

	private JsonNode menus(String tenantCode, String access, String lang, String acceptLanguage) throws Exception {
		var request = get("/api/mdm/v1/menus/my").header(HeaderTenantResolver.HEADER, tenantCode)
				.header("Authorization", "Bearer " + access);
		if (lang != null) {
			request.param("lang", lang);
		}
		if (acceptLanguage != null) {
			request.header("Accept-Language", acceptLanguage);
		}
		MvcResult result = mockMvc.perform(request)
				.andExpect(status().isOk())
				.andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
				.andExpect(header().doesNotExist("ETag"))
				.andReturn();
		return json(result).get("data");
	}

	private long insertUser(String schema, String loginId, String roleCode) throws Exception {
		requireSchema(schema);
		try (Connection connection = dataSource.getConnection()) {
			IntegrationDatabase.requirePmis(connection);
			try (Statement statement = connection.createStatement()) {
				statement.execute("SET search_path TO " + schema);
			}
			long userPk;
			try (PreparedStatement statement = connection.prepareStatement("""
					INSERT INTO user_account (
					    account_type_code, login_id, password_hash, password_changed_datetime, password_expire_datetime,
					    must_change_password_yn, user_status_code, support_language_code, timezone_id)
					VALUES ('NORMAL', ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '30 days', 'N', 'ACTIVE', 'ko-KR', 'Asia/Seoul')
					RETURNING user_pk
					""")) {
				statement.setString(1, loginId);
				statement.setString(2, passwordHasher.hash(NEXT));
				try (ResultSet resultSet = statement.executeQuery()) {
					resultSet.next();
					userPk = resultSet.getLong(1);
				}
			}
			try (PreparedStatement statement = connection.prepareStatement("""
					INSERT INTO user_role (user_pk, role_pk, use_yn)
					SELECT ?, role_pk, 'Y' FROM role WHERE role_code = ?
					""")) {
				statement.setLong(1, userPk);
				statement.setString(2, roleCode);
				assertEquals(1, statement.executeUpdate());
			}
			return userPk;
		}
	}

	private MvcResult login(String tenantCode, String loginId, String password) throws Exception {
		return mockMvc.perform(post("/api/mdm/v1/auth/login").header(HeaderTenantResolver.HEADER, tenantCode)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"loginId\":\"" + loginId + "\",\"password\":\"" + password + "\"}"))
				.andExpect(status().isOk())
				.andReturn();
	}

	private void execute(String schema, String sql) throws Exception {
		requireSchema(schema);
		try (Connection connection = dataSource.getConnection()) {
			IntegrationDatabase.requirePmis(connection);
			try (Statement statement = connection.createStatement()) {
				statement.execute("SET search_path TO " + schema);
				statement.execute(sql);
			}
		}
	}

	private String provision() {
		String tenantCode = "it-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
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

	private static void requireSchema(String schema) {
		if (!schema.matches("^t_it_[a-z0-9]{16}$")) {
			throw new IllegalArgumentException("테스트 스키마가 아닙니다.");
		}
	}

	private static JsonNode menu(JsonNode data, String menuCode) {
		for (JsonNode item : data) {
			if (menuCode.equals(item.get("menuCode").asString())) {
				return item;
			}
		}
		return null;
	}

	private static List<String> codes(JsonNode data) {
		List<String> values = new ArrayList<>();
		for (JsonNode item : data) {
			values.add(item.get("menuCode").asString());
		}
		return values;
	}

	private static List<String> texts(JsonNode array) {
		List<String> values = new ArrayList<>();
		for (JsonNode item : array) {
			values.add(item.asString());
		}
		return values;
	}

	private static String json(MvcResult result, String field) throws Exception {
		return json(result).get("data").get(field).asString();
	}

	private static JsonNode json(MvcResult result) throws Exception {
		return JsonMapper.builder().build().readTree(result.getResponse().getContentAsString());
	}

	private void validateSchema(String schema) {
		StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
				.applySetting(AvailableSettings.DATASOURCE, dataSource)
				.applySetting(AvailableSettings.HBM2DDL_AUTO, "validate")
				.applySetting(AvailableSettings.DEFAULT_SCHEMA, schema)
				.applySetting(AvailableSettings.PHYSICAL_NAMING_STRATEGY, CamelCaseToUnderscoresNamingStrategy.class.getName())
				.applySetting(AvailableSettings.DIALECT, "org.hibernate.dialect.PostgreSQLDialect")
				.build();
		try (org.hibernate.SessionFactory sessionFactory = new MetadataSources(registry)
				.addAnnotatedClass(Menu.class)
				.buildMetadata()
				.buildSessionFactory()) {
			assertFalse(sessionFactory.getMetamodel().getEntities().isEmpty());
		}
		finally {
			StandardServiceRegistryBuilder.destroy(registry);
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
