package com.tommy.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.sql.DataSource;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.AbstractDataSource;

import com.tommy.common.global.exception.ApiException;
import com.tommy.common.global.multitenancy.SchemaMultiTenantConnectionProvider;
import com.tommy.common.global.multitenancy.TenantAccess;
import com.tommy.common.tenant.JdbcTenantDirectory;

/**
 * 테스트가 만든 t_it_ 스키마와 pmis.tenant 행만 지운다.
 */
@Tag("integration")
class TenantSchemaIsolationTest {

	private static final String SCHEMA_PATTERN = "^t_it_[a-z0-9]{16}$";

	@Test
	void isolatesSchemas() throws Exception {
		org.junit.jupiter.api.Assumptions.assumeTrue(TestcontainersConfiguration.isConfigured());
		String suffixA = randomSuffix();
		String suffixB = randomSuffix();
		String schemaA = "t_it_" + suffixA;
		String schemaB = "t_it_" + suffixB;
		boolean createdA = false;
		boolean createdB = false;
		try (Connection connection = openConnection()) {
			IntegrationDatabase.requirePmis(connection);
			assertTrue(schemaA.matches(SCHEMA_PATTERN));
			assertTrue(schemaB.matches(SCHEMA_PATTERN));
			createSchema(connection, schemaA);
			createdA = true;
			createSchema(connection, schemaB);
			createdB = true;
			createMarker(connection, schemaA, "A");
			createMarker(connection, schemaB, "B");
			SchemaMultiTenantConnectionProvider provider = new SchemaMultiTenantConnectionProvider(dataSource());
			assertEquals("A", readMarker(provider, schemaA));
			assertEquals("B", readMarker(provider, schemaB));
			assertResetLeavesPreviousTenant(schemaA);
		}
		finally {
			cleanup(java.util.List.of(), createdA ? schemaA : null, createdB ? schemaB : null, null);
		}
	}

	@Test
	void rejectsMissingAndSuspendedTenants() throws Exception {
		org.junit.jupiter.api.Assumptions.assumeTrue(TestcontainersConfiguration.isConfigured());
		String suffixA = randomSuffix();
		String suffixB = randomSuffix();
		String suffixS = randomSuffix();
		String schemaA = "t_it_" + suffixA;
		String schemaB = "t_it_" + suffixB;
		String schemaS = "t_it_" + suffixS;
		String codeA = "ita" + suffixA;
		String codeB = "itb" + suffixB;
		String suspendedCode = "its" + suffixS;
		assertTrue(schemaA.matches(SCHEMA_PATTERN));
		assertTrue(schemaB.matches(SCHEMA_PATTERN));
		assertTrue(schemaS.matches(SCHEMA_PATTERN));
		List<Long> tenantPks = new ArrayList<>();
		boolean createdA = false;
		boolean createdB = false;
		boolean createdS = false;
		try (Connection connection = openConnection()) {
			IntegrationDatabase.requirePmis(connection);
			org.junit.jupiter.api.Assumptions.assumeTrue(publicTenantTableExists(connection),
					"pmis.tenant 가 없습니다. 공용 스키마는 마이그레이션하지 않았습니다.");
			createSchema(connection, schemaA);
			createdA = true;
			createSchema(connection, schemaB);
			createdB = true;
			createSchema(connection, schemaS);
			createdS = true;
			tenantPks.add(insertTenant(connection, codeA, schemaA, "ACTIVE"));
			tenantPks.add(insertTenant(connection, codeB, schemaB, "ACTIVE"));
			tenantPks.add(insertTenant(connection, suspendedCode, schemaS, "SUSPENDED"));
			insertDomain(connection, tenantPks.get(0), codeA + ".test");

			JdbcTenantDirectory directory = new JdbcTenantDirectory(new JdbcTemplate(dataSource()));
			assertEquals(schemaA, directory.findByTenantCode(codeA).orElseThrow().schemaName());
			assertEquals(codeA, directory.findByDomain(codeA + ".test").orElseThrow().tenantCode());
			assertTrue(directory.findByTenantCode("missing-" + suffixA).isEmpty());
			ApiException inactive = assertThrows(ApiException.class,
					() -> TenantAccess.requireActiveSchema(directory.findByTenantCode(suspendedCode).orElseThrow()));
			assertEquals("TENANT.INACTIVE", inactive.errorCode());
		}
		finally {
			cleanup(tenantPks, createdA ? schemaA : null, createdB ? schemaB : null, createdS ? schemaS : null);
		}
	}

	private static String readMarker(SchemaMultiTenantConnectionProvider provider, String schema) throws Exception {
		Connection connection = provider.getConnection(schema);
		try (Statement statement = connection.createStatement()) {
			String label;
			try (ResultSet resultSet = statement.executeQuery("SELECT label FROM marker")) {
				assertTrue(resultSet.next());
				label = resultSet.getString(1);
			}
			try (ResultSet path = statement.executeQuery("SHOW search_path")) {
				assertTrue(path.next());
				assertTrue(path.getString(1).contains(schema));
			}
			return label;
		}
		finally {
			provider.releaseConnection(schema, connection);
		}
	}

	private static void assertResetLeavesPreviousTenant(String schema) throws Exception {
		Connection raw = openConnection();
		Connection spy = org.mockito.Mockito.spy(raw);
		doAnswer(invocation -> null).when(spy).close();
		DataSource dataSource = mock(DataSource.class);
		when(dataSource.getConnection()).thenReturn(spy);
		SchemaMultiTenantConnectionProvider provider = new SchemaMultiTenantConnectionProvider(dataSource);
		try {
			provider.getConnection(schema);
			provider.releaseConnection(schema, spy);
			try (Statement statement = raw.createStatement();
					ResultSet resultSet = statement.executeQuery("SHOW search_path")) {
				assertTrue(resultSet.next());
				assertFalse(resultSet.getString(1).contains(schema));
			}
		}
		finally {
			raw.close();
		}
	}

	private static boolean publicTenantTableExists(Connection connection) throws Exception {
		try (Statement statement = connection.createStatement();
				ResultSet resultSet = statement.executeQuery("SELECT to_regclass('pmis.tenant')")) {
			return resultSet.next() && resultSet.getString(1) != null;
		}
	}

	private static long insertTenant(Connection connection, String tenantCode, String schemaName, String status)
			throws Exception {
		try (PreparedStatement statement = connection.prepareStatement("""
				INSERT INTO pmis.tenant (
				    tenant_code, tenant_name, schema_name, tenant_status_code, default_language_code)
				VALUES (?, 'isolation', ?, ?, 'ko')
				RETURNING tenant_pk
				""")) {
			statement.setString(1, tenantCode);
			statement.setString(2, schemaName);
			statement.setString(3, status);
			try (ResultSet resultSet = statement.executeQuery()) {
				assertTrue(resultSet.next());
				return resultSet.getLong(1);
			}
		}
	}

	private static void insertDomain(Connection connection, long tenantPk, String domainName) throws Exception {
		try (PreparedStatement statement = connection.prepareStatement("""
				INSERT INTO pmis.tenant_domain (tenant_pk, domain_name, domain_type_code, primary_yn, verified_yn)
				VALUES (?, ?, 'SUBDOMAIN', 'Y', 'N')
				""")) {
			statement.setLong(1, tenantPk);
			statement.setString(2, domainName);
			statement.executeUpdate();
		}
	}

	private static void createMarker(Connection connection, String schema, String label) throws Exception {
		requireTestSchema(schema);
		try (Statement statement = connection.createStatement()) {
			statement.execute("CREATE TABLE " + schema + ".marker (label VARCHAR(10) PRIMARY KEY)");
			statement.execute("INSERT INTO " + schema + ".marker (label) VALUES ('" + label + "')");
		}
	}

	private static void createSchema(Connection connection, String schema) throws Exception {
		requireTestSchema(schema);
		try (Statement statement = connection.createStatement()) {
			statement.execute("CREATE SCHEMA " + schema);
		}
	}

	private static void cleanup(List<Long> tenantPks, String schemaA, String schemaB, String schemaS) throws Exception {
		try (Connection connection = openConnection()) {
			IntegrationDatabase.requirePmis(connection);
			if (!tenantPks.isEmpty()) {
				try (PreparedStatement domain = connection.prepareStatement(
						"DELETE FROM pmis.tenant_domain WHERE tenant_pk = ?");
						PreparedStatement tenant = connection.prepareStatement(
								"DELETE FROM pmis.tenant WHERE tenant_pk = ?")) {
					for (Long tenantPk : tenantPks) {
						domain.setLong(1, tenantPk);
						domain.executeUpdate();
						tenant.setLong(1, tenantPk);
						tenant.executeUpdate();
					}
				}
			}
			dropSchema(connection, schemaA);
			dropSchema(connection, schemaB);
			dropSchema(connection, schemaS);
		}
	}

	private static void dropSchema(Connection connection, String schema) throws Exception {
		if (schema == null) {
			return;
		}
		requireTestSchema(schema);
		try (Statement statement = connection.createStatement()) {
			statement.execute("DROP SCHEMA " + schema + " CASCADE");
		}
	}

	private static void requireTestSchema(String schema) {
		if (!schema.matches(SCHEMA_PATTERN)) {
			throw new IllegalArgumentException("테스트 스키마명이 아닙니다.");
		}
	}

	private static DataSource dataSource() {
		return new AbstractDataSource() {
			@Override
			public Connection getConnection() throws java.sql.SQLException {
				return openConnection();
			}

			@Override
			public Connection getConnection(String username, String password) throws java.sql.SQLException {
				return openConnection();
			}
		};
	}

	private static Connection openConnection() throws java.sql.SQLException {
		return DriverManager.getConnection(
				System.getenv("MDM_TEST_DB_URL"),
				System.getenv("MDM_TEST_DB_USERNAME"),
				System.getenv("MDM_TEST_DB_PASSWORD"));
	}

	private static String randomSuffix() {
		return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
	}

}
