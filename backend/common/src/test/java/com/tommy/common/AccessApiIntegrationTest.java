package com.tommy.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
import com.tommy.common.tenant.provision.TenantProvisioningService;
import com.tommy.common.tenant.provision.TenantUpgradeService;

import tools.jackson.databind.JsonNode;

@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class AccessApiIntegrationTest {

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
	void schemaValidatesAndEarlierOrgFixesHold() throws Exception {
		String tenant = provision();
		validateSchema(schema(tenant));
		long parent = pk(mockMvc.perform(post("/api/mdm/v1/departments").headers(auth(tenant)).contentType(MediaType.APPLICATION_JSON)
				.content(dept(tenant, null, "HQ", "본부", "DIVISION", 1))).andExpect(status().isOk()).andReturn());
		long child = pk(mockMvc.perform(post("/api/mdm/v1/departments").headers(auth(tenant)).contentType(MediaType.APPLICATION_JSON)
				.content(dept(tenant, "HQ", "TEAM", "팀", "TEAM", 1))).andExpect(status().isOk()).andReturn());
		execute(schema(tenant), "UPDATE dept SET use_yn = 'N' WHERE dept_pk = " + parent);
		mockMvc.perform(put("/api/mdm/v1/departments/" + child).headers(auth(tenant)).contentType(MediaType.APPLICATION_JSON)
				.content(deptUpdate("HQ", "팀개명", "TEAM")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.deptName").value("팀개명"))
				.andExpect(jsonPath("$.data.parentDeptCode").value("HQ"));
		long person = pk(mockMvc.perform(post("/api/mdm/v1/people").headers(auth(tenant)).contentType(MediaType.APPLICATION_JSON)
				.content("{\"personName\":\"소속\"}")).andExpect(status().isOk()).andReturn());
		long ended = pk(mockMvc.perform(post("/api/mdm/v1/people/" + person + "/assignments").headers(auth(tenant))
				.contentType(MediaType.APPLICATION_JSON).content(assignment(tenant, "TEAM", "2026-01-01")))
				.andExpect(status().isOk()).andReturn());
		mockMvc.perform(post("/api/mdm/v1/person-assignments/" + ended + "/end").headers(auth(tenant))
				.contentType(MediaType.APPLICATION_JSON).content("{\"endDate\":\"2026-02-01\"}")).andExpect(status().isOk());
		mockMvc.perform(post("/api/mdm/v1/person-assignments/" + ended + "/end").headers(auth(tenant))
				.contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.errorCode").value("ASSIGNMENT.ALREADY_ENDED"));
		long inactive = pk(mockMvc.perform(post("/api/mdm/v1/people/" + person + "/assignments").headers(auth(tenant))
				.contentType(MediaType.APPLICATION_JSON).content(assignment(tenant, "TEAM", "2026-03-01")))
				.andExpect(status().isOk()).andReturn());
		mockMvc.perform(delete("/api/mdm/v1/person-assignments/" + inactive).headers(auth(tenant))).andExpect(status().isOk());
		mockMvc.perform(post("/api/mdm/v1/person-assignments/" + inactive + "/end").headers(auth(tenant))
				.contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.errorCode").value("ASSIGNMENT.INACTIVE"));
	}

	@Test
	void accountsRolesAndPermissionsFollowAccessRules() throws Exception {
		String tenant = provision();
		String other = provision();
		HttpHeaders admin = auth(tenant);
		mockMvc.perform(get("/api/mdm/v1/users").header(HeaderTenantResolver.HEADER, tenant))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.errorCode").value("AUTH.UNAUTHORIZED"));
		mockMvc.perform(get("/api/mdm/v1/users").headers(admin).param("sort", "password,asc"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errorCode").value("COMMON.INVALID_SORT"));
		long adminPk = userPk(schema(tenant), "admin@example.com");
		mockMvc.perform(post("/api/mdm/v1/users/" + adminPk + "/password/reset").headers(admin))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.errorCode").value("USER.SELF_OPERATION_FORBIDDEN"));
		mockMvc.perform(put("/api/mdm/v1/users/" + adminPk + "/roles").headers(admin).contentType(MediaType.APPLICATION_JSON)
				.content("[]"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.errorCode").value("USER.SELF_OPERATION_FORBIDDEN"));

		long person = pk(mockMvc.perform(post("/api/mdm/v1/people").headers(admin).contentType(MediaType.APPLICATION_JSON)
				.content("{\"personName\":\"홍길동\"}")).andExpect(status().isOk()).andReturn());
		MvcResult created = mockMvc.perform(post("/api/mdm/v1/users").headers(admin).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"loginId":" Staff.User@Example.com ","personPk":%d,"supportLanguageCode":"ko-KR","timezoneId":"Asia/Seoul","roleCodes":["USER"]}
						""".formatted(person)))
				.andExpect(status().isOk())
				.andExpect(header().string("Cache-Control", "no-store"))
				.andExpect(jsonPath("$.data.loginId").value("staff.user@example.com"))
				.andExpect(jsonPath("$.data.mustChangePasswordYn").value("Y"))
				.andReturn();
		JsonNode createdData = json(created).get("data");
		long userPk = createdData.get("userPk").asLong();
		String temporary = createdData.get("temporaryPassword").asText();
		assertTrue(temporary.length() >= 8);
		assertEquals("INITIAL", scalar(schema(tenant),
				"SELECT change_type_code FROM user_password_history WHERE user_pk = " + userPk + " ORDER BY password_history_pk"));
		assertFalse(scalar(schema(tenant), "SELECT password_expire_datetime::text FROM user_account WHERE user_pk = " + userPk).isBlank());
		login(tenant, "staff.user@example.com", temporary, true);
		mockMvc.perform(post("/api/mdm/v1/users").headers(admin).contentType(MediaType.APPLICATION_JSON)
				.content("{\"loginId\":\"STAFF.USER@example.com\"}"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.errorCode").value("USER.DUPLICATE_LOGIN_ID"));
		mockMvc.perform(post("/api/mdm/v1/users").headers(admin).contentType(MediaType.APPLICATION_JSON)
				.content("{\"loginId\":\"other@example.com\",\"personPk\":" + person + "}"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.errorCode").value("USER.PERSON_ALREADY_LINKED"));
		mockMvc.perform(delete("/api/mdm/v1/people/" + person).headers(admin))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.errorCode").value("PERSON.HAS_ACCOUNT"));
		mockMvc.perform(get("/api/mdm/v1/users/" + userPk).headers(admin))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.personName").value("홍길동"))
				.andExpect(jsonPath("$.data.passwordHash").doesNotExist())
				.andExpect(jsonPath("$.data.roles[?(@.roleCode == 'USER')].currentlyValid").value(true));
		mockMvc.perform(get("/api/mdm/v1/users").headers(admin).param("keyword", "홍길동"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.items[?(@.userPk == " + userPk + ")].roleCodes[0]").value("USER"));

		login(tenant, "staff.user@example.com", temporary, true);
		mockMvc.perform(put("/api/mdm/v1/users/" + userPk + "/status").headers(admin).contentType(MediaType.APPLICATION_JSON)
				.content("{\"userStatusCode\":\"LOCKED\"}")).andExpect(status().isOk());
		assertTrue(countSchema(schema(tenant),
				"SELECT count(*) FROM user_refresh_token WHERE user_pk = " + userPk + " AND revoke_reason_code = 'ADMIN'") >= 1);
		login(tenant, "staff.user@example.com", temporary, false);
		execute(schema(tenant), "UPDATE user_account SET login_fail_count = 4 WHERE user_pk = " + userPk);
		mockMvc.perform(put("/api/mdm/v1/users/" + userPk + "/status").headers(admin).contentType(MediaType.APPLICATION_JSON)
				.content("{\"userStatusCode\":\"ACTIVE\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.loginFailCount").value(0));
		mockMvc.perform(put("/api/mdm/v1/users/" + userPk + "/status").headers(admin).contentType(MediaType.APPLICATION_JSON)
				.content("{\"userStatusCode\":\"ACTIVE\"}"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.errorCode").value("USER.INVALID_STATUS_TRANSITION"));

		MvcResult reset = mockMvc.perform(post("/api/mdm/v1/users/" + userPk + "/password/reset").headers(admin))
				.andExpect(status().isOk())
				.andExpect(header().string("Cache-Control", "no-store"))
				.andReturn();
		String nextPassword = json(reset).get("data").get("temporaryPassword").asText();
		assertTrue(nextPassword.length() >= 8);
		assertFalse(nextPassword.equals(temporary));
		login(tenant, "staff.user@example.com", nextPassword, true);
		login(tenant, "staff.user@example.com", temporary, false);
		assertEquals("RESET", scalar(schema(tenant),
				"SELECT change_type_code FROM user_password_history WHERE user_pk = " + userPk + " ORDER BY password_history_pk DESC"));
		mockMvc.perform(put("/api/mdm/v1/users/" + userPk + "/status").headers(admin).contentType(MediaType.APPLICATION_JSON)
				.content("{\"userStatusCode\":\"LOCKED\"}")).andExpect(status().isOk());
		mockMvc.perform(post("/api/mdm/v1/users/" + userPk + "/password/reset").headers(admin)).andExpect(status().isOk());
		assertEquals("ACTIVE", scalar(schema(tenant), "SELECT user_status_code FROM user_account WHERE user_pk = " + userPk));

		mockMvc.perform(delete("/api/mdm/v1/users/" + userPk).headers(admin)).andExpect(status().isOk());
		assertEquals("WITHDRAWN", scalar(schema(tenant), "SELECT user_status_code FROM user_account WHERE user_pk = " + userPk));
		login(tenant, "staff.user@example.com", nextPassword, false);
		mockMvc.perform(post("/api/mdm/v1/users").headers(admin).contentType(MediaType.APPLICATION_JSON)
				.content("{\"loginId\":\"staff.user@example.com\"}"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.errorCode").value("USER.DUPLICATE_LOGIN_ID"));
		mockMvc.perform(post("/api/mdm/v1/users/" + userPk + "/restore").headers(admin))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.mustChangePasswordYn").value("Y"))
				.andExpect(jsonPath("$.data.userStatusCode").value("ACTIVE"));

		long gate = pk(mockMvc.perform(post("/api/mdm/v1/roles").headers(admin).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"roleCode":"GATE","roleName":"게이트","sortOrder":5,"translations":[{"supportLanguageCode":"en-US","text":"Gate"}]}
						""")).andExpect(status().isOk()).andReturn());
		mockMvc.perform(get("/api/mdm/v1/roles/" + gate).headers(admin))
				.andExpect(jsonPath("$.data.translations[?(@.supportLanguageCode == 'en-US')].text").value("Gate"));
		mockMvc.perform(post("/api/mdm/v1/roles").headers(admin).contentType(MediaType.APPLICATION_JSON)
				.content("{\"roleCode\":\"A\",\"roleName\":\"짧음\"}"))
				.andExpect(status().isBadRequest());
		mockMvc.perform(post("/api/mdm/v1/roles").headers(admin).contentType(MediaType.APPLICATION_JSON)
				.content("{\"roleCode\":\"GATE\",\"roleName\":\"중복\"}"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.errorCode").value("ROLE.DUPLICATE"));
		long tenantAdmin = rolePk(schema(tenant), "TENANT_ADMIN");
		mockMvc.perform(delete("/api/mdm/v1/roles/" + tenantAdmin).headers(admin))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.errorCode").value("ROLE.PROTECTED"));
		mockMvc.perform(put("/api/mdm/v1/roles/" + tenantAdmin + "/permissions").headers(admin).contentType(MediaType.APPLICATION_JSON)
				.content("{\"permissionCodes\":[]}"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.errorCode").value("ROLE.PROTECTED"));
		mockMvc.perform(put("/api/mdm/v1/users/" + userPk + "/roles").headers(admin).contentType(MediaType.APPLICATION_JSON)
				.content("[{\"roleCode\":\"GATE\"}]")).andExpect(status().isOk());
		mockMvc.perform(delete("/api/mdm/v1/roles/" + gate).headers(admin))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.errorCode").value("ROLE.IN_USE"));
		mockMvc.perform(get("/api/mdm/v1/roles").headers(admin).param("keyword", "GATE"))
				.andExpect(jsonPath("$.data.items[0].userCount").value(1));
		mockMvc.perform(put("/api/mdm/v1/users/" + userPk + "/roles").headers(admin).contentType(MediaType.APPLICATION_JSON)
				.content("[{\"roleCode\":\"GATE\",\"validFromDate\":\"2026-12-01\",\"validToDate\":\"2026-01-01\"}]"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errorCode").value("ROLE.INVALID_DATE"));
		mockMvc.perform(put("/api/mdm/v1/users/" + userPk + "/roles").headers(admin).contentType(MediaType.APPLICATION_JSON)
				.content("[{\"roleCode\":\"MISSING\"}]"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.errorCode").value("ROLE.NOT_FOUND"));
		mockMvc.perform(delete("/api/mdm/v1/roles/" + gate).headers(admin)).andExpect(status().isConflict());
		execute(schema(tenant), "UPDATE role SET use_yn = 'N' WHERE role_pk = " + gate);
		mockMvc.perform(put("/api/mdm/v1/users/" + userPk + "/roles").headers(admin).contentType(MediaType.APPLICATION_JSON)
				.content("[{\"roleCode\":\"GATE\"}]"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errorCode").value("ROLE.INACTIVE"));
		mockMvc.perform(post("/api/mdm/v1/roles/" + gate + "/restore").headers(admin)).andExpect(status().isOk());
		mockMvc.perform(put("/api/mdm/v1/users/" + userPk + "/roles").headers(admin).contentType(MediaType.APPLICATION_JSON)
				.content("[{\"roleCode\":\"GATE\",\"validFromDate\":\"2026-01-01\",\"validToDate\":\"2026-12-31\"}]"))
				.andExpect(status().isOk());
		mockMvc.perform(put("/api/mdm/v1/users/" + userPk + "/roles").headers(admin).contentType(MediaType.APPLICATION_JSON)
				.content("[]")).andExpect(status().isOk());
		mockMvc.perform(put("/api/mdm/v1/users/" + userPk + "/roles").headers(admin).contentType(MediaType.APPLICATION_JSON)
				.content("[{\"roleCode\":\"GATE\"}]")).andExpect(status().isOk());
		assertEquals(1, countSchema(schema(tenant),
				"SELECT count(*) FROM user_role ur JOIN role r ON r.role_pk = ur.role_pk WHERE ur.user_pk = " + userPk
						+ " AND r.role_code = 'GATE'"));

		mockMvc.perform(put("/api/mdm/v1/roles/" + gate + "/permissions").headers(admin).contentType(MediaType.APPLICATION_JSON)
				.content("{\"permissionCodes\":[\"COMPANY:UPDATE\"]}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errorCode").value("ROLE.READ_REQUIRED"))
				.andExpect(jsonPath("$.fieldErrors[?(@.field == 'COMPANY')]").exists());
		mockMvc.perform(put("/api/mdm/v1/roles/" + gate + "/permissions").headers(admin).contentType(MediaType.APPLICATION_JSON)
				.content("{\"permissionCodes\":[\"NOPE:READ\"]}"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.errorCode").value("PERMISSION.NOT_FOUND"));
		mockMvc.perform(put("/api/mdm/v1/roles/" + gate + "/permissions").headers(admin).contentType(MediaType.APPLICATION_JSON)
				.content("{\"permissionCodes\":[\"COMPANY:READ\",\"COMPANY:UPDATE\"]}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[?(@.permissionCode == 'COMPANY:READ')].granted").value(true));
		mockMvc.perform(put("/api/mdm/v1/roles/" + gate + "/permissions").headers(admin).contentType(MediaType.APPLICATION_JSON)
				.content("{\"permissionCodes\":[\"COMPANY:READ\"]}"))
				.andExpect(status().isOk());
		mockMvc.perform(put("/api/mdm/v1/roles/" + gate + "/permissions").headers(admin).contentType(MediaType.APPLICATION_JSON)
				.content("{\"permissionCodes\":[\"COMPANY:READ\",\"COMPANY:UPDATE\"]}"))
				.andExpect(status().isOk());
		assertEquals(1, countSchema(schema(tenant), """
				SELECT count(*) FROM role_permission rp
				JOIN permission p ON p.permission_pk = rp.permission_pk
				WHERE rp.role_pk = %d AND p.permission_code = 'COMPANY:UPDATE'
				""".formatted(gate)));
		HttpHeaders gateUser = bearer(tenant, userPk);
		mockMvc.perform(get("/api/mdm/v1/companies").headers(gateUser)).andExpect(status().isOk());
		mockMvc.perform(put("/api/mdm/v1/roles/" + gate + "/permissions").headers(admin).contentType(MediaType.APPLICATION_JSON)
				.content("{\"permissionCodes\":[]}")).andExpect(status().isOk());
		mockMvc.perform(get("/api/mdm/v1/companies").headers(gateUser))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.errorCode").value("AUTH.FORBIDDEN"));
		mockMvc.perform(put("/api/mdm/v1/users/" + userPk + "/roles").headers(admin).contentType(MediaType.APPLICATION_JSON)
				.content("[{\"roleCode\":\"USER\"}]")).andExpect(status().isOk());
		mockMvc.perform(get("/api/mdm/v1/companies").headers(gateUser)).andExpect(status().isOk());

		long approve = pk(mockMvc.perform(post("/api/mdm/v1/permissions").headers(admin).contentType(MediaType.APPLICATION_JSON)
				.content("{\"menuCode\":\"COMPANY\",\"actionCode\":\"APPROVE\",\"permissionName\":\"회사승인\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.permissionCode").value("COMPANY:APPROVE"))
				.andReturn());
		mockMvc.perform(post("/api/mdm/v1/permissions").headers(admin).contentType(MediaType.APPLICATION_JSON)
				.content("{\"menuCode\":\"SYSTEM\",\"actionCode\":\"APPROVE\",\"permissionName\":\"그룹\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errorCode").value("PERMISSION.MENU_NOT_PAGE"));
		mockMvc.perform(post("/api/mdm/v1/permissions").headers(admin).contentType(MediaType.APPLICATION_JSON)
				.content("{\"menuCode\":\"COMPANY\",\"actionCode\":\"APPROVE\",\"permissionName\":\"중복\"}"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.errorCode").value("PERMISSION.DUPLICATE"));
		long companyRead = permissionPk(schema(tenant), "COMPANY:READ");
		mockMvc.perform(delete("/api/mdm/v1/permissions/" + companyRead).headers(admin))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.errorCode").value("PERMISSION.PROTECTED"));
		mockMvc.perform(put("/api/mdm/v1/roles/" + gate + "/permissions").headers(admin).contentType(MediaType.APPLICATION_JSON)
				.content("{\"permissionCodes\":[\"COMPANY:READ\",\"COMPANY:APPROVE\"]}"))
				.andExpect(status().isOk());
		mockMvc.perform(delete("/api/mdm/v1/permissions/" + approve).headers(admin)).andExpect(status().isOk());
		assertEquals("N", scalar(schema(tenant),
				"SELECT rp.use_yn FROM role_permission rp WHERE rp.role_pk = " + gate
						+ " AND rp.permission_pk = " + approve));
		mockMvc.perform(put("/api/mdm/v1/companies/1").headers(bearer(tenant, userPk)).contentType(MediaType.APPLICATION_JSON)
				.content("{\"companyName\":\"없음\",\"companyTypeCode\":\"OWN\"}"))
				.andExpect(status().isForbidden());

		long operator = pk(mockMvc.perform(post("/api/mdm/v1/roles").headers(admin).contentType(MediaType.APPLICATION_JSON)
				.content("{\"roleCode\":\"OPERATOR\",\"roleName\":\"운영\"}")).andExpect(status().isOk()).andReturn());
		mockMvc.perform(put("/api/mdm/v1/roles/" + operator + "/permissions").headers(admin).contentType(MediaType.APPLICATION_JSON)
				.content("{\"permissionCodes\":[\"USER:READ\",\"USER:DELETE\",\"ROLE:READ\",\"ROLE:UPDATE\"]}"))
				.andExpect(status().isOk());
		MvcResult operatorUser = mockMvc.perform(post("/api/mdm/v1/users").headers(admin).contentType(MediaType.APPLICATION_JSON)
				.content("{\"loginId\":\"operator@example.com\",\"roleCodes\":[\"OPERATOR\"]}"))
				.andExpect(status().isOk()).andReturn();
		long operatorPk = json(operatorUser).get("data").get("userPk").asLong();
		HttpHeaders operatorHeaders = bearer(tenant, operatorPk);
		mockMvc.perform(delete("/api/mdm/v1/users/" + adminPk).headers(operatorHeaders))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.errorCode").value("USER.LAST_ADMIN"));
		mockMvc.perform(put("/api/mdm/v1/users/" + adminPk + "/status").headers(admin).contentType(MediaType.APPLICATION_JSON)
				.content("{\"userStatusCode\":\"LOCKED\"}"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.errorCode").value("USER.SELF_OPERATION_FORBIDDEN"));
		mockMvc.perform(put("/api/mdm/v1/users/" + adminPk + "/status").headers(operatorHeaders).contentType(MediaType.APPLICATION_JSON)
				.content("{\"userStatusCode\":\"DORMANT\"}"))
				.andExpect(status().isForbidden());
		mockMvc.perform(put("/api/mdm/v1/roles/" + operator + "/permissions").headers(admin).contentType(MediaType.APPLICATION_JSON)
				.content("{\"permissionCodes\":[\"USER:READ\",\"USER:UPDATE\",\"USER:DELETE\",\"ROLE:READ\",\"ROLE:UPDATE\"]}"))
				.andExpect(status().isOk());
		mockMvc.perform(put("/api/mdm/v1/users/" + adminPk + "/status").headers(operatorHeaders).contentType(MediaType.APPLICATION_JSON)
				.content("{\"userStatusCode\":\"DORMANT\"}"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.errorCode").value("USER.LAST_ADMIN"));
		mockMvc.perform(put("/api/mdm/v1/users/" + adminPk + "/roles").headers(operatorHeaders).contentType(MediaType.APPLICATION_JSON)
				.content("[]"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.errorCode").value("USER.LAST_ADMIN"));
		mockMvc.perform(get("/api/mdm/v1/users/" + userPk).headers(auth(other)))
				.andExpect(status().isNotFound());
		mockMvc.perform(get("/api/mdm/v1/users").headers(bearer(tenant, userPk)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.errorCode").value("AUTH.FORBIDDEN"));
	}

	@Test
	void upgradeAddsAccessMessagesWithoutOverwriting() throws Exception {
		String tenant = provision();
		String schema = schema(tenant);
		execute(schema, """
				DELETE FROM i18n_message_text WHERE message_pk IN (
				    SELECT m.message_pk FROM i18n_message m
				    JOIN i18n_namespace n ON n.namespace_pk = m.namespace_pk
				    WHERE n.namespace_code = 'error' AND m.message_key = 'USER.NOT_FOUND')
				""");
		execute(schema, "DELETE FROM i18n_message WHERE message_key = 'USER.NOT_FOUND'");
		execute(schema, "UPDATE i18n_message SET default_message = '바꿈' WHERE message_key = 'ROLE.DUPLICATE'");
		assertEquals("SUCCESS", upgradeService.upgradeTenants(List.of(tenant), null).results().stream()
				.filter(result -> tenant.equals(result.tenantCode())).findFirst().orElseThrow().executionStatus());
		assertEquals("사용자를 찾을 수 없습니다.", scalar(schema, "SELECT default_message FROM i18n_message WHERE message_key = 'USER.NOT_FOUND'"));
		assertEquals("바꿈", scalar(schema, "SELECT default_message FROM i18n_message WHERE message_key = 'ROLE.DUPLICATE'"));
		assertEquals("The role code already exists.", scalar(schema, """
				SELECT t.message_text FROM i18n_message_text t
				JOIN i18n_message m ON m.message_pk = t.message_pk
				WHERE m.message_key = 'ROLE.DUPLICATE' AND t.support_language_code = 'en-US'
				"""));
	}

	private void login(String tenant, String loginId, String password, boolean success) throws Exception {
		var action = mockMvc.perform(post("/api/mdm/v1/auth/login").header(HeaderTenantResolver.HEADER, tenant)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"loginId\":\"" + loginId + "\",\"password\":\"" + password + "\"}"));
		if (success) {
			action.andExpect(status().isOk()).andExpect(jsonPath("$.data.passwordChangeRequired").value(true));
		}
		else {
			action.andExpect(status().isUnauthorized()).andExpect(jsonPath("$.errorCode").value("AUTH.INVALID_CREDENTIALS"));
		}
	}

	private String provision() throws Exception {
		String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
		String tenantCode = "it-" + suffix;
		String schema = schema(tenantCode);
		assertTrue(schema.matches(SCHEMA_PATTERN));
		tenantCodes.add(tenantCode);
		schemas.add(schema);
		provisioningService.provision(new ProvisionTenantCommand(tenantCode, "아이티", null, "ko-KR", "관리자", "admin@example.com", null));
		tokens.put(tenantCode, TestAccess.issueAdmin(jwtTokenService, dataSource, schema, tenantCode));
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

	private static String schema(String tenantCode) {
		return "t_" + tenantCode.replace('-', '_');
	}

	private long userPk(String schema, String loginId) throws Exception {
		return Long.parseLong(scalar(schema, "SELECT user_pk FROM user_account WHERE login_id = '" + loginId + "'"));
	}

	private long rolePk(String schema, String roleCode) throws Exception {
		return Long.parseLong(scalar(schema, "SELECT role_pk FROM role WHERE role_code = '" + roleCode + "'"));
	}

	private long permissionPk(String schema, String permissionCode) throws Exception {
		return Long.parseLong(scalar(schema, "SELECT permission_pk FROM permission WHERE permission_code = '" + permissionCode + "'"));
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
			resultSet.next();
			return resultSet.getLong(1);
		}
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

	private static long pk(MvcResult result) throws Exception {
		return json(result).get("data").get(pkField(result)).asLong();
	}

	private static String pkField(MvcResult result) {
		String uri = result.getRequest().getRequestURI();
		if (uri.contains("/assignments")) {
			return "assignmentPk";
		}
		if (uri.contains("/people")) {
			return "personPk";
		}
		if (uri.contains("/departments")) {
			return "deptPk";
		}
		if (uri.contains("/users")) {
			return "userPk";
		}
		if (uri.contains("/roles")) {
			return "rolePk";
		}
		if (uri.contains("/permissions")) {
			return "permissionPk";
		}
		return "companyPk";
	}

	private static JsonNode json(MvcResult result) throws Exception {
		return tools.jackson.databind.json.JsonMapper.builder().build().readTree(result.getResponse().getContentAsString());
	}

	private static String dept(String company, String parent, String code, String name, String type, int sort) {
		String parentJson = parent == null ? "" : ",\"parentDeptCode\":\"" + parent + "\"";
		return "{\"companyCode\":\"" + company + "\"" + parentJson + ",\"deptCode\":\"" + code + "\",\"deptName\":\"" + name
				+ "\",\"deptTypeCode\":\"" + type + "\",\"sortOrder\":" + sort + "}";
	}

	private static String deptUpdate(String parent, String name, String type) {
		return "{\"parentDeptCode\":\"" + parent + "\",\"deptName\":\"" + name + "\",\"deptTypeCode\":\"" + type + "\"}";
	}

	private static String assignment(String company, String dept, String start) {
		return "{\"companyCode\":\"" + company + "\",\"deptCode\":\"" + dept + "\",\"employmentTypeCode\":\"REGULAR\","
				+ "\"assignmentStatusCode\":\"ACTIVE\",\"primaryYn\":\"N\",\"startDate\":\"" + start + "\"}";
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
