package com.tommy.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.sql.DataSource;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
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

import com.tommy.common.access.entity.Permission;
import com.tommy.common.access.entity.Role;
import com.tommy.common.access.entity.RolePermission;
import com.tommy.common.access.entity.UserAccount;
import com.tommy.common.access.entity.UserPasswordHistory;
import com.tommy.common.access.entity.UserRole;
import com.tommy.common.global.multitenancy.HeaderTenantResolver;
import com.tommy.common.global.security.JwtTokenService;
import com.tommy.common.i18n.entity.Nation;
import com.tommy.common.i18n.entity.SupportLanguage;
import com.tommy.common.menu.entity.Menu;
import com.tommy.common.org.entity.Company;
import com.tommy.common.org.entity.Dept;
import com.tommy.common.org.entity.DeptHistory;
import com.tommy.common.org.entity.Person;
import com.tommy.common.org.entity.PersonAssignment;
import com.tommy.common.tenant.provision.ProvisionTenantCommand;
import com.tommy.common.tenant.provision.ProvisioningResult;
import com.tommy.common.tenant.provision.TenantProvisioningService;
import com.tommy.common.tenant.provision.TenantUpgradeService;

import tools.jackson.databind.JsonNode;

@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class MenuApiIntegrationTest {

	private static final String SCHEMA_PATTERN = "^t_it_[a-z0-9]{16}$";

	private static Long realTenantCount;

	@DynamicPropertySource
	static void datasource(DynamicPropertyRegistry registry) {
		TestcontainersConfiguration.registerDatasource(registry);
		TestJwtKey.register(registry);
	}

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private TenantProvisioningService provisioningService;

	@Autowired
	private TenantUpgradeService upgradeService;

	@Autowired
	private DataSource dataSource;

	@Autowired
	private JwtTokenService jwtTokenService;

	private final List<String> tenantCodes = new ArrayList<>();

	private final Map<String, String> tokens = new HashMap<>();

	private final List<String> schemas = new ArrayList<>();

	@BeforeEach
	void rememberRealTenants() throws Exception {
		if (realTenantCount == null) {
			try (Connection connection = dataSource.getConnection()) {
				IntegrationDatabase.requirePmis(connection);
				realTenantCount = count(connection, "SELECT count(*) FROM pmis.tenant WHERE tenant_code NOT LIKE 'it-%'");
			}
		}
	}

	@AfterEach
	void cleanup() throws Exception {
		try (Connection connection = dataSource.getConnection()) {
			IntegrationDatabase.requirePmis(connection);
			for (String tenantCode : tenantCodes) {
				deleteTenant(connection, tenantCode);
			}
			for (String schema : schemas) {
				dropSchema(connection, schema);
			}
			assertEquals(realTenantCount, count(connection, "SELECT count(*) FROM pmis.tenant WHERE tenant_code NOT LIKE 'it-%'"));
		}
	}

	@Test
	void schemaValidatesAndAdminLockStaysUntilReleased() throws Exception {
		String tenant = provision();
		validateSchema(schema(tenant));
		HttpHeaders admin = auth(tenant);
		long userPk = userPk(schema(tenant), "admin@example.com");
		mockMvc.perform(get("/api/mdm/v1/users/" + userPk).headers(admin))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.lockType").value(org.hamcrest.Matchers.nullValue()));
		long person = pk(mockMvc.perform(post("/api/mdm/v1/people").headers(admin).contentType(MediaType.APPLICATION_JSON)
				.content("{\"personName\":\"잠금\"}")).andExpect(status().isOk()).andReturn(), "personPk");
		MvcResult created = mockMvc.perform(post("/api/mdm/v1/users").headers(admin).contentType(MediaType.APPLICATION_JSON)
				.content("{\"loginId\":\"lock.user@example.com\",\"personPk\":" + person + ",\"roleCodes\":[\"USER\"]}")).andExpect(status().isOk())
				.andReturn();
		String temporary = json(created).get("data").get("temporaryPassword").asString();
		long lockedUser = json(created).get("data").get("userPk").asLong();
		mockMvc.perform(put("/api/mdm/v1/users/" + lockedUser + "/status").headers(admin).contentType(MediaType.APPLICATION_JSON)
				.content("{\"userStatusCode\":\"LOCKED\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.lockType").value("ADMIN"))
				.andExpect(jsonPath("$.data.lockedDatetime").value(org.hamcrest.Matchers.nullValue()));
		login(tenant, "lock.user@example.com", temporary, false);
		mockMvc.perform(put("/api/mdm/v1/users/" + lockedUser + "/status").headers(admin).contentType(MediaType.APPLICATION_JSON)
				.content("{\"userStatusCode\":\"ACTIVE\"}")).andExpect(status().isOk());
		login(tenant, "lock.user@example.com", temporary, true);
		execute(schema(tenant), """
				UPDATE user_account
				SET user_status_code = 'LOCKED', locked_datetime = CURRENT_TIMESTAMP - INTERVAL '2 days', login_fail_count = 5
				WHERE user_pk = %d
				""".formatted(lockedUser));
		login(tenant, "lock.user@example.com", temporary, true);
	}

	@Test
	void menusFollowAdminRules() throws Exception {
		String tenant = provision();
		String other = provision();
		HttpHeaders admin = auth(tenant);
		mockMvc.perform(get("/api/mdm/v1/menus").header(HeaderTenantResolver.HEADER, tenant))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(get("/api/mdm/v1/menus/my").headers(admin)).andExpect(status().isOk())
				.andExpect(jsonPath("$.data").isArray());
		long menuLeaf = Long.parseLong(scalar(schema(tenant), "SELECT menu_pk FROM menu WHERE menu_code = 'MENU'"));
		mockMvc.perform(delete("/api/mdm/v1/menus/" + menuLeaf).headers(admin)).andExpect(status().isConflict())
				.andExpect(jsonPath("$.errorCode").value("MENU.PROTECTED"));
		long system = Long.parseLong(scalar(schema(tenant), "SELECT menu_pk FROM menu WHERE menu_code = 'SYSTEM'"));
		mockMvc.perform(put("/api/mdm/v1/menus/" + system).headers(admin).contentType(MediaType.APPLICATION_JSON)
				.content("{\"menuName\":\"시스템\",\"menuCode\":\"OTHER\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errorCode").value("COMMON.VALIDATION_FAILED"));

		long group = pk(mockMvc.perform(post("/api/mdm/v1/menus").headers(admin).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"solutionCode":"MDM","menuCode":"OPS","menuName":"운영","menuTypeCode":"GROUP","parentMenuCode":"SYSTEM","sortOrder":200}
						""")).andExpect(status().isOk()).andExpect(jsonPath("$.data.levelNo").value(2))
				.andExpect(jsonPath("$.data.menuPath").value("/SYSTEM/OPS")).andReturn(), "menuPk");
		MvcResult page = mockMvc.perform(post("/api/mdm/v1/menus").headers(admin).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"solutionCode":"MDM","menuCode":"OPS_BOARD","menuName":"게시판","menuTypeCode":"PAGE","parentMenuCode":"OPS","routePath":"/ops/board","sortOrder":1,"translations":[{"supportLanguageCode":"ko-KR","messageText":"게시판"},{"supportLanguageCode":"en-US","messageText":"Board"}]}
						""")).andExpect(status().isOk()).andExpect(jsonPath("$.data.menuPath").value("/SYSTEM/OPS/OPS_BOARD"))
				.andExpect(jsonPath("$.data.permissions.length()").value(5)).andReturn();
		long board = json(page).get("data").get("menuPk").asLong();
		assertEquals(5L, countSchema(schema(tenant), "SELECT count(*) FROM permission WHERE permission_code LIKE 'OPS_BOARD:%' AND use_yn = 'Y'"));
		mockMvc.perform(post("/api/mdm/v1/menus").headers(admin).contentType(MediaType.APPLICATION_JSON)
				.content("{\"solutionCode\":\"MDM\",\"menuCode\":\"OPS_BOARD\",\"menuName\":\"중복\",\"menuTypeCode\":\"GROUP\"}"))
				.andExpect(status().isConflict()).andExpect(jsonPath("$.errorCode").value("MENU.DUPLICATE"));
		mockMvc.perform(post("/api/mdm/v1/menus").headers(admin).contentType(MediaType.APPLICATION_JSON)
				.content("{\"solutionCode\":\"MDM\",\"menuCode\":\"OPS_DUP\",\"menuName\":\"중복경로\",\"menuTypeCode\":\"PAGE\",\"parentMenuCode\":\"OPS\",\"routePath\":\"/ops/board\"}"))
				.andExpect(status().isConflict()).andExpect(jsonPath("$.errorCode").value("MENU.DUPLICATE_ROUTE"));
		mockMvc.perform(post("/api/mdm/v1/menus").headers(admin).contentType(MediaType.APPLICATION_JSON)
				.content("{\"solutionCode\":\"MDM\",\"menuCode\":\"OPS_BAD\",\"menuName\":\"나쁨\",\"menuTypeCode\":\"PAGE\",\"parentMenuCode\":\"OPS_BOARD\",\"routePath\":\"/ops/bad\"}"))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.errorCode").value("MENU.PARENT_NOT_GROUP"));

		long child = pk(mockMvc.perform(post("/api/mdm/v1/menus").headers(admin).contentType(MediaType.APPLICATION_JSON)
				.content("{\"solutionCode\":\"MDM\",\"menuCode\":\"OPS_CHILD\",\"menuName\":\"하위\",\"menuTypeCode\":\"PAGE\",\"parentMenuCode\":\"OPS\",\"routePath\":\"/ops/child\",\"sortOrder\":2}"))
				.andExpect(status().isOk()).andReturn(), "menuPk");
		pk(mockMvc.perform(post("/api/mdm/v1/menus").headers(admin).contentType(MediaType.APPLICATION_JSON)
				.content("{\"solutionCode\":\"MDM\",\"menuCode\":\"OPS_SUB\",\"menuName\":\"하위그룹\",\"menuTypeCode\":\"GROUP\",\"parentMenuCode\":\"OPS\",\"sortOrder\":3}"))
				.andExpect(status().isOk()).andReturn(), "menuPk");
		mockMvc.perform(put("/api/mdm/v1/menus/" + group).headers(admin).contentType(MediaType.APPLICATION_JSON)
				.content("{\"menuName\":\"운영개명\",\"parentMenuCode\":\"OPS_SUB\"}"))
				.andExpect(status().isConflict()).andExpect(jsonPath("$.errorCode").value("MENU.PARENT_CYCLE"));
		execute(schema(tenant), "UPDATE menu SET use_yn = 'N' WHERE menu_pk = " + group);
		mockMvc.perform(put("/api/mdm/v1/menus/" + child).headers(admin).contentType(MediaType.APPLICATION_JSON)
				.content("{\"menuName\":\"하위개명\",\"parentMenuCode\":\"OPS\",\"routePath\":\"/ops/child\"}"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.data.menuName").value("하위개명"));
		execute(schema(tenant), "UPDATE menu SET use_yn = 'Y' WHERE menu_pk = " + group);
		mockMvc.perform(put("/api/mdm/v1/menus/sort-orders").headers(admin).contentType(MediaType.APPLICATION_JSON)
				.content("[{\"menuCode\":\"OPS_CHILD\",\"sortOrder\":9},{\"menuCode\":\"MISSING\",\"sortOrder\":1}]"))
				.andExpect(status().isNotFound());
		assertEquals("2", scalar(schema(tenant), "SELECT sort_order::text FROM menu WHERE menu_code = 'OPS_CHILD'"));
		mockMvc.perform(put("/api/mdm/v1/menus/sort-orders").headers(admin).contentType(MediaType.APPLICATION_JSON)
				.content("[{\"menuCode\":\"OPS_CHILD\",\"sortOrder\":9}]")).andExpect(status().isOk());
		assertEquals("9", scalar(schema(tenant), "SELECT sort_order::text FROM menu WHERE menu_code = 'OPS_CHILD'"));

		mockMvc.perform(delete("/api/mdm/v1/menus/" + group).headers(admin)).andExpect(status().isConflict())
				.andExpect(jsonPath("$.errorCode").value("MENU.HAS_CHILD"));
		long role = pk(mockMvc.perform(post("/api/mdm/v1/roles").headers(admin).contentType(MediaType.APPLICATION_JSON)
				.content("{\"roleCode\":\"BOARD_READER\",\"roleName\":\"게시판조회\"}")).andExpect(status().isOk()).andReturn(), "rolePk");
		mockMvc.perform(put("/api/mdm/v1/roles/" + role + "/permissions").headers(admin).contentType(MediaType.APPLICATION_JSON)
				.content("{\"permissionCodes\":[\"OPS_BOARD:READ\"]}")).andExpect(status().isOk());
		long person = pk(mockMvc.perform(post("/api/mdm/v1/people").headers(admin).contentType(MediaType.APPLICATION_JSON)
				.content("{\"personName\":\"메뉴\"}")).andExpect(status().isOk()).andReturn(), "personPk");
		MvcResult reader = mockMvc.perform(post("/api/mdm/v1/users").headers(admin).contentType(MediaType.APPLICATION_JSON)
				.content("{\"loginId\":\"menu.reader@example.com\",\"personPk\":" + person + ",\"roleCodes\":[\"BOARD_READER\"]}"))
				.andExpect(status().isOk()).andReturn();
		long readerPk = json(reader).get("data").get("userPk").asLong();
		HttpHeaders readerHeaders = bearer(tenant, readerPk);
		mockMvc.perform(get("/api/mdm/v1/menus/my").headers(readerHeaders).param("lang", "en-US"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.data[?(@.menuCode == 'OPS_BOARD')].menuName").value("Board"));
		mockMvc.perform(get("/api/mdm/v1/menus").headers(readerHeaders)).andExpect(status().isForbidden());
		mockMvc.perform(delete("/api/mdm/v1/menus/" + board).headers(admin)).andExpect(status().isOk());
		assertEquals("N", scalar(schema(tenant), "SELECT use_yn FROM permission WHERE permission_code = 'OPS_BOARD:READ'"));
		assertEquals(0L, countSchema(schema(tenant), """
				SELECT count(*) FROM role_permission rp
				JOIN permission p ON p.permission_pk = rp.permission_pk
				WHERE p.permission_code = 'OPS_BOARD:READ' AND rp.use_yn = 'Y'
				"""));
		mockMvc.perform(get("/api/mdm/v1/menus/my").headers(readerHeaders))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[?(@.menuCode == 'OPS_BOARD')]").isEmpty());
		mockMvc.perform(post("/api/mdm/v1/menus/" + board + "/restore").headers(admin)).andExpect(status().isOk());
		assertEquals("N", scalar(schema(tenant), "SELECT use_yn FROM permission WHERE permission_code = 'OPS_BOARD:READ'"));
		mockMvc.perform(get("/api/mdm/v1/menus").headers(auth(other)).param("keyword", "OPS_BOARD"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(0));

		execute(schema(tenant), "DELETE FROM i18n_message_text WHERE message_pk IN (SELECT message_pk FROM i18n_message WHERE message_key = 'MENU.NOT_FOUND')");
		execute(schema(tenant), "DELETE FROM i18n_message WHERE message_key = 'MENU.NOT_FOUND'");
		execute(schema(tenant), "UPDATE i18n_message SET default_message = '바꿈' WHERE message_key = 'MENU.DUPLICATE'");
		assertEquals("SUCCESS", upgradeService.upgradeTenants(List.of(tenant), null).results().stream()
				.filter(result -> tenant.equals(result.tenantCode())).findFirst().orElseThrow().executionStatus());
		assertEquals("메뉴를 찾을 수 없습니다.", scalar(schema(tenant), "SELECT default_message FROM i18n_message WHERE message_key = 'MENU.NOT_FOUND'"));
		assertEquals("바꿈", scalar(schema(tenant), "SELECT default_message FROM i18n_message WHERE message_key = 'MENU.DUPLICATE'"));
	}

	private void login(String tenant, String loginId, String password, boolean success) throws Exception {
		var action = mockMvc.perform(post("/api/mdm/v1/auth/login").header(HeaderTenantResolver.HEADER, tenant)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"loginId\":\"" + loginId + "\",\"password\":\"" + password + "\"}"));
		if (success) {
			action.andExpect(status().isOk());
		}
		else {
			action.andExpect(status().isUnauthorized()).andExpect(jsonPath("$.errorCode").value("AUTH.INVALID_CREDENTIALS"));
		}
	}

	private String provision() throws Exception {
		String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
		String tenantCode = "it-" + suffix;
		String schema = "t_" + tenantCode.replace('-', '_');
		assertTrue(schema.matches(SCHEMA_PATTERN));
		tenantCodes.add(tenantCode);
		schemas.add(schema);
		ProvisioningResult result = provisioningService.provision(new ProvisionTenantCommand(tenantCode, "아이티", null, "ko-KR", "관리자",
				"admin@example.com", null));
		tokens.put(tenantCode, TestAccess.issueAdmin(jwtTokenService, dataSource, schema, tenantCode));
		if (result.temporaryPassword() == null) {
			throw new IllegalStateException("테넌트 관리자 비밀번호가 없습니다.");
		}
		return tenantCode;
	}

	private HttpHeaders auth(String tenantCode) {
		HttpHeaders headers = new HttpHeaders();
		headers.set(HeaderTenantResolver.HEADER, tenantCode);
		headers.setBearerAuth(tokens.get(tenantCode));
		return headers;
	}

	private HttpHeaders bearer(String tenantCode, long userPk) {
		HttpHeaders headers = new HttpHeaders();
		headers.set(HeaderTenantResolver.HEADER, tenantCode);
		headers.setBearerAuth(jwtTokenService.issue(Long.toString(userPk), JwtTokenService.TENANT_AUDIENCE, JwtTokenService.TENANT_TYPE,
				tenantCode, false, Duration.ofMinutes(15)));
		return headers;
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
				.addAnnotatedClass(Company.class)
				.addAnnotatedClass(Dept.class)
				.addAnnotatedClass(DeptHistory.class)
				.addAnnotatedClass(Person.class)
				.addAnnotatedClass(PersonAssignment.class)
				.addAnnotatedClass(Nation.class)
				.addAnnotatedClass(SupportLanguage.class)
				.addAnnotatedClass(Menu.class)
				.addAnnotatedClass(UserAccount.class)
				.addAnnotatedClass(UserPasswordHistory.class)
				.addAnnotatedClass(Role.class)
				.addAnnotatedClass(UserRole.class)
				.addAnnotatedClass(Permission.class)
				.addAnnotatedClass(RolePermission.class)
				.buildMetadata()
				.buildSessionFactory()) {
			assertFalse(sessionFactory.getMetamodel().getEntities().isEmpty());
		}
		finally {
			StandardServiceRegistryBuilder.destroy(registry);
		}
	}

	private static String schema(String tenantCode) {
		return "t_" + tenantCode.replace('-', '_');
	}

	private long userPk(String schema, String loginId) throws Exception {
		return Long.parseLong(scalar(schema, "SELECT user_pk FROM user_account WHERE login_id = '" + loginId + "'"));
	}

	private void execute(String schema, String sql) throws Exception {
		try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
			IntegrationDatabase.requirePmis(connection);
			statement.execute("SET search_path TO " + schema);
			statement.execute(sql);
		}
	}

	private String scalar(String schema, String sql) throws Exception {
		try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
			IntegrationDatabase.requirePmis(connection);
			statement.execute("SET search_path TO " + schema);
			try (ResultSet resultSet = statement.executeQuery(sql)) {
				assertTrue(resultSet.next());
				return resultSet.getString(1);
			}
		}
	}

	private long countSchema(String schema, String sql) throws Exception {
		return Long.parseLong(scalar(schema, sql));
	}

	private static long count(Connection connection, String sql) throws Exception {
		try (Statement statement = connection.createStatement(); ResultSet resultSet = statement.executeQuery(sql)) {
			assertTrue(resultSet.next());
			return resultSet.getLong(1);
		}
	}

	private static long pk(MvcResult result, String field) throws Exception {
		return json(result).get("data").get(field).asLong();
	}

	private static JsonNode json(MvcResult result) throws Exception {
		return tools.jackson.databind.json.JsonMapper.builder().build().readTree(result.getResponse().getContentAsString());
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

	private static void dropSchema(Connection connection, String schema) throws Exception {
		if (!schema.matches(SCHEMA_PATTERN)) {
			throw new IllegalArgumentException("테스트 스키마명이 아닙니다.");
		}
		try (Statement statement = connection.createStatement()) {
			statement.execute("DROP SCHEMA IF EXISTS " + schema + " CASCADE");
		}
	}

}
