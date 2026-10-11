package com.tommy.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.tommy.common.tenant.TenantStatus;
import com.tommy.common.tenant.entity.Tenant;

/**
 * tenant_code, schema_name 은 엔티티에서 수정할 수 없고, 이름 변경 UPDATE 에도 포함되지 않는다.
 */
@Tag("integration")
class TenantCodeImmutabilityTest {

	private static final String SCHEMA_PATTERN = "^it_[a-z0-9]{16}$";

	@Test
	void renamingTenantDoesNotChangeCodeOrSchemaName() throws Exception {
		Assumptions.assumeTrue(TestcontainersConfiguration.isConfigured());
		String schema = "it_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
		assertTrue(schema.matches(SCHEMA_PATTERN));
		boolean created = false;
		try (Connection connection = openConnection()) {
			IntegrationDatabase.requirePmis(connection);
			try (Statement statement = connection.createStatement()) {
				statement.execute("CREATE SCHEMA " + schema);
			}
			created = true;
			org.flywaydb.core.Flyway.configure()
					.dataSource(System.getenv("MDM_TEST_DB_URL"), System.getenv("MDM_TEST_DB_USERNAME"),
							System.getenv("MDM_TEST_DB_PASSWORD"))
					.schemas(schema)
					.defaultSchema(schema)
					.createSchemas(false)
					.cleanDisabled(true)
					.locations("classpath:db/migration/public")
					.load()
					.migrate();

			AtomicReference<String> updateSql = new AtomicReference<>();
			Configuration configuration = new Configuration();
			configuration.addAnnotatedClass(Tenant.class);
			configuration.setProperty(AvailableSettings.JAKARTA_JDBC_URL, System.getenv("MDM_TEST_DB_URL"));
			configuration.setProperty(AvailableSettings.JAKARTA_JDBC_USER, System.getenv("MDM_TEST_DB_USERNAME"));
			configuration.setProperty(AvailableSettings.JAKARTA_JDBC_PASSWORD, System.getenv("MDM_TEST_DB_PASSWORD"));
			configuration.setProperty(AvailableSettings.DEFAULT_SCHEMA, schema);
			configuration.setProperty(AvailableSettings.HBM2DDL_AUTO, "none");
			configuration.setPhysicalNamingStrategy(new CamelCaseToUnderscoresNamingStrategy() {
				@Override
				public Identifier toPhysicalSchemaName(Identifier name, JdbcEnvironment jdbcEnvironment) {
					if (name != null && "pmis".equals(name.getText())) {
						return Identifier.toIdentifier(schema);
					}
					return super.toPhysicalSchemaName(name, jdbcEnvironment);
				}
			});
			configuration.setStatementInspector((StatementInspector) sql -> {
				if (sql.toLowerCase().startsWith("update")) {
					updateSql.set(sql);
				}
				return sql;
			});
			try (SessionFactory sessionFactory = configuration.buildSessionFactory();
					Session session = sessionFactory.openSession()) {
				session.beginTransaction();
				Tenant tenant = Tenant.create("acme", "Acme", "t_acme", TenantStatus.ACTIVE, "ko-KR");
				session.persist(tenant);
				session.flush();
				tenant.rename("Acme Renamed");
				setField(tenant, "tenantCode", "changed");
				setField(tenant, "schemaName", "t_changed");
				session.flush();
				session.getTransaction().commit();
			}
			assertTrue(updateSql.get() != null && updateSql.get().toLowerCase().contains("tenant_name"));
			assertFalse(updateSql.get().toLowerCase().contains("tenant_code"));
			assertFalse(updateSql.get().toLowerCase().contains("schema_name"));
			try (Statement statement = connection.createStatement()) {
				statement.execute("SET search_path TO " + schema);
				try (ResultSet resultSet = statement.executeQuery(
						"SELECT tenant_code, schema_name, tenant_name FROM tenant")) {
					assertTrue(resultSet.next());
					assertEquals("acme", resultSet.getString("tenant_code"));
					assertEquals("t_acme", resultSet.getString("schema_name"));
					assertEquals("Acme Renamed", resultSet.getString("tenant_name"));
				}
			}
		}
		finally {
			if (created) {
				dropSchema(schema);
			}
		}
	}

	private static void setField(Object target, String name, Object value) throws Exception {
		Field field = target.getClass().getDeclaredField(name);
		field.setAccessible(true);
		field.set(target, value);
	}

	private static Connection openConnection() throws Exception {
		return DriverManager.getConnection(
				System.getenv("MDM_TEST_DB_URL"),
				System.getenv("MDM_TEST_DB_USERNAME"),
				System.getenv("MDM_TEST_DB_PASSWORD"));
	}

	private static void dropSchema(String schema) throws Exception {
		if (!schema.matches(SCHEMA_PATTERN)) {
			throw new IllegalArgumentException("테스트 스키마명이 아닙니다.");
		}
		try (Connection connection = openConnection()) {
			IntegrationDatabase.requirePmis(connection);
			try (Statement statement = connection.createStatement()) {
				statement.execute("DROP SCHEMA " + schema + " CASCADE");
			}
		}
	}

}
