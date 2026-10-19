package com.tommy.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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

import com.tommy.common.global.multitenancy.HeaderTenantResolver;
import com.tommy.common.tenant.provision.ProvisionTenantCommand;
import com.tommy.common.tenant.provision.TenantProvisioningService;

@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class AccessContextIntegrationTest {

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
	private DataSource dataSource;

	private final List<String> tenantCodes = new ArrayList<>();

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
	void contextFollowsDevHeaderPlatformHostAndTenantDomain() throws Exception {
		String tenant = provision();
		String host = tenant.replace('-', '.') + ".example.test";
		execute("""
				INSERT INTO pmis.tenant_domain (tenant_pk, domain_name, domain_type_code, primary_yn, verified_yn, use_yn)
				SELECT tenant_pk, '%s', 'CUSTOM', 'Y', 'Y', 'Y' FROM pmis.tenant WHERE tenant_code = '%s'
				""".formatted(host, tenant));
		mockMvc.perform(get("/api/mdm/v1/context"))
				.andExpect(status().isOk())
				.andExpect(header().string("Cache-Control", "no-store"))
				.andExpect(jsonPath("$.data.kind").value("PLATFORM"))
				.andExpect(jsonPath("$.data.schemaName").doesNotExist());
		mockMvc.perform(get("/api/mdm/v1/context").header(HeaderTenantResolver.HEADER, tenant))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.kind").value("TENANT"))
				.andExpect(jsonPath("$.data.tenantCode").value(tenant))
				.andExpect(jsonPath("$.data.tenantName").value("아이티"))
				.andExpect(jsonPath("$.data.schemaName").doesNotExist());
		mockMvc.perform(get("/api/mdm/v1/context").header("Host", host))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.tenantCode").value(tenant));
		mockMvc.perform(get("/api/mdm/v1/context").header(HeaderTenantResolver.HEADER, "missing-tenant"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.errorCode").value("TENANT.NOT_FOUND"));
		mockMvc.perform(get("/api/mdm/v1/context").header("Host", "unknown.example.test"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errorCode").value("TENANT.UNRESOLVED"));
		mockMvc.perform(post("/api/platform/v1/auth/login").header("Host", "unknown.example.test")
				.contentType(MediaType.APPLICATION_JSON).content("{\"loginId\":\"nobody\",\"password\":\"x\"}"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.errorCode").value("TENANT.NOT_FOUND"));
		mockMvc.perform(post("/api/platform/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"loginId\":\"nobody\",\"password\":\"x\"}"))
				.andExpect(status().isUnauthorized());
	}

	private String provision() {
		String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
		String tenantCode = "it-" + suffix;
		String schema = "t_" + tenantCode.replace('-', '_');
		assertTrue(schema.matches(SCHEMA_PATTERN));
		tenantCodes.add(tenantCode);
		schemas.add(schema);
		provisioningService.provision(new ProvisionTenantCommand(tenantCode, "아이티", null, "ko-KR", "관리자", "admin@example.com", null));
		return tenantCode;
	}

	private void execute(String sql) throws Exception {
		try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
			IntegrationDatabase.requirePmis(connection);
			statement.execute(sql);
		}
	}

	private static long count(Connection connection, String sql) throws Exception {
		try (Statement statement = connection.createStatement(); ResultSet resultSet = statement.executeQuery(sql)) {
			resultSet.next();
			return resultSet.getLong(1);
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

	private static void dropSchema(Connection connection, String schema) throws Exception {
		if (!schema.matches(SCHEMA_PATTERN)) {
			throw new IllegalArgumentException("테스트 스키마명이 아닙니다.");
		}
		try (Statement statement = connection.createStatement()) {
			statement.execute("DROP SCHEMA IF EXISTS " + schema + " CASCADE");
		}
	}

}
