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
import java.sql.SQLException;
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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
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
import com.tommy.common.i18n.entity.I18nMessage;
import com.tommy.common.i18n.entity.I18nMessageText;
import com.tommy.common.i18n.entity.I18nNamespace;
import com.tommy.common.i18n.entity.Nation;
import com.tommy.common.i18n.entity.SupportLanguage;
import com.tommy.common.tenant.provision.ProvisionTenantCommand;
import com.tommy.common.tenant.provision.TenantProvisioningService;

@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class I18nApiIntegrationTest {

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
	void i18nApiTranslatesBundlesAndReusesDeletedKeys() throws Exception {
		String codeA = provision();
		String schemaA = "t_" + codeA.replace('-', '_');
		String codeB = provision();
		validateSchema(schemaA);
		mockMvc.perform(get("/api/mdm/v1/i18n/languages").headers(auth(codeA)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data").isArray())
				.andExpect(jsonPath("$.data[?(@.supportLanguageCode == 'ko-KR')].defaultLanguage").value(true));
		mockMvc.perform(get("/api/mdm/v1/nations").headers(auth(codeA)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[?(@.nationCode == 'KR')].nationName").value("한국"));
		mockMvc.perform(get("/api/mdm/v1/i18n/bundles/error").headers(auth(codeA)).param("lang", "ko-KR"))
				.andExpect(jsonPath("$.data.messages['COMMON.VALIDATION_FAILED']").value("요청 값이 올바르지 않습니다."));
		mockMvc.perform(get("/api/mdm/v1/i18n/bundles/error").headers(auth(codeA)).param("lang", "en-US"))
				.andExpect(jsonPath("$.data.messages['COMMON.VALIDATION_FAILED']").value("The request values are invalid."));
		mockMvc.perform(get("/api/mdm/v1/i18n/bundles/error").headers(auth(codeA)).header("Accept-Language", "en-US"))
				.andExpect(jsonPath("$.data.messages['COMMON.VALIDATION_FAILED']").value("The request values are invalid."));
		mockMvc.perform(get("/api/mdm/v1/i18n/bundles/error").headers(auth(codeA)).param("lang", "fr-FR"))
				.andExpect(jsonPath("$.data.messages['COMMON.VALIDATION_FAILED']").value("요청 값이 올바르지 않습니다."));
		MvcResult cached = mockMvc.perform(get("/api/mdm/v1/i18n/bundles/error").headers(auth(codeA)).param("lang", "ko-KR"))
				.andExpect(status().isOk())
				.andExpect(header().exists("ETag"))
				.andReturn();
		mockMvc.perform(get("/api/mdm/v1/i18n/bundles/error").headers(auth(codeA)).param("lang", "ko-KR")
				.header("If-None-Match", cached.getResponse().getHeader("ETag")))
				.andExpect(status().isNotModified());
		mockMvc.perform(get("/api/mdm/v1/i18n/bundles").headers(auth(codeA)).param("namespaces", "error,menu"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.error.namespaceCode").value("error"));
		mockMvc.perform(get("/api/mdm/v1/i18n/bundles/missing").headers(auth(codeA)).param("lang", "en-US"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.errorCode").value("I18N.NAMESPACE_NOT_FOUND"))
				.andExpect(jsonPath("$.errorMessage").value("The namespace was not found."));
		long validationPk = messagePk(codeA, "COMMON.VALIDATION_FAILED");
		mockMvc.perform(put("/api/mdm/v1/i18n-messages/" + validationPk)
				.headers(auth(codeA))
				.contentType(MediaType.APPLICATION_JSON)
				.content(messageJson("COMMON.VALIDATION_FAILED", "요청 값이 올바르지 않습니다.", "ko-KR", "테넌트A", "APPROVED")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.translations[?(@.supportLanguageCode == 'ko-KR')].versionNo").value(1));
		mockMvc.perform(get("/api/mdm/v1/i18n/bundles/error").headers(auth(codeA)).param("lang", "ko-KR"))
				.andExpect(jsonPath("$.data.messages['COMMON.VALIDATION_FAILED']").value("테넌트A"));
		mockMvc.perform(get("/api/mdm/v1/i18n/bundles/error").headers(auth(codeB)).param("lang", "ko-KR"))
				.andExpect(jsonPath("$.data.messages['COMMON.VALIDATION_FAILED']").value("요청 값이 올바르지 않습니다."));
		mockMvc.perform(put("/api/mdm/v1/i18n-messages/" + validationPk)
				.headers(auth(codeA))
				.contentType(MediaType.APPLICATION_JSON)
				.content(messageJson("COMMON.VALIDATION_FAILED", "요청 값이 올바르지 않습니다.", "ko-KR", "테넌트A", "REVIEWED")))
				.andExpect(jsonPath("$.data.translations[?(@.supportLanguageCode == 'ko-KR')].versionNo").value(1));
		mockMvc.perform(post("/api/mdm/v1/i18n-namespaces")
				.headers(auth(codeA))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"namespaceCode\":\"ITBOX\",\"namespaceName\":\"테스트\"}"))
				.andExpect(status().isOk());
		mockMvc.perform(post("/api/mdm/v1/i18n-messages")
				.headers(auth(codeA))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"namespaceCode\":\"ITBOX\",\"messageKey\":\"PLAIN\",\"messageTypeCode\":\"MESSAGE\",\"defaultMessage\":\"기본문구\"}"))
				.andExpect(status().isOk());
		mockMvc.perform(get("/api/mdm/v1/i18n/bundles/ITBOX").headers(auth(codeA)).param("lang", "en-US"))
				.andExpect(jsonPath("$.data.messages.PLAIN").value("기본문구"));
		mockMvc.perform(post("/api/mdm/v1/i18n-messages")
				.headers(auth(codeA))
				.contentType(MediaType.APPLICATION_JSON)
				.content(messageJson("ITBOX", "KO", "기본문구", "ko-KR", "한글문구", "APPROVED").replace(
						"\"namespaceCode\":\"error\"", "\"namespaceCode\":\"ITBOX\"")))
				.andExpect(status().isOk());
		mockMvc.perform(get("/api/mdm/v1/i18n/bundles/ITBOX").headers(auth(codeA)).param("lang", "en-US"))
				.andExpect(jsonPath("$.data.messages.KO").value("한글문구"));
		long samplePk = createMessage(codeA, "ITBOX", "SAMPLE");
		mockMvc.perform(delete("/api/mdm/v1/i18n-messages/" + samplePk).headers(auth(codeA)))
				.andExpect(status().isOk());
		mockMvc.perform(get("/api/mdm/v1/i18n/bundles/ITBOX").headers(auth(codeA)).param("lang", "ko-KR"))
				.andExpect(jsonPath("$.data.messages.SAMPLE").doesNotExist());
		createMessage(codeA, "ITBOX", "SAMPLE");
		mockMvc.perform(post("/api/mdm/v1/i18n-messages")
				.headers(auth(codeA))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"namespaceCode\":\"ITBOX\",\"messageKey\":\"SAMPLE\",\"messageTypeCode\":\"MESSAGE\",\"defaultMessage\":\"다시\"}"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.errorCode").value("I18N.DUPLICATE"));
		mockMvc.perform(post("/api/mdm/v1/support-languages")
				.headers(auth(codeA))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"supportLanguageCode":"ja-JP","languageName":"Japanese","nativeName":"日本語","textDirectionCode":"LTR","defaultLanguage":true,"fallbackLanguageCode":"en-US"}
						"""))
				.andExpect(status().isOk());
		mockMvc.perform(get("/api/mdm/v1/support-languages").headers(auth(codeA)).param("useYn", "Y"))
				.andExpect(jsonPath("$.data[?(@.defaultLanguage == true)]").value(org.hamcrest.Matchers.hasSize(1)));
		mockMvc.perform(delete("/api/mdm/v1/support-languages/ko-KR").headers(auth(codeA)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.errorCode").value("I18N.LANGUAGE_IN_USE"));
		mockMvc.perform(delete("/api/mdm/v1/support-languages/ja-JP").headers(auth(codeA)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.errorCode").value("I18N.LANGUAGE_IN_USE"));
		mockMvc.perform(put("/api/mdm/v1/support-languages/ko-KR")
				.headers(auth(codeA))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"supportLanguageCode":"ko-KR","languageName":"한국어","nativeName":"한국어","textDirectionCode":"LTR","defaultLanguage":false,"fallbackLanguageCode":"en-US"}
						"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errorCode").value("I18N.FALLBACK_CYCLE"));
		reusePartialUniques(schemaA);
	}

	private long createMessage(String tenantCode, String namespace, String key) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/mdm/v1/i18n-messages")
				.headers(auth(tenantCode))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"namespaceCode\":\"" + namespace + "\",\"messageKey\":\"" + key
						+ "\",\"messageTypeCode\":\"MESSAGE\",\"defaultMessage\":\"값\"}"))
				.andExpect(status().isOk())
				.andReturn();
		return json(result).get("data").get("messagePk").asLong();
	}

	private long messagePk(String tenantCode, String messageKey) throws Exception {
		MvcResult result = mockMvc.perform(get("/api/mdm/v1/i18n-messages")
				.headers(auth(tenantCode))
				.param("namespaceCode", "error")
				.param("keyword", messageKey)
				.param("size", "50"))
				.andExpect(status().isOk())
				.andReturn();
		for (tools.jackson.databind.JsonNode item : json(result).get("data").get("items")) {
			if (messageKey.equals(item.get("messageKey").asString())) {
				return item.get("messagePk").asLong();
			}
		}
		throw new IllegalStateException("메시지가 없습니다.");
	}

	private static String messageJson(String key, String defaultMessage, String language, String text, String status) {
		return messageJson("error", key, defaultMessage, language, text, status);
	}

	private static String messageJson(String namespace, String key, String defaultMessage, String language, String text, String status) {
		return "{\"namespaceCode\":\"" + namespace + "\",\"messageKey\":\"" + key + "\",\"messageTypeCode\":\"ERROR\",\"defaultMessage\":\""
				+ defaultMessage + "\",\"translations\":[{\"supportLanguageCode\":\"" + language + "\",\"messageText\":\"" + text
				+ "\",\"translationStatusCode\":\"" + status + "\"}]}";
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
				.addAnnotatedClass(SupportLanguage.class)
				.addAnnotatedClass(Nation.class)
				.addAnnotatedClass(I18nNamespace.class)
				.addAnnotatedClass(I18nMessage.class)
				.addAnnotatedClass(I18nMessageText.class)
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

	private void reusePartialUniques(String schema) throws Exception {
		try (Connection connection = dataSource.getConnection()) {
			IntegrationDatabase.requirePmis(connection);
			try (Statement statement = connection.createStatement()) {
				statement.execute("SET search_path TO " + schema);
			}
			reusePair(connection, "SELECT user_pk, role_pk FROM user_role WHERE use_yn = 'Y' LIMIT 1",
					"UPDATE user_role SET use_yn = 'N' WHERE user_pk = ? AND role_pk = ?",
					"INSERT INTO user_role (user_pk, role_pk) VALUES (?, ?)");
			reusePair(connection, "SELECT role_pk, permission_pk FROM role_permission WHERE use_yn = 'Y' LIMIT 1",
					"UPDATE role_permission SET use_yn = 'N' WHERE role_pk = ? AND permission_pk = ?",
					"INSERT INTO role_permission (role_pk, permission_pk) VALUES (?, ?)");
			reuseFileAttachment(connection);
			reuseExternalMap(connection);
			reuseEmployeeNo(connection);
			reusePersonAccount(connection);
		}
	}

	private static void reusePair(Connection connection, String select, String disable, String insert) throws Exception {
		long left;
		long right;
		try (Statement statement = connection.createStatement(); ResultSet resultSet = statement.executeQuery(select)) {
			assertTrue(resultSet.next());
			left = resultSet.getLong(1);
			right = resultSet.getLong(2);
		}
		try (PreparedStatement statement = connection.prepareStatement(disable)) {
			statement.setLong(1, left);
			statement.setLong(2, right);
			statement.executeUpdate();
		}
		insertPair(connection, insert, left, right);
		expectUnique(connection, insert, left, right);
	}

	private static void insertPair(Connection connection, String sql, long left, long right) throws Exception {
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setLong(1, left);
			statement.setLong(2, right);
			statement.executeUpdate();
		}
	}

	private static void expectUnique(Connection connection, String sql, long left, long right) throws Exception {
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setLong(1, left);
			statement.setLong(2, right);
			statement.executeUpdate();
			throw new AssertionError("사용 중인 중복이 거절되지 않았습니다.");
		}
		catch (SQLException ex) {
			assertEquals("23505", ex.getSQLState());
		}
	}

	private static void reuseFileAttachment(Connection connection) throws Exception {
		long storagePk;
		try (Statement statement = connection.createStatement();
				ResultSet resultSet = statement.executeQuery("""
						INSERT INTO file_storage (storage_code, storage_name, storage_type_code, base_path)
						VALUES ('ITSTORE', '테스트', 'LOCAL', '/tmp') RETURNING storage_pk
						""")) {
			resultSet.next();
			storagePk = resultSet.getLong(1);
		}
		long filePk;
		try (PreparedStatement statement = connection.prepareStatement("""
				INSERT INTO file (storage_pk, storage_key, original_file_name, file_size, file_status_code, upload_datetime)
				VALUES (?, 'it-key', 'a.txt', 1, 'TEMP', CURRENT_TIMESTAMP) RETURNING file_pk
				""")) {
			statement.setLong(1, storagePk);
			try (ResultSet resultSet = statement.executeQuery()) {
				resultSet.next();
				filePk = resultSet.getLong(1);
			}
		}
		String insert = "INSERT INTO file_attachment (file_pk, target_table_name, target_key) VALUES (?, 'person', '1')";
		try (PreparedStatement statement = connection.prepareStatement(insert)) {
			statement.setLong(1, filePk);
			statement.executeUpdate();
		}
		try (PreparedStatement statement = connection.prepareStatement(
				"UPDATE file_attachment SET use_yn = 'N' WHERE file_pk = ? AND target_table_name = 'person'")) {
			statement.setLong(1, filePk);
			statement.executeUpdate();
		}
		try (PreparedStatement statement = connection.prepareStatement(insert)) {
			statement.setLong(1, filePk);
			statement.executeUpdate();
		}
		expectUniqueFile(connection, insert, filePk);
	}

	private static void expectUniqueFile(Connection connection, String sql, long filePk) throws Exception {
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setLong(1, filePk);
			statement.executeUpdate();
			throw new AssertionError("사용 중인 중복이 거절되지 않았습니다.");
		}
		catch (SQLException ex) {
			assertEquals("23505", ex.getSQLState());
		}
	}

	private static void reuseExternalMap(Connection connection) throws Exception {
		long codePk;
		try (Statement statement = connection.createStatement();
				ResultSet resultSet = statement.executeQuery("SELECT code_pk FROM common_code WHERE use_yn = 'Y' LIMIT 1")) {
			assertTrue(resultSet.next());
			codePk = resultSet.getLong(1);
		}
		String insert = "INSERT INTO common_code_external_map (code_pk, external_system_code, external_code) VALUES (?, 'ITSYS', 'X')";
		try (PreparedStatement statement = connection.prepareStatement(insert)) {
			statement.setLong(1, codePk);
			statement.executeUpdate();
		}
		try (PreparedStatement statement = connection.prepareStatement(
				"UPDATE common_code_external_map SET use_yn = 'N' WHERE code_pk = ? AND external_system_code = 'ITSYS'")) {
			statement.setLong(1, codePk);
			statement.executeUpdate();
		}
		try (PreparedStatement statement = connection.prepareStatement(insert)) {
			statement.setLong(1, codePk);
			statement.executeUpdate();
		}
		expectUniqueFile(connection, insert, codePk);
	}

	private static void reuseEmployeeNo(Connection connection) throws Exception {
		long companyPk = scalar(connection, "SELECT company_pk FROM company LIMIT 1");
		long personPk = scalar(connection, "SELECT person_pk FROM person LIMIT 1");
		String insert = """
				INSERT INTO person_assignment (person_pk, company_pk, employee_no, employment_type_code, assignment_status_code, start_date)
				VALUES (?, ?, 'E-IT', 'REGULAR', 'ACTIVE', CURRENT_DATE)
				""";
		try (PreparedStatement statement = connection.prepareStatement(insert)) {
			statement.setLong(1, personPk);
			statement.setLong(2, companyPk);
			statement.executeUpdate();
		}
		try (PreparedStatement statement = connection.prepareStatement(
				"UPDATE person_assignment SET use_yn = 'N' WHERE company_pk = ? AND employee_no = 'E-IT'")) {
			statement.setLong(1, companyPk);
			statement.executeUpdate();
		}
		try (PreparedStatement statement = connection.prepareStatement(insert)) {
			statement.setLong(1, personPk);
			statement.setLong(2, companyPk);
			statement.executeUpdate();
		}
		try (PreparedStatement statement = connection.prepareStatement(insert)) {
			statement.setLong(1, personPk);
			statement.setLong(2, companyPk);
			statement.executeUpdate();
			throw new AssertionError("사용 중인 중복이 거절되지 않았습니다.");
		}
		catch (SQLException ex) {
			assertEquals("23505", ex.getSQLState());
		}
	}

	private static void reusePersonAccount(Connection connection) throws Exception {
		long personPk = scalar(connection, "SELECT person_pk FROM user_account WHERE person_pk IS NOT NULL LIMIT 1");
		long userPk = scalar(connection, "SELECT user_pk FROM user_account WHERE person_pk = " + personPk + " LIMIT 1");
		String insert = """
				INSERT INTO user_account (person_pk, account_type_code, login_id, user_status_code)
				VALUES (?, 'NORMAL', ?, 'ACTIVE')
				""";
		expectAccountUnique(connection, insert, personPk, "second-it@example.com");
		try (PreparedStatement statement = connection.prepareStatement("UPDATE user_account SET use_yn = 'N' WHERE user_pk = ?")) {
			statement.setLong(1, userPk);
			statement.executeUpdate();
		}
		try (PreparedStatement statement = connection.prepareStatement(insert)) {
			statement.setLong(1, personPk);
			statement.setString(2, "second-it@example.com");
			statement.executeUpdate();
		}
		expectAccountUnique(connection, insert, personPk, "third-it@example.com");
	}

	private static void expectAccountUnique(Connection connection, String sql, long personPk, String loginId) throws Exception {
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setLong(1, personPk);
			statement.setString(2, loginId);
			statement.executeUpdate();
			throw new AssertionError("사용 중인 중복이 거절되지 않았습니다.");
		}
		catch (SQLException ex) {
			assertEquals("23505", ex.getSQLState());
		}
	}

	private static long scalar(Connection connection, String sql) throws Exception {
		try (Statement statement = connection.createStatement(); ResultSet resultSet = statement.executeQuery(sql)) {
			assertTrue(resultSet.next());
			return resultSet.getLong(1);
		}
	}

	private static tools.jackson.databind.JsonNode json(MvcResult result) throws Exception {
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
