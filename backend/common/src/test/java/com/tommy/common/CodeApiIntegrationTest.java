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
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.tommy.common.code.entity.CodeGroup;
import com.tommy.common.code.entity.CodeGroupLang;
import com.tommy.common.code.entity.CommonCode;
import com.tommy.common.code.entity.CommonCodeHistory;
import com.tommy.common.code.entity.CommonCodeLang;
import com.tommy.common.global.multitenancy.HeaderTenantResolver;
import com.tommy.common.global.security.JwtTokenService;
import com.tommy.common.tenant.provision.ProvisionTenantCommand;
import com.tommy.common.tenant.provision.TenantProvisioningService;

@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class CodeApiIntegrationTest {

	private static final String SCHEMA_PATTERN = "^t_it_[a-z0-9]{16}$";

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
	private DataSource dataSource;

	@Autowired
	private JwtTokenService jwtTokenService;

	private final List<String> tenantCodes = new ArrayList<>();
	private final Map<String, String> tokens = new HashMap<>();
	private final List<String> schemas = new ArrayList<>();

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
		}
	}

	@Test
	void codeApiFollowsTenantLanguageAndHistoryRules() throws Exception {
		String codeA = provision();
		String schemaA = "t_" + codeA.replace('-', '_');
		String codeB = provision();
		validateSchema(schemaA);
		mockMvc.perform(get("/api/mdm/v1/codes/MENU_TYPE").headers(auth(codeA)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[?(@.codeValue == 'GROUP')].codeName").exists());
		String original = baseName(codeB, "MENU_TYPE", "GROUP");
		mockMvc.perform(put("/api/mdm/v1/common-codes/" + codePk(codeA, "MENU_TYPE", "GROUP"))
				.headers(auth(codeA))
				.contentType(MediaType.APPLICATION_JSON)
				.content(codeJson("MENU_TYPE", "GROUP", "TENANT-A", null)))
				.andExpect(status().isOk());
		assertEquals("TENANT-A", baseName(codeA, "MENU_TYPE", "GROUP"));
		assertEquals(original, baseName(codeB, "MENU_TYPE", "GROUP"));
		mockMvc.perform(post("/api/mdm/v1/code-groups")
				.headers(auth(codeA))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"codeGroupCode":"HIER_IT","codeGroupName":"계층","hierarchy":true,"solutionCode":"MDM"}
						"""))
				.andExpect(status().isOk());
		mockMvc.perform(post("/api/mdm/v1/common-codes")
				.headers(auth(codeA))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"groupCode":"HIER_IT","codeValue":"ROOT","codeName":"기본","translations":[{"supportLanguageCode":"ko-KR","name":"한글"}]}
						"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.codePath").value("/ROOT"))
				.andExpect(jsonPath("$.data.levelNo").value(1));
		mockMvc.perform(get("/api/mdm/v1/codes/HIER_IT").headers(auth(codeA)).param("lang", "en-US"))
				.andExpect(jsonPath("$.data[0].codeName").value("한글"));
		mockMvc.perform(get("/api/mdm/v1/codes/HIER_IT").headers(auth(codeA))
				.header("Accept-Language", "en-US"))
				.andExpect(jsonPath("$.data[0].codeName").value("한글"));
		mockMvc.perform(get("/api/mdm/v1/codes/HIER_IT").headers(auth(codeA)).param("lang", "fr-FR"))
				.andExpect(jsonPath("$.data[0].codeName").value("기본"));
		long rootPk = codePk(codeA, "HIER_IT", "ROOT");
		mockMvc.perform(post("/api/mdm/v1/common-codes")
				.headers(auth(codeA))
				.contentType(MediaType.APPLICATION_JSON)
				.content(codeJson("HIER_IT", "CHILD", "자식", "ROOT")))
				.andExpect(jsonPath("$.data.codePath").value("/ROOT/CHILD"));
		mockMvc.perform(put("/api/mdm/v1/common-codes/" + rootPk)
				.headers(auth(codeA))
				.contentType(MediaType.APPLICATION_JSON)
				.content(codeJson("HIER_IT", "ROOT", "기본", "CHILD")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errorCode").value("CODE.CYCLE"));
		mockMvc.perform(post("/api/mdm/v1/code-groups")
				.headers(auth(codeA))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"codeGroupCode":"FLAT_IT","codeGroupName":"평면","hierarchy":false,"codeValueLength":2}
						"""))
				.andExpect(status().isOk());
		mockMvc.perform(post("/api/mdm/v1/common-codes")
				.headers(auth(codeA))
				.contentType(MediaType.APPLICATION_JSON)
				.content(codeJson("FLAT_IT", "ABC", "김", null)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errorCode").value("CODE.VALUE_LENGTH"));
		mockMvc.perform(post("/api/mdm/v1/common-codes")
				.headers(auth(codeA))
				.contentType(MediaType.APPLICATION_JSON)
				.content(codeJson("FLAT_IT", "AB", "김", "NO")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errorCode").value("CODE.PARENT_NOT_ALLOWED"));
		MvcResult created = mockMvc.perform(post("/api/mdm/v1/common-codes")
				.headers(auth(codeA))
				.contentType(MediaType.APPLICATION_JSON)
				.content(codeJson("FLAT_IT", "AB", "김", null)))
				.andExpect(status().isOk())
				.andReturn();
		long flatPk = json(created).get("data").get("codePk").asLong();
		mockMvc.perform(get("/api/mdm/v1/common-codes/" + flatPk + "/history").headers(auth(codeA)))
				.andExpect(jsonPath("$.data.items[0].changeTypeCode").value("C"))
				.andExpect(jsonPath("$.data.total").value(1));
		mockMvc.perform(put("/api/mdm/v1/common-codes/" + flatPk)
				.headers(auth(codeA))
				.contentType(MediaType.APPLICATION_JSON)
				.content(codeJson("FLAT_IT", "AB", "이", null).replace("}", ",\"changeReason\":\"이름\"}")))
				.andExpect(status().isOk());
		mockMvc.perform(delete("/api/mdm/v1/common-codes/" + flatPk).headers(auth(codeA)))
				.andExpect(status().isOk());
		mockMvc.perform(get("/api/mdm/v1/codes/FLAT_IT").headers(auth(codeA)))
				.andExpect(jsonPath("$.data[?(@.codeValue == 'AB')]").isEmpty());
		mockMvc.perform(post("/api/mdm/v1/common-codes/" + flatPk + "/restore").headers(auth(codeA)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.useYn").value("Y"));
		mockMvc.perform(get("/api/mdm/v1/common-codes/" + flatPk + "/history").headers(auth(codeA)))
				.andExpect(jsonPath("$.data.items[?(@.changeTypeCode == 'C')]").exists())
				.andExpect(jsonPath("$.data.items[?(@.changeTypeCode == 'U')]").exists())
				.andExpect(jsonPath("$.data.items[?(@.changeTypeCode == 'D')]").exists());
		mockMvc.perform(delete("/api/mdm/v1/common-codes/" + flatPk).headers(auth(codeA)))
				.andExpect(status().isOk());
		mockMvc.perform(post("/api/mdm/v1/common-codes")
				.headers(auth(codeA))
				.contentType(MediaType.APPLICATION_JSON)
				.content(codeJson("FLAT_IT", "AB", "박", null)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.useYn").value("Y"));
		mockMvc.perform(get("/api/mdm/v1/code-groups").headers(auth(codeA))
				.param("sort", "unknown,asc"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errorCode").value("COMMON.INVALID_SORT"));
		mockMvc.perform(get("/api/mdm/v1/code-groups").headers(auth(codeA))
				.param("page", "1").param("size", "5"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.page").value(1))
				.andExpect(jsonPath("$.data.size").value(5))
				.andExpect(jsonPath("$.data.total").isNumber());
		long policyPk = codePk(codeA, "SECURITY_POLICY", "TEMP_PASSWORD_VALID_DAYS");
		mockMvc.perform(put("/api/mdm/v1/common-codes/" + policyPk)
				.headers(auth(codeA))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"groupCode":"SECURITY_POLICY","codeValue":"TEMP_PASSWORD_VALID_DAYS","codeName":"임시 비밀번호 유효 일수","extAttr1":"0"}
						"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errorCode").value("CODE.INVALID_POLICY_VALUE"));
		mockMvc.perform(delete("/api/mdm/v1/common-codes/" + policyPk).headers(auth(codeA)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errorCode").value("CODE.INVALID_POLICY_VALUE"));
	}

	private String provision() throws Exception {
		String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
		String tenantCode = "it-" + suffix;
		String schema = "t_it_" + suffix;
		assertTrue(schema.matches(SCHEMA_PATTERN));
		tenantCodes.add(tenantCode);
		schemas.add(schema);
		provisioningService.provision(new ProvisionTenantCommand(
				tenantCode, "아이티", null, "ko-KR", "관리자", "admin@example.com", null));
		tokens.put(tenantCode, TestAccess.issueAdmin(jwtTokenService, dataSource, schema, tenantCode));
		return tenantCode;
	}

	private HttpHeaders auth(String tenantCode) {
		HttpHeaders headers = new HttpHeaders();
		headers.set(HeaderTenantResolver.HEADER, tenantCode);
		headers.setBearerAuth(tokens.get(tenantCode));
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
				.addAnnotatedClass(CodeGroup.class)
				.addAnnotatedClass(CodeGroupLang.class)
				.addAnnotatedClass(CommonCode.class)
				.addAnnotatedClass(CommonCodeLang.class)
				.addAnnotatedClass(CommonCodeHistory.class)
				.buildMetadata()
				.buildSessionFactory()) {
			assertFalse(sessionFactory.getMetamodel().getEntities().isEmpty());
		}
		finally {
			StandardServiceRegistryBuilder.destroy(registry);
		}
	}

	private String baseName(String tenantCode, String groupCode, String codeValue) throws Exception {
		MvcResult result = mockMvc.perform(get("/api/mdm/v1/common-codes")
				.headers(auth(tenantCode))
				.param("groupCode", groupCode)
				.param("size", "200"))
				.andExpect(status().isOk())
				.andReturn();
		for (tools.jackson.databind.JsonNode item : json(result).get("data").get("items")) {
			if (codeValue.equals(item.get("codeValue").asString())) {
				return item.get("codeName").asString();
			}
		}
		throw new IllegalStateException("코드가 없습니다.");
	}

	private long codePk(String tenantCode, String groupCode, String codeValue) throws Exception {
		MvcResult result = mockMvc.perform(get("/api/mdm/v1/common-codes")
				.headers(auth(tenantCode))
				.param("groupCode", groupCode)
				.param("size", "200"))
				.andExpect(status().isOk())
				.andReturn();
		for (tools.jackson.databind.JsonNode item : json(result).get("data").get("items")) {
			if (codeValue.equals(item.get("codeValue").asString())) {
				return item.get("codePk").asLong();
			}
		}
		throw new IllegalStateException("코드가 없습니다.");
	}

	private static tools.jackson.databind.JsonNode json(MvcResult result) throws Exception {
		return tools.jackson.databind.json.JsonMapper.builder().build()
				.readTree(result.getResponse().getContentAsString());
	}

	private static String codeJson(String groupCode, String codeValue, String codeName, String parent) {
		String parentJson = parent == null ? "" : ",\"parentCodeValue\":\"" + parent + "\"";
		return "{\"groupCode\":\"" + groupCode + "\",\"codeValue\":\"" + codeValue + "\",\"codeName\":\"" + codeName + "\""
				+ parentJson + "}";
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
