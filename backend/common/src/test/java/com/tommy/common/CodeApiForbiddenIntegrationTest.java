package com.tommy.common;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.UUID;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;

import com.tommy.common.global.security.JwtTokenService;

import com.tommy.common.global.multitenancy.HeaderTenantResolver;
import com.tommy.common.tenant.provision.ProvisionTenantCommand;
import com.tommy.common.tenant.provision.TenantProvisioningService;

@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
@Import(CodeApiForbiddenIntegrationTest.HeaderConfig.class)
class CodeApiForbiddenIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private TenantProvisioningService provisioningService;

	@Autowired
	private DataSource dataSource;

	@Autowired
	private JwtTokenService jwtTokenService;

	@DynamicPropertySource
	static void datasource(DynamicPropertyRegistry registry) {
		TestcontainersConfiguration.registerDatasource(registry);
		TestJwtKey.register(registry);
	}

	private String tenantCode;
	private String schema;

	@AfterEach
	void cleanup() throws Exception {
		if (tenantCode == null) {
			return;
		}
		try (Connection connection = dataSource.getConnection()) {
			IntegrationDatabase.requirePmis(connection);
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
			if (tenantPk != null) {
				for (String table : java.util.List.of(
						"tenant_schema_migration", "tenant_status_history", "tenant_provisioning", "tenant_domain")) {
					try (PreparedStatement statement = connection.prepareStatement(
							"DELETE FROM pmis." + table + " WHERE tenant_pk = ?")) {
						statement.setLong(1, tenantPk);
						statement.executeUpdate();
					}
				}
				try (PreparedStatement statement = connection.prepareStatement("DELETE FROM pmis.tenant WHERE tenant_pk = ?")) {
					statement.setLong(1, tenantPk);
					statement.executeUpdate();
				}
			}
			if (schema != null && schema.matches("^t_it_[a-z0-9]{16}$")) {
				try (Statement statement = connection.createStatement()) {
					statement.execute("DROP SCHEMA IF EXISTS " + schema + " CASCADE");
				}
			}
		}
	}

	@Test
	void managementApiIsForbiddenOutsideDev() throws Exception {
		String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
		tenantCode = "it-" + suffix;
		schema = "t_it_" + suffix;
		provisioningService.provision(new ProvisionTenantCommand(
				tenantCode, "아이티", null, "ko-KR", "관리자", "admin@example.com", null));
		mockMvc.perform(get("/api/mdm/v1/code-groups").header(HeaderTenantResolver.HEADER, tenantCode))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.errorCode").value("AUTH.UNAUTHORIZED"));
		mockMvc.perform(get("/api/mdm/v1/codes/MENU_TYPE").header(HeaderTenantResolver.HEADER, tenantCode))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.errorCode").value("AUTH.UNAUTHORIZED"));
		mockMvc.perform(get("/api/mdm/v1/support-languages").header(HeaderTenantResolver.HEADER, tenantCode))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.errorCode").value("AUTH.UNAUTHORIZED"));
		mockMvc.perform(get("/api/mdm/v1/i18n/languages").header(HeaderTenantResolver.HEADER, tenantCode))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data").isArray());
		long userPk = insertUserWithoutRole();
		String token = jwtTokenService.issue(Long.toString(userPk), JwtTokenService.TENANT_AUDIENCE, JwtTokenService.TENANT_TYPE,
				tenantCode, false, Duration.ofMinutes(5));
		mockMvc.perform(get("/api/mdm/v1/code-groups").header(HeaderTenantResolver.HEADER, tenantCode)
				.header("Authorization", "Bearer " + token))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.errorCode").value("AUTH.FORBIDDEN"));
		mockMvc.perform(get("/api/mdm/v1/codes/MENU_TYPE").header(HeaderTenantResolver.HEADER, tenantCode)
				.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk());
	}

	private long insertUserWithoutRole() throws Exception {
		try (Connection connection = dataSource.getConnection()) {
			IntegrationDatabase.requirePmis(connection);
			try (Statement statement = connection.createStatement()) {
				statement.execute("SET search_path TO " + schema);
				try (ResultSet resultSet = statement.executeQuery("""
						INSERT INTO user_account (account_type_code, login_id, user_status_code)
						VALUES ('NORMAL', 'noperm@example.com', 'ACTIVE')
						RETURNING user_pk
						""")) {
					resultSet.next();
					return resultSet.getLong(1);
				}
			}
		}
	}

	@TestConfiguration
	static class HeaderConfig {
		@Bean
		HeaderTenantResolver headerTenantResolver() {
			return new HeaderTenantResolver();
		}
	}

}
