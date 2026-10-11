package com.tommy.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.tommy.common.code.entity.CodeGroup;
import com.tommy.common.code.entity.CodeGroupLang;
import com.tommy.common.code.entity.CommonCode;
import com.tommy.common.code.entity.CommonCodeHistory;
import com.tommy.common.code.entity.CommonCodeLang;
import com.tommy.common.global.multitenancy.HeaderTenantResolver;
import com.tommy.common.global.security.AccountStatusCache;
import com.tommy.common.global.security.DbPermissionChecker;
import com.tommy.common.global.security.JwtTokenService;
import com.tommy.common.tenant.provision.ProvisionTenantCommand;
import com.tommy.common.tenant.provision.TenantProvisioningService;

@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class SecurityApiIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private TenantProvisioningService provisioningService;

	@Autowired
	private DataSource dataSource;

	@Autowired
	private JwtTokenService jwtTokenService;

	@Autowired
	private DbPermissionChecker permissionChecker;

	@Autowired
	private AccountStatusCache accountStatusCache;

	private String codeA;
	private String schemaA;
	private String codeB;
	private String schemaB;

	@AfterEach
	void cleanup() throws Exception {
		try (Connection connection = dataSource.getConnection()) {
			IntegrationDatabase.requirePmis(connection);
			deleteTenant(connection, codeA);
			deleteTenant(connection, codeB);
			dropSchema(connection, schemaA);
			dropSchema(connection, schemaB);
		}
	}

	@Test
	void enforcesJwtPermissionAccountAndBusinessNumberRules() throws Exception {
		codeA = provision();
		schemaA = "t_" + codeA.replace('-', '_');
		codeB = provision();
		schemaB = "t_" + codeB.replace('-', '_');
		validateSchema(schemaA);
		String adminA = TestAccess.issueAdmin(jwtTokenService, dataSource, schemaA, codeA);
		String adminB = TestAccess.issueAdmin(jwtTokenService, dataSource, schemaB, codeB);
		mockMvc.perform(get("/api/mdm/v1/code-groups").header(HeaderTenantResolver.HEADER, codeA).header("Authorization", "Bearer " + adminA))
				.andExpect(status().isOk());
		long limited = insertLimitedUser(schemaA);
		String limitedToken = token(limited, codeA);
		mockMvc.perform(get("/api/mdm/v1/code-groups").header(HeaderTenantResolver.HEADER, codeA).header("Authorization", "Bearer " + limitedToken))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.errorCode").value("AUTH.FORBIDDEN"));
		grant(schemaA, limited, "READ");
		execute(schemaA, "UPDATE user_role SET valid_from_date = CURRENT_DATE + 1 WHERE user_pk = " + limited);
		permissionChecker.evict(schemaA, limited);
		mockMvc.perform(get("/api/mdm/v1/code-groups").header(HeaderTenantResolver.HEADER, codeA).header("Authorization", "Bearer " + limitedToken))
				.andExpect(status().isForbidden());
		execute(schemaA, "UPDATE user_role SET valid_from_date = NULL WHERE user_pk = " + limited);
		permissionChecker.evict(schemaA, limited);
		mockMvc.perform(get("/api/mdm/v1/code-groups").header(HeaderTenantResolver.HEADER, codeA).header("Authorization", "Bearer " + limitedToken))
				.andExpect(status().isOk());
		execute(schemaA, "UPDATE role SET use_yn = 'N' WHERE role_code = 'IT_LIMITED'");
		permissionChecker.evict(schemaA, limited);
		mockMvc.perform(get("/api/mdm/v1/code-groups").header(HeaderTenantResolver.HEADER, codeA).header("Authorization", "Bearer " + limitedToken))
				.andExpect(status().isForbidden());
		execute(schemaA, "UPDATE role SET use_yn = 'Y' WHERE role_code = 'IT_LIMITED'");
		execute(schemaA, """
				UPDATE permission SET use_yn = 'N'
				WHERE permission_pk IN (
				    SELECT p.permission_pk FROM permission p
				    JOIN menu m ON m.menu_pk = p.menu_pk
				    WHERE m.menu_code = 'COMMON_CODE' AND p.action_code = 'READ')
				""");
		permissionChecker.evict(schemaA, limited);
		mockMvc.perform(get("/api/mdm/v1/code-groups").header(HeaderTenantResolver.HEADER, codeA).header("Authorization", "Bearer " + limitedToken))
				.andExpect(status().isForbidden());
		execute(schemaA, """
				UPDATE permission SET use_yn = 'Y'
				WHERE permission_pk IN (
				    SELECT p.permission_pk FROM permission p
				    JOIN menu m ON m.menu_pk = p.menu_pk
				    WHERE m.menu_code = 'COMMON_CODE' AND p.action_code = 'READ')
				""");
		execute(schemaA, "UPDATE menu SET use_yn = 'N' WHERE menu_code = 'COMMON_CODE'");
		permissionChecker.evict(schemaA, limited);
		mockMvc.perform(get("/api/mdm/v1/code-groups").header(HeaderTenantResolver.HEADER, codeA).header("Authorization", "Bearer " + limitedToken))
				.andExpect(status().isForbidden());
		execute(schemaA, "UPDATE menu SET use_yn = 'Y' WHERE menu_code = 'COMMON_CODE'");
		permissionChecker.evict(schemaA, limited);
		execute(schemaA, "UPDATE user_account SET user_status_code = 'LOCKED' WHERE user_pk = " + limited);
		accountStatusCache.evict(schemaA, limited);
		mockMvc.perform(get("/api/mdm/v1/code-groups").header(HeaderTenantResolver.HEADER, codeA).header("Authorization", "Bearer " + limitedToken))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.errorCode").value("AUTH.ACCOUNT_DISABLED"));
		execute(schemaA, "UPDATE user_account SET user_status_code = 'ACTIVE', use_yn = 'N' WHERE user_pk = " + limited);
		accountStatusCache.evict(schemaA, limited);
		mockMvc.perform(get("/api/mdm/v1/code-groups").header(HeaderTenantResolver.HEADER, codeA).header("Authorization", "Bearer " + limitedToken))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.errorCode").value("AUTH.ACCOUNT_DISABLED"));
		mockMvc.perform(get("/api/mdm/v1/code-groups").header(HeaderTenantResolver.HEADER, codeB).header("Authorization", "Bearer " + adminA))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errorCode").value("TENANT.HOST_MISMATCH"));
		long pkA = adminPk(schemaA);
		long pkB = adminPk(schemaB);
		assertEquals(pkA, pkB);
		execute(schemaB, "UPDATE user_role SET use_yn = 'N'");
		permissionChecker.evict(schemaB, pkB);
		mockMvc.perform(get("/api/mdm/v1/code-groups").header(HeaderTenantResolver.HEADER, codeB).header("Authorization", "Bearer " + adminB))
				.andExpect(status().isForbidden());
		mockMvc.perform(get("/api/mdm/v1/code-groups").header(HeaderTenantResolver.HEADER, codeA).header("Authorization", "Bearer " + adminA))
				.andExpect(status().isOk());
		reuseBusinessNumber(schemaA);
		mockMvc.perform(get("/api/mdm/v1/auth/jwks"))
				.andExpect(status().isOk())
				.andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("max-age=300")))
				.andExpect(jsonPath("$.keys[0].n").exists())
				.andExpect(jsonPath("$.keys[0].d").doesNotExist())
				.andExpect(jsonPath("$.keys[0].p").doesNotExist())
				.andExpect(jsonPath("$.keys[0].q").doesNotExist());
		mockMvc.perform(get("/api/mdm/v1/i18n/languages").header(HeaderTenantResolver.HEADER, codeA))
				.andExpect(status().isOk());
		mockMvc.perform(get("/api/mdm/v1/i18n/bundles").header(HeaderTenantResolver.HEADER, codeA).param("namespaces", "common,error"))
				.andExpect(status().isOk());
		mockMvc.perform(get("/api/mdm/v1/i18n/bundles/menu").header(HeaderTenantResolver.HEADER, codeA))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.errorCode").value("AUTH.UNAUTHORIZED"));
		mockMvc.perform(get("/api/mdm/v1/codes/MENU_TYPE").header(HeaderTenantResolver.HEADER, codeA))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(get("/api/mdm/v1/nations").header(HeaderTenantResolver.HEADER, codeA))
				.andExpect(status().isUnauthorized());
		String platform = jwtTokenService.issue("1", JwtTokenService.PLATFORM_AUDIENCE, JwtTokenService.PLATFORM_TYPE, null, false,
				Duration.ofMinutes(5));
		mockMvc.perform(get("/api/mdm/v1/code-groups").header(HeaderTenantResolver.HEADER, codeA).header("Authorization", "Bearer " + platform))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.errorCode").value("AUTH.TOKEN_REJECTED"));
		mockMvc.perform(get("/api/platform/v1/auth/me").header("Authorization", "Bearer " + adminA))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.errorCode").value("AUTH.TOKEN_REJECTED"));
	}

	private String provision() {
		String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
		String tenantCode = "it-" + suffix;
		provisioningService.provision(new ProvisionTenantCommand(
				tenantCode, "아이티", null, "ko-KR", "관리자", "admin@example.com", null));
		return tenantCode;
	}

	private String token(long userPk, String tenantCode) {
		return jwtTokenService.issue(Long.toString(userPk), JwtTokenService.TENANT_AUDIENCE, JwtTokenService.TENANT_TYPE, tenantCode,
				false, Duration.ofMinutes(5));
	}

	private long insertLimitedUser(String schema) throws Exception {
		try (Connection connection = dataSource.getConnection()) {
			IntegrationDatabase.requirePmis(connection);
			try (Statement statement = connection.createStatement()) {
				statement.execute("SET search_path TO " + schema);
				statement.execute("""
						INSERT INTO role (role_code, role_name)
						VALUES ('IT_LIMITED', '제한')
						""");
				long permissionPk;
				try (ResultSet permission = statement.executeQuery("""
						SELECT p.permission_pk FROM permission p
						JOIN menu m ON m.menu_pk = p.menu_pk
						WHERE m.menu_code = 'COMMON_CODE' AND p.action_code = 'UPDATE'
						""")) {
					permission.next();
					permissionPk = permission.getLong(1);
				}
				statement.execute("""
						INSERT INTO role_permission (role_pk, permission_pk)
						SELECT role_pk, %d FROM role WHERE role_code = 'IT_LIMITED'
						""".formatted(permissionPk));
				long userPk;
				try (ResultSet user = statement.executeQuery("""
						INSERT INTO user_account (account_type_code, login_id, user_status_code)
						VALUES ('NORMAL', 'limited@example.com', 'ACTIVE')
						RETURNING user_pk
						""")) {
					user.next();
					userPk = user.getLong(1);
				}
				statement.execute("INSERT INTO user_role (user_pk, role_pk) SELECT " + userPk
						+ ", role_pk FROM role WHERE role_code = 'IT_LIMITED'");
				return userPk;
			}
		}
	}

	private void grant(String schema, long userPk, String action) throws Exception {
		execute(schema, """
				INSERT INTO role_permission (role_pk, permission_pk)
				SELECT ur.role_pk, p.permission_pk
				FROM user_role ur
				JOIN permission p ON p.action_code = '%s'
				JOIN menu m ON m.menu_pk = p.menu_pk AND m.menu_code = 'COMMON_CODE'
				WHERE ur.user_pk = %d
				""".formatted(action, userPk));
	}

	private void reuseBusinessNumber(String schema) throws Exception {
		execute(schema, """
				INSERT INTO company (company_code, company_name, company_type_code, business_registration_no)
				VALUES ('IT_OLD', '이전', 'HQ', '1108112345')
				""");
		execute(schema, "UPDATE company SET use_yn = 'N' WHERE company_code = 'IT_OLD'");
		execute(schema, """
				INSERT INTO company (company_code, company_name, company_type_code, business_registration_no)
				VALUES ('IT_NEW', '신규', 'HQ', '1108112345')
				""");
		SQLException duplicate = org.junit.jupiter.api.Assertions.assertThrows(SQLException.class, () -> {
			try {
				execute(schema, """
						INSERT INTO company (company_code, company_name, company_type_code, business_registration_no)
						VALUES ('IT_DUP', '중복', 'HQ', '1108112345')
						""");
			}
			catch (SQLException ex) {
				throw ex;
			}
			catch (Exception ex) {
				throw new IllegalStateException(ex);
			}
		});
		assertEquals("23505", duplicate.getSQLState());
	}

	private long adminPk(String schema) throws Exception {
		try (Connection connection = dataSource.getConnection()) {
			IntegrationDatabase.requirePmis(connection);
			try (Statement statement = connection.createStatement()) {
				statement.execute("SET search_path TO " + schema);
				try (ResultSet resultSet = statement.executeQuery(
						"SELECT user_pk FROM user_account WHERE login_id = 'admin@example.com'")) {
					resultSet.next();
					return resultSet.getLong(1);
				}
			}
		}
	}

	private void execute(String schema, String sql) throws Exception {
		if (!schema.matches("^t_it_[a-z0-9]{16}$")) {
			throw new IllegalArgumentException("테스트 스키마가 아닙니다.");
		}
		try (Connection connection = dataSource.getConnection()) {
			IntegrationDatabase.requirePmis(connection);
			try (Statement statement = connection.createStatement()) {
				statement.execute("SET search_path TO " + schema);
				statement.execute(sql);
			}
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

	private void deleteTenant(Connection connection, String tenantCode) throws SQLException {
		if (tenantCode == null) {
			return;
		}
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
		for (String table : java.util.List.of("tenant_schema_migration", "tenant_status_history", "tenant_provisioning", "tenant_domain")) {
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

	private void dropSchema(Connection connection, String schema) throws SQLException {
		if (schema != null && schema.matches("^t_it_[a-z0-9]{16}$")) {
			try (Statement statement = connection.createStatement()) {
				statement.execute("DROP SCHEMA IF EXISTS " + schema + " CASCADE");
			}
		}
	}

}
