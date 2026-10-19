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

import com.tommy.common.global.multitenancy.HeaderTenantResolver;
import com.tommy.common.global.security.JwtTokenService;
import com.tommy.common.i18n.entity.I18nEntityText;
import com.tommy.common.i18n.entity.Nation;
import com.tommy.common.i18n.entity.SupportLanguage;
import com.tommy.common.org.entity.Company;
import com.tommy.common.org.entity.Dept;
import com.tommy.common.org.entity.DeptHistory;
import com.tommy.common.org.entity.Person;
import com.tommy.common.org.entity.PersonAssignment;
import com.tommy.common.tenant.provision.ProvisionTenantCommand;
import com.tommy.common.tenant.provision.TenantProvisioningService;
import com.tommy.common.tenant.provision.TenantUpgradeService;

@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class OrgApiIntegrationTest {

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
	void companiesFollowOrgRules() throws Exception {
		String tenant = provision();
		validateSchema(schema(tenant));
		long adminPk = userPk(schema(tenant), "admin@example.com");
		mockMvc.perform(get("/api/mdm/v1/companies").header(HeaderTenantResolver.HEADER, tenant))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.errorCode").value("AUTH.UNAUTHORIZED"));
		mockMvc.perform(get("/api/mdm/v1/companies").headers(auth(tenant)).param("sort", "unknown,asc"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errorCode").value("COMMON.INVALID_SORT"));
		long acme = pk(mockMvc.perform(post("/api/mdm/v1/companies").headers(auth(tenant)).contentType(MediaType.APPLICATION_JSON)
				.content(company("ACME", "에이씨엠이", "OWN", null, "KR", "123-45-67890", "ko-KR", "에이", "en-US", "Acme")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.businessRegistrationNo").value("123-45-67890"))
				.andExpect(jsonPath("$.data.nationCode").value("KR"))
				.andExpect(jsonPath("$.data.createUserPk").value(adminPk))
				.andExpect(jsonPath("$.data.translations[?(@.supportLanguageCode == 'en-US')].text").value("Acme"))
				.andReturn());
		mockMvc.perform(get("/api/mdm/v1/companies").headers(auth(tenant)).param("keyword", "ACME"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.items[0].companyCode").value("ACME"))
				.andExpect(jsonPath("$.data.items[0].translations").doesNotExist());
		mockMvc.perform(post("/api/mdm/v1/companies").headers(auth(tenant)).contentType(MediaType.APPLICATION_JSON)
				.content(company("ACME", "중복", "OWN", null, null, null, null, null, null, null)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.errorCode").value("COMPANY.DUPLICATE"));
		mockMvc.perform(post("/api/mdm/v1/companies").headers(auth(tenant)).contentType(MediaType.APPLICATION_JSON)
				.content(company("NOPE", "없음", "NOT_A_TYPE", null, null, null, null, null, null, null)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errorCode").value("ORG.INVALID_CODE_VALUE"));
		mockMvc.perform(post("/api/mdm/v1/companies").headers(auth(tenant)).contentType(MediaType.APPLICATION_JSON)
				.content(company("NATION", "국가", "CLIENT", null, "ZZ", null, null, null, null, null)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errorCode").value("COMMON.VALIDATION_FAILED"))
				.andExpect(jsonPath("$.fieldErrors[?(@.field == 'nationCode')]").exists());
		mockMvc.perform(delete("/api/mdm/v1/nations/US").headers(auth(tenant))).andExpect(status().isOk());
		mockMvc.perform(post("/api/mdm/v1/companies").headers(auth(tenant)).contentType(MediaType.APPLICATION_JSON)
				.content(company("USA", "미국", "CLIENT", null, "US", null, null, null, null, null)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors[?(@.field == 'nationCode')]").exists());
		mockMvc.perform(post("/api/mdm/v1/nations/US/restore").headers(auth(tenant))).andExpect(status().isOk());
		long sub = pk(mockMvc.perform(post("/api/mdm/v1/companies").headers(auth(tenant)).contentType(MediaType.APPLICATION_JSON)
				.content(company("SUB", "자회사", "AFFILIATE", "ACME", null, null, null, null, null, null)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.parentCompanyCode").value("ACME"))
				.andReturn());
		mockMvc.perform(put("/api/mdm/v1/companies/" + acme).headers(auth(tenant)).contentType(MediaType.APPLICATION_JSON)
				.content(companyUpdate("에이씨엠이", "OWN", "SUB", "123-45-67890")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.errorCode").value("COMPANY.PARENT_CYCLE"));
		mockMvc.perform(post("/api/mdm/v1/companies").headers(auth(tenant)).contentType(MediaType.APPLICATION_JSON)
				.content(company("DUPNO", "번호", "CLIENT", null, null, "123-45-67890", null, null, null, null)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.errorCode").value("COMPANY.DUPLICATE_BUSINESS_NO"));
		mockMvc.perform(delete("/api/mdm/v1/companies/" + acme).headers(auth(tenant)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.errorCode").value("COMPANY.HAS_CHILD_COMPANY"));
		mockMvc.perform(delete("/api/mdm/v1/companies/" + sub).headers(auth(tenant))).andExpect(status().isOk());
		long dept = pk(mockMvc.perform(post("/api/mdm/v1/departments").headers(auth(tenant)).contentType(MediaType.APPLICATION_JSON)
				.content(dept("ACME", null, "HQ", "본부", "DIVISION", 1)))
				.andExpect(status().isOk()).andReturn());
		mockMvc.perform(delete("/api/mdm/v1/companies/" + acme).headers(auth(tenant)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.errorCode").value("COMPANY.HAS_ACTIVE_DEPT"));
		mockMvc.perform(delete("/api/mdm/v1/departments/" + dept).headers(auth(tenant))).andExpect(status().isOk());
		long person = pk(mockMvc.perform(post("/api/mdm/v1/people").headers(auth(tenant)).contentType(MediaType.APPLICATION_JSON)
				.content("{\"personName\":\"김\"}")).andExpect(status().isOk()).andReturn());
		long assignment = pk(mockMvc.perform(post("/api/mdm/v1/people/" + person + "/assignments").headers(auth(tenant))
				.contentType(MediaType.APPLICATION_JSON).content(assignment("ACME", null, "REGULAR", "ACTIVE", "N", "2026-01-01", null)))
				.andExpect(status().isOk()).andReturn());
		mockMvc.perform(delete("/api/mdm/v1/companies/" + acme).headers(auth(tenant)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.errorCode").value("COMPANY.HAS_ACTIVE_ASSIGNMENT"));
		mockMvc.perform(post("/api/mdm/v1/person-assignments/" + assignment + "/end").headers(auth(tenant))
				.contentType(MediaType.APPLICATION_JSON).content("{\"endDate\":\"2026-02-01\"}")).andExpect(status().isOk());
		mockMvc.perform(delete("/api/mdm/v1/companies/" + acme).headers(auth(tenant))).andExpect(status().isOk());
		mockMvc.perform(post("/api/mdm/v1/companies").headers(auth(tenant)).contentType(MediaType.APPLICATION_JSON)
				.content(company("ACME", "재등록", "CLIENT", null, null, null, null, null, null, null)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.errorCode").value("COMPANY.DUPLICATE"));
		long reused = pk(mockMvc.perform(post("/api/mdm/v1/companies").headers(auth(tenant)).contentType(MediaType.APPLICATION_JSON)
				.content(company("REUSE", "재사용", "CLIENT", null, null, "123-45-67890", null, null, null, null)))
				.andExpect(status().isOk()).andReturn());
		mockMvc.perform(delete("/api/mdm/v1/companies/" + reused).headers(auth(tenant))).andExpect(status().isOk());
		long spare = pk(mockMvc.perform(post("/api/mdm/v1/companies").headers(auth(tenant)).contentType(MediaType.APPLICATION_JSON)
				.content(company("SPARE", "예비", "OWN", null, null, null, null, null, null, null)))
				.andExpect(status().isOk()).andReturn());
		for (long assignmentPk : currentAssignmentIds(tenant, personPkByName(tenant, "관리자"))) {
			mockMvc.perform(post("/api/mdm/v1/person-assignments/" + assignmentPk + "/end").headers(auth(tenant)))
					.andExpect(status().isOk());
		}
		long ownPk = ownCompanyPk(tenant, "it-");
		mockMvc.perform(delete("/api/mdm/v1/companies/" + ownPk).headers(auth(tenant))).andExpect(status().isOk());
		mockMvc.perform(delete("/api/mdm/v1/companies/" + spare).headers(auth(tenant)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.errorCode").value("COMPANY.LAST_OWN"));
	}

	@Test
	void departmentsKeepTreePathAndHistory() throws Exception {
		String tenant = provision();
		long adminPk = userPk(schema(tenant), "admin@example.com");
		mockMvc.perform(post("/api/mdm/v1/companies").headers(auth(tenant)).contentType(MediaType.APPLICATION_JSON)
				.content(company("ACME", "에이", "CLIENT", null, null, null, null, null, null, null))).andExpect(status().isOk());
		mockMvc.perform(post("/api/mdm/v1/companies").headers(auth(tenant)).contentType(MediaType.APPLICATION_JSON)
				.content(company("BETA", "베타", "CLIENT", null, null, null, null, null, null, null))).andExpect(status().isOk());
		long rootA = pk(mockMvc.perform(post("/api/mdm/v1/departments").headers(auth(tenant)).contentType(MediaType.APPLICATION_JSON)
				.content(dept("ACME", null, "A", "에이", "DIVISION", 1))).andExpect(status().isOk()).andReturn());
		MvcResult rootAResult = mockMvc.perform(get("/api/mdm/v1/departments/" + rootA).headers(auth(tenant))).andReturn();
		assertEquals("/" + rootA, json(rootAResult).get("data").get("deptPath").asString());
		long rootB = pk(mockMvc.perform(post("/api/mdm/v1/departments").headers(auth(tenant)).contentType(MediaType.APPLICATION_JSON)
				.content(dept("ACME", null, "B", "비", "DIVISION", 2))).andExpect(status().isOk()).andReturn());
		long child = pk(mockMvc.perform(post("/api/mdm/v1/departments").headers(auth(tenant)).contentType(MediaType.APPLICATION_JSON)
				.content(dept("ACME", "B", "C", "씨", "TEAM", 1))).andExpect(status().isOk()).andReturn());
		long grand = pk(mockMvc.perform(post("/api/mdm/v1/departments").headers(auth(tenant)).contentType(MediaType.APPLICATION_JSON)
				.content(dept("ACME", "C", "G", "지", "TEAM", 1))).andExpect(status().isOk()).andReturn());
		mockMvc.perform(post("/api/mdm/v1/departments").headers(auth(tenant)).contentType(MediaType.APPLICATION_JSON)
				.content(dept("BETA", null, "X", "엑스", "TEAM", 1))).andExpect(status().isOk());
		mockMvc.perform(get("/api/mdm/v1/departments/tree").headers(auth(tenant)).param("companyCode", "ACME"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[0].deptCode").value("A"))
				.andExpect(jsonPath("$.data[1].deptCode").value("B"))
				.andExpect(jsonPath("$.data[2].deptCode").value("C"))
				.andExpect(jsonPath("$.data[3].deptCode").value("G"));
		mockMvc.perform(put("/api/mdm/v1/departments/" + child).headers(auth(tenant)).contentType(MediaType.APPLICATION_JSON)
				.content(deptUpdate("A", "씨팀", "TEAM", "이름")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.deptPath").value("/" + rootA + "/" + child));
		mockMvc.perform(get("/api/mdm/v1/departments/" + grand).headers(auth(tenant)))
				.andExpect(jsonPath("$.data.deptPath").value("/" + rootA + "/" + child + "/" + grand))
				.andExpect(jsonPath("$.data.levelNo").value(3));
		mockMvc.perform(put("/api/mdm/v1/departments/" + child).headers(auth(tenant)).contentType(MediaType.APPLICATION_JSON)
				.content(deptUpdate("G", "씨팀", "TEAM", null)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.errorCode").value("DEPT.PARENT_CYCLE"));
		mockMvc.perform(put("/api/mdm/v1/departments/" + child).headers(auth(tenant)).contentType(MediaType.APPLICATION_JSON)
				.content(deptUpdate("X", "씨팀", "TEAM", null)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.errorCode").value("DEPT.PARENT_COMPANY_MISMATCH"));
		mockMvc.perform(get("/api/mdm/v1/departments/" + child + "/history").headers(auth(tenant)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.items[?(@.changeTypeCode == 'CREATE')]").exists())
				.andExpect(jsonPath("$.data.items[?(@.changeTypeCode == 'RENAME')]").exists())
				.andExpect(jsonPath("$.data.items[?(@.changeTypeCode == 'MOVE')]").exists())
				.andExpect(jsonPath("$.data.items[0].changeUserPk").value(adminPk));
		mockMvc.perform(post("/api/mdm/v1/departments").headers(auth(tenant)).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"companyCode":"ACME","deptCode":"FUTURE","deptName":"미래","deptTypeCode":"TEAM","validFromDate":"2999-01-01"}
						""")).andExpect(status().isOk());
		mockMvc.perform(get("/api/mdm/v1/departments/tree").headers(auth(tenant)).param("companyCode", "ACME")
				.param("asOfDate", "2020-01-01"))
				.andExpect(jsonPath("$.data[?(@.deptCode == 'FUTURE')]").isEmpty())
				.andExpect(jsonPath("$.data[?(@.deptCode == 'A')]").isEmpty());
		long person = pk(mockMvc.perform(post("/api/mdm/v1/people").headers(auth(tenant)).contentType(MediaType.APPLICATION_JSON)
				.content("{\"personName\":\"박\"}")).andExpect(status().isOk()).andReturn());
		long assignment = pk(mockMvc.perform(post("/api/mdm/v1/people/" + person + "/assignments").headers(auth(tenant))
				.contentType(MediaType.APPLICATION_JSON)
				.content(assignment("ACME", "G", "REGULAR", "ACTIVE", "N", "2026-01-01", null)))
				.andExpect(status().isOk()).andReturn());
		mockMvc.perform(delete("/api/mdm/v1/departments/" + child).headers(auth(tenant)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.errorCode").value("DEPT.HAS_CHILD"));
		mockMvc.perform(delete("/api/mdm/v1/departments/" + grand).headers(auth(tenant)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.errorCode").value("DEPT.HAS_ACTIVE_ASSIGNMENT"));
		mockMvc.perform(post("/api/mdm/v1/person-assignments/" + assignment + "/end").headers(auth(tenant))).andExpect(status().isOk());
		mockMvc.perform(delete("/api/mdm/v1/departments/" + grand).headers(auth(tenant))).andExpect(status().isOk());
		mockMvc.perform(get("/api/mdm/v1/departments/tree").headers(auth(tenant)).param("companyCode", "ACME"))
				.andExpect(jsonPath("$.data[?(@.deptCode == 'G')]").isEmpty());
		mockMvc.perform(get("/api/mdm/v1/departments/tree").headers(auth(tenant)).param("companyCode", "ACME")
				.param("includeClosed", "true"))
				.andExpect(jsonPath("$.data[?(@.deptCode == 'G')]").exists());
		mockMvc.perform(delete("/api/mdm/v1/departments/" + child).headers(auth(tenant))).andExpect(status().isOk());
		mockMvc.perform(delete("/api/mdm/v1/departments/" + rootA).headers(auth(tenant))).andExpect(status().isOk());
		mockMvc.perform(post("/api/mdm/v1/departments/" + child + "/restore").headers(auth(tenant)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.errorCode").value("DEPT.PARENT_INACTIVE"));
		mockMvc.perform(post("/api/mdm/v1/departments/" + rootA + "/restore").headers(auth(tenant))).andExpect(status().isOk());
		mockMvc.perform(post("/api/mdm/v1/departments/" + child + "/restore").headers(auth(tenant)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.useYn").value("Y"))
				.andExpect(jsonPath("$.data.validToDate").doesNotExist());
	}

	@Test
	void peopleAndAssignmentsFollowPrimaryAndBlockingRules() throws Exception {
		String tenant = provision();
		mockMvc.perform(post("/api/mdm/v1/companies").headers(auth(tenant)).contentType(MediaType.APPLICATION_JSON)
				.content(company("ACME", "에이", "CLIENT", null, null, null, null, null, null, null))).andExpect(status().isOk());
		mockMvc.perform(post("/api/mdm/v1/companies").headers(auth(tenant)).contentType(MediaType.APPLICATION_JSON)
				.content(company("BETA", "베타", "CLIENT", null, null, null, null, null, null, null))).andExpect(status().isOk());
		long sales = pk(mockMvc.perform(post("/api/mdm/v1/departments").headers(auth(tenant)).contentType(MediaType.APPLICATION_JSON)
				.content(dept("ACME", null, "SALES", "영업", "TEAM", 1))).andExpect(status().isOk()).andReturn());
		mockMvc.perform(post("/api/mdm/v1/departments").headers(auth(tenant)).contentType(MediaType.APPLICATION_JSON)
				.content(dept("ACME", null, "DEV", "개발", "TEAM", 2))).andExpect(status().isOk());
		mockMvc.perform(post("/api/mdm/v1/departments").headers(auth(tenant)).contentType(MediaType.APPLICATION_JSON)
				.content(dept("BETA", null, "SALES", "영업", "TEAM", 1))).andExpect(status().isOk());
		long person = pk(mockMvc.perform(post("/api/mdm/v1/people").headers(auth(tenant)).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"personName":"김","email":"kim@example.com","mobilePhoneNo":"010","translations":[
						{"supportLanguageCode":"ko-KR","text":"김"},{"supportLanguageCode":"en-US","text":"Kim"}]}
						""")).andExpect(status().isOk()).andReturn());
		mockMvc.perform(post("/api/mdm/v1/people").headers(auth(tenant)).contentType(MediaType.APPLICATION_JSON)
				.content("{\"personName\":\"같은메일\",\"email\":\"kim@example.com\"}")).andExpect(status().isOk());
		mockMvc.perform(put("/api/mdm/v1/people/" + person).headers(auth(tenant)).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"personName":"김","email":"kim@example.com","translations":[{"supportLanguageCode":"ko-KR","text":"김철수"}]}
						""")).andExpect(status().isOk());
		mockMvc.perform(get("/api/mdm/v1/people/" + person).headers(auth(tenant)))
				.andExpect(jsonPath("$.data.translations[?(@.supportLanguageCode == 'ko-KR')].text").value("김철수"))
				.andExpect(jsonPath("$.data.translations[?(@.supportLanguageCode == 'en-US')].text").value("Kim"));
		long first = pk(mockMvc.perform(post("/api/mdm/v1/people/" + person + "/assignments").headers(auth(tenant))
				.contentType(MediaType.APPLICATION_JSON)
				.content(assignment("ACME", "SALES", "REGULAR", "ACTIVE", "N", "2026-01-01", null).replace("}", ",\"employeeNo\":\"E001\"}")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.primaryYn").value("Y"))
				.andReturn());
		mockMvc.perform(post("/api/mdm/v1/people/" + person + "/assignments").headers(auth(tenant)).contentType(MediaType.APPLICATION_JSON)
				.content(assignment("ACME", "SALES", "REGULAR", "ACTIVE", "N", "2026-03-01", null)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.errorCode").value("ASSIGNMENT.DUPLICATE"));
		long second = pk(mockMvc.perform(post("/api/mdm/v1/people/" + person + "/assignments").headers(auth(tenant))
				.contentType(MediaType.APPLICATION_JSON)
				.content(assignment("ACME", "DEV", "REGULAR", "ACTIVE", "Y", "2026-03-01", null)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.primaryYn").value("Y"))
				.andReturn());
		mockMvc.perform(get("/api/mdm/v1/people").headers(auth(tenant)).param("keyword", "E001"))
				.andExpect(jsonPath("$.data.items[?(@.personPk == " + person + ")].primaryAssignment.deptCode").value("DEV"));
		mockMvc.perform(get("/api/mdm/v1/people/" + person + "/assignments").headers(auth(tenant)))
				.andExpect(jsonPath("$.data.items[?(@.assignmentPk == " + first + ")].primaryYn").value("N"));
		mockMvc.perform(post("/api/mdm/v1/people/" + person + "/assignments").headers(auth(tenant)).contentType(MediaType.APPLICATION_JSON)
				.content(assignment("BETA", "SALES", "REGULAR", "ACTIVE", "N", "2026-04-01", null).replace("}", ",\"employeeNo\":\"E001\"}")))
				.andExpect(status().isOk());
		mockMvc.perform(post("/api/mdm/v1/people/" + person + "/assignments").headers(auth(tenant)).contentType(MediaType.APPLICATION_JSON)
				.content(assignment("ACME", null, "REGULAR", "ACTIVE", "N", "2026-05-01", null).replace("}", ",\"employeeNo\":\"E001\"}")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.errorCode").value("ASSIGNMENT.EMPLOYEE_NO_DUPLICATE"));
		mockMvc.perform(post("/api/mdm/v1/people/" + person + "/assignments").headers(auth(tenant)).contentType(MediaType.APPLICATION_JSON)
				.content(assignment("BETA", "DEV", "REGULAR", "ACTIVE", "N", "2026-05-01", null)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.errorCode").value("ASSIGNMENT.DEPT_COMPANY_MISMATCH"));
		mockMvc.perform(post("/api/mdm/v1/people/" + person + "/assignments").headers(auth(tenant)).contentType(MediaType.APPLICATION_JSON)
				.content(assignment("ACME", null, "REGULAR", "ACTIVE", "N", "2026-05-01", null).replace("}", ",\"deptHeadYn\":\"Y\"}")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errorCode").value("ASSIGNMENT.DEPT_HEAD_REQUIRES_DEPT"));
		mockMvc.perform(post("/api/mdm/v1/people/" + person + "/assignments").headers(auth(tenant)).contentType(MediaType.APPLICATION_JSON)
				.content(assignment("ACME", null, "REGULAR", "RESIGNED", "N", "2026-05-01", null)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errorCode").value("ASSIGNMENT.RESIGNED_REQUIRES_END"));
		mockMvc.perform(post("/api/mdm/v1/common-codes").headers(auth(tenant)).contentType(MediaType.APPLICATION_JSON)
				.content("{\"groupCode\":\"POSITION\",\"codeValue\":\"OLD\",\"codeName\":\"옛\",\"validToDate\":\"2000-01-01\"}"))
				.andExpect(status().isOk());
		mockMvc.perform(post("/api/mdm/v1/people/" + person + "/assignments").headers(auth(tenant)).contentType(MediaType.APPLICATION_JSON)
				.content(assignment("ACME", "DEV", "REGULAR", "ACTIVE", "N", "2026-06-01", "2026-06-02").replace("}", ",\"positionCode\":\"OLD\"}")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errorCode").value("ORG.INVALID_CODE_VALUE"));
		mockMvc.perform(post("/api/mdm/v1/people").headers(auth(tenant)).contentType(MediaType.APPLICATION_JSON)
				.content("{\"personName\":\"번역없음\",\"translations\":[{\"supportLanguageCode\":\"xx-XX\",\"text\":\"x\"}]}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errorCode").value("I18N.LANGUAGE_NOT_FOUND"));
		mockMvc.perform(delete("/api/mdm/v1/people/" + person).headers(auth(tenant)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.errorCode").value("PERSON.HAS_ACTIVE_ASSIGNMENT"));
		mockMvc.perform(post("/api/mdm/v1/person-assignments/" + second + "/end").headers(auth(tenant))).andExpect(status().isOk());
		mockMvc.perform(get("/api/mdm/v1/people/" + person + "/assignments").headers(auth(tenant)))
				.andExpect(jsonPath("$.data.items[?(@.assignmentPk == " + first + ")].primaryYn").value("N"));
		for (long id : currentAssignmentIds(tenant, person)) {
			mockMvc.perform(post("/api/mdm/v1/person-assignments/" + id + "/end").headers(auth(tenant))).andExpect(status().isOk());
		}
		execute(schema(tenant), """
				INSERT INTO user_account (person_pk, account_type_code, login_id, user_status_code, support_language_code)
				SELECT %d, account_type_code, 'linked@example.com', user_status_code, support_language_code
				FROM user_account WHERE login_id = 'admin@example.com'
				""".formatted(person));
		mockMvc.perform(delete("/api/mdm/v1/people/" + person).headers(auth(tenant)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.errorCode").value("PERSON.HAS_ACCOUNT"));
		execute(schema(tenant), "UPDATE user_account SET use_yn = 'N' WHERE login_id = 'linked@example.com'");
		mockMvc.perform(delete("/api/mdm/v1/person-assignments/" + first).headers(auth(tenant))).andExpect(status().isOk());
		mockMvc.perform(post("/api/mdm/v1/person-assignments/" + first + "/restore").headers(auth(tenant)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.useYn").value("Y"));
		mockMvc.perform(delete("/api/mdm/v1/departments/" + sales).headers(auth(tenant))).andExpect(status().isOk());
		mockMvc.perform(delete("/api/mdm/v1/people/" + person).headers(auth(tenant))).andExpect(status().isOk());
		mockMvc.perform(post("/api/mdm/v1/people/" + person + "/restore").headers(auth(tenant)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.useYn").value("Y"));
	}

	@Test
	void permissionsAndTenantsStayIsolated() throws Exception {
		String tenantA = provision();
		String tenantB = provision();
		mockMvc.perform(post("/api/mdm/v1/companies").headers(auth(tenantA)).contentType(MediaType.APPLICATION_JSON)
				.content(company("ACME", "에이", "CLIENT", null, null, null, null, null, null, null))).andExpect(status().isOk());
		mockMvc.perform(get("/api/mdm/v1/companies").headers(auth(tenantB)).param("keyword", "ACME"))
				.andExpect(jsonPath("$.data.items[?(@.companyCode == 'ACME')]").isEmpty());
		execute(schema(tenantA), """
				INSERT INTO user_account (account_type_code, login_id, user_status_code, support_language_code)
				SELECT account_type_code, 'reader@example.com', user_status_code, support_language_code
				FROM user_account WHERE login_id = 'admin@example.com'
				""");
		execute(schema(tenantA), """
				INSERT INTO user_role (user_pk, role_pk)
				SELECT u.user_pk, r.role_pk FROM user_account u, role r
				WHERE u.login_id = 'reader@example.com' AND r.role_code = 'USER'
				""");
		execute(schema(tenantA), """
				INSERT INTO user_account (account_type_code, login_id, user_status_code, support_language_code)
				SELECT account_type_code, 'none@example.com', user_status_code, support_language_code
				FROM user_account WHERE login_id = 'admin@example.com'
				""");
		HttpHeaders reader = bearer(tenantA, userPk(schema(tenantA), "reader@example.com"));
		mockMvc.perform(get("/api/mdm/v1/companies").headers(reader)).andExpect(status().isOk());
		mockMvc.perform(post("/api/mdm/v1/companies").headers(reader).contentType(MediaType.APPLICATION_JSON)
				.content(company("BLOCK", "차단", "CLIENT", null, null, null, null, null, null, null)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.errorCode").value("AUTH.FORBIDDEN"));
		HttpHeaders none = bearer(tenantA, userPk(schema(tenantA), "none@example.com"));
		mockMvc.perform(get("/api/mdm/v1/companies").headers(none))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.errorCode").value("AUTH.FORBIDDEN"));
	}

	@Test
	void upgradeAddsOrgMessagesWithoutOverwriting() throws Exception {
		String tenant = provision();
		String schema = schema(tenant);
		execute(schema, """
				DELETE FROM i18n_message_text WHERE message_pk IN (
				    SELECT m.message_pk FROM i18n_message m
				    JOIN i18n_namespace n ON n.namespace_pk = m.namespace_pk
				    WHERE n.namespace_code = 'error' AND m.message_key = 'ORG.INVALID_CODE_VALUE')
				""");
		execute(schema, """
				DELETE FROM i18n_message WHERE message_key = 'ORG.INVALID_CODE_VALUE'
				""");
		execute(schema, """
				UPDATE i18n_message SET default_message = '바꿈'
				WHERE message_key = 'COMPANY.DUPLICATE'
				""");
		assertEquals("SUCCESS", upgradeService.upgradeTenants(List.of(tenant), null).results().stream()
				.filter(result -> tenant.equals(result.tenantCode())).findFirst().orElseThrow().executionStatus());
		assertEquals("사용할 수 없는 코드 값입니다.", scalar(schema, """
				SELECT default_message FROM i18n_message WHERE message_key = 'ORG.INVALID_CODE_VALUE'
				"""));
		assertEquals("바꿈", scalar(schema, "SELECT default_message FROM i18n_message WHERE message_key = 'COMPANY.DUPLICATE'"));
		assertEquals("The company code already exists.", scalar(schema, """
				SELECT t.message_text FROM i18n_message_text t
				JOIN i18n_message m ON m.message_pk = t.message_pk
				WHERE m.message_key = 'COMPANY.DUPLICATE' AND t.support_language_code = 'en-US'
				"""));
	}

	private long ownCompanyPk(String tenant, String codePrefix) throws Exception {
		MvcResult result = mockMvc.perform(get("/api/mdm/v1/companies").headers(auth(tenant)).param("companyTypeCode", "OWN")
				.param("useYn", "Y").param("size", "50")).andExpect(status().isOk()).andReturn();
		long found = 0;
		int count = 0;
		for (tools.jackson.databind.JsonNode item : json(result).get("data").get("items")) {
			if (item.get("companyCode").asString().startsWith(codePrefix)) {
				count++;
				found = item.get("companyPk").asLong();
			}
		}
		assertEquals(1, count);
		return found;
	}

	private long personPkByName(String tenant, String personName) throws Exception {
		MvcResult result = mockMvc.perform(get("/api/mdm/v1/people").headers(auth(tenant)).param("keyword", personName)
				.param("size", "20")).andExpect(status().isOk()).andReturn();
		for (tools.jackson.databind.JsonNode item : json(result).get("data").get("items")) {
			if (personName.equals(item.get("personName").asString())) {
				return item.get("personPk").asLong();
			}
		}
		throw new IllegalStateException("사람이 없습니다.");
	}

	private List<Long> currentAssignmentIds(String tenant, long personPk) throws Exception {
		MvcResult result = mockMvc.perform(get("/api/mdm/v1/people/" + personPk + "/assignments").headers(auth(tenant))
				.param("size", "50")).andExpect(status().isOk()).andReturn();
		List<Long> ids = new ArrayList<>();
		for (tools.jackson.databind.JsonNode item : json(result).get("data").get("items")) {
			if ("Y".equals(item.get("useYn").asString()) && (item.get("endDate") == null || item.get("endDate").isNull())) {
				ids.add(item.get("assignmentPk").asLong());
			}
		}
		return ids;
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
		try (Connection connection = dataSource.getConnection()) {
			IntegrationDatabase.requirePmis(connection);
			try (Statement statement = connection.createStatement()) {
				statement.execute("SET search_path TO " + schema);
				try (PreparedStatement query = connection.prepareStatement("SELECT user_pk FROM user_account WHERE login_id = ?")) {
					query.setString(1, loginId);
					try (ResultSet resultSet = query.executeQuery()) {
						assertTrue(resultSet.next());
						return resultSet.getLong(1);
					}
				}
			}
		}
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
				.addAnnotatedClass(I18nEntityText.class)
				.addAnnotatedClass(Nation.class)
				.addAnnotatedClass(SupportLanguage.class)
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
		return "companyPk";
	}

	private static tools.jackson.databind.JsonNode json(MvcResult result) throws Exception {
		return tools.jackson.databind.json.JsonMapper.builder().build().readTree(result.getResponse().getContentAsString());
	}

	private static String company(String code, String name, String type, String parent, String nation, String businessNo,
			String language, String text, String language2, String text2) {
		StringBuilder json = new StringBuilder();
		json.append("{\"companyCode\":\"").append(code).append("\",\"companyName\":\"").append(name)
				.append("\",\"companyTypeCode\":\"").append(type).append('"');
		if (parent != null) {
			json.append(",\"parentCompanyCode\":\"").append(parent).append('"');
		}
		if (nation != null) {
			json.append(",\"nationCode\":\"").append(nation).append('"');
		}
		if (businessNo != null) {
			json.append(",\"businessRegistrationNo\":\"").append(businessNo).append('"');
		}
		if (language != null) {
			json.append(",\"translations\":[{\"supportLanguageCode\":\"").append(language).append("\",\"text\":\"")
					.append(text).append("\"}");
			if (language2 != null) {
				json.append(",{\"supportLanguageCode\":\"").append(language2).append("\",\"text\":\"").append(text2).append("\"}");
			}
			json.append(']');
		}
		json.append('}');
		return json.toString();
	}

	private static String companyUpdate(String name, String type, String parent, String businessNo) {
		return "{\"companyName\":\"" + name + "\",\"companyTypeCode\":\"" + type + "\",\"parentCompanyCode\":\"" + parent
				+ "\",\"businessRegistrationNo\":\"" + businessNo + "\"}";
	}

	private static String dept(String company, String parent, String code, String name, String type, int sort) {
		String parentJson = parent == null ? "" : ",\"parentDeptCode\":\"" + parent + "\"";
		return "{\"companyCode\":\"" + company + "\"" + parentJson + ",\"deptCode\":\"" + code + "\",\"deptName\":\"" + name
				+ "\",\"deptTypeCode\":\"" + type + "\",\"sortOrder\":" + sort + "}";
	}

	private static String deptUpdate(String parent, String name, String type, String reason) {
		String reasonJson = reason == null ? "" : ",\"changeReason\":\"" + reason + "\"";
		return "{\"parentDeptCode\":\"" + parent + "\",\"deptName\":\"" + name + "\",\"deptTypeCode\":\"" + type + "\""
				+ reasonJson + "}";
	}

	private static String assignment(String company, String dept, String employment, String status, String primary, String start,
			String end) {
		String deptJson = dept == null ? "" : ",\"deptCode\":\"" + dept + "\"";
		String endJson = end == null ? "" : ",\"endDate\":\"" + end + "\"";
		return "{\"companyCode\":\"" + company + "\"" + deptJson + ",\"employmentTypeCode\":\"" + employment
				+ "\",\"assignmentStatusCode\":\"" + status + "\",\"primaryYn\":\"" + primary + "\",\"startDate\":\"" + start
				+ "\"" + endJson + "}";
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
