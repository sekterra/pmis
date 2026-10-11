package com.tommy.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Stream;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import org.springframework.boot.json.JsonParserFactory;

/**
 * 공용 스키마 마이그레이션을 빈 스키마 it_&lt;랜덤&gt; 에만 적용하고 ERD 테이블/컬럼명과 비교한다.
 * 스키마 pmis 와 그 외 스키마는 변경하지 않는다. 환경변수가 없으면 skip 한다.
 */
@Tag("integration")
class PublicTenantMigrationTest {

	private static final String SCHEMA_PATTERN = "^it_[a-z0-9]{16}$";

	@Test
	void migratesEmptySchemaAndMatchesErd() throws Exception {
		Assumptions.assumeTrue(TestcontainersConfiguration.isConfigured());

		String schema = "it_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
		assertTrue(schema.matches(SCHEMA_PATTERN));
		boolean created = false;
		try (Connection connection = openConnection()) {
			IntegrationDatabase.requirePmis(connection);
			createSchema(connection, schema);
			created = true;
			migrate(schema);
			assertMatchesErd(connection, schema);
			assertFamilyIndex(connection, schema);
			assertDomainNamePartialUnique(connection, schema);
			assertTenantConstraints(connection, schema);
		}
		finally {
			if (created) {
				dropSchema(schema);
			}
		}
	}

	private static Connection openConnection() throws Exception {
		return DriverManager.getConnection(
				System.getenv("MDM_TEST_DB_URL"),
				System.getenv("MDM_TEST_DB_USERNAME"),
				System.getenv("MDM_TEST_DB_PASSWORD"));
	}

	private static void createSchema(Connection connection, String schema) throws Exception {
		requireTestSchema(schema);
		try (Statement statement = connection.createStatement()) {
			statement.execute("CREATE SCHEMA " + schema);
		}
	}

	private static void migrate(String schema) {
		requireTestSchema(schema);
		Flyway.configure()
				.dataSource(
						System.getenv("MDM_TEST_DB_URL"),
						System.getenv("MDM_TEST_DB_USERNAME"),
						System.getenv("MDM_TEST_DB_PASSWORD"))
				.schemas(schema)
				.defaultSchema(schema)
				.createSchemas(false)
				.cleanDisabled(true)
				.locations("classpath:db/migration/public")
				.load()
				.migrate();
	}

	private static void assertMatchesErd(Connection connection, String schema) throws Exception {
		Path erd = latestTenantErd();
		Map<String, Object> root = JsonParserFactory.getJsonParser().parseMap(Files.readString(erd));
		List<Map<String, Object>> entities = castList(root.get("entities"));
		Set<String> expectedTables = new TreeSet<>();
		for (Map<String, Object> entity : entities) {
			expectedTables.add(String.valueOf(entity.get("physicalName")));
		}

		Set<String> actualTables = queryStrings(connection, """
				SELECT table_name
				FROM information_schema.tables
				WHERE table_schema = ? AND table_type = 'BASE TABLE' AND table_name <> 'flyway_schema_history'
				""", schema);
		assertEquals(expectedTables, new TreeSet<>(actualTables));

		for (Map<String, Object> entity : entities) {
			String table = String.valueOf(entity.get("physicalName"));
			Set<String> expectedColumns = new TreeSet<>();
			for (Map<String, Object> attribute : castList(entity.get("attributes"))) {
				expectedColumns.add(String.valueOf(attribute.get("physicalName")));
			}
			Set<String> actualColumns = queryStrings(connection, """
					SELECT column_name
					FROM information_schema.columns
					WHERE table_schema = ? AND table_name = ?
					""", schema, table);
			assertEquals(expectedColumns, new TreeSet<>(actualColumns), table);
		}
	}

	private static void assertFamilyIndex(Connection connection, String schema) throws Exception {
		Set<String> indexes = queryStrings(connection, """
				SELECT indexname FROM pg_indexes
				WHERE schemaname = ? AND indexname = 'ix_platform_admin_refresh_token_token_family_id'
				""", schema);
		assertEquals(Set.of("ix_platform_admin_refresh_token_token_family_id"), indexes);
	}

	private static void assertDomainNamePartialUnique(Connection connection, String schema) throws Exception {
		Set<String> definitions = queryStrings(connection, """
				SELECT indexdef FROM pg_indexes
				WHERE schemaname = ? AND indexname = 'uk_tenant_domain_domain_name'
				""", schema);
		assertEquals(1, definitions.size());
		assertTrue(definitions.iterator().next().toLowerCase().contains("use_yn"));
	}

	@SuppressWarnings("unchecked")
	private static List<Map<String, Object>> castList(Object value) {
		return (List<Map<String, Object>>) value;
	}

	private static Set<String> queryStrings(Connection connection, String sql, String... parameters) throws Exception {
		Set<String> values = new HashSet<>();
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			for (int i = 0; i < parameters.length; i++) {
				statement.setString(i + 1, parameters[i]);
			}
			try (ResultSet resultSet = statement.executeQuery()) {
				while (resultSet.next()) {
					values.add(resultSet.getString(1));
				}
			}
		}
		return values;
	}

	private static Path latestTenantErd() throws Exception {
		Path directory = Path.of("doc", "erd");
		try (Stream<Path> files = Files.list(directory)) {
			return files
					.filter(path -> {
						String name = path.getFileName().toString();
						return name.startsWith("erd-model-tenant-") && name.endsWith(".json");
					})
					.max(Comparator.comparing(path -> path.getFileName().toString()))
					.orElseThrow(() -> new IllegalStateException("doc/erd/erd-model-tenant-*.json 이 없습니다."));
		}
	}

	private static void dropSchema(String schema) throws Exception {
		requireTestSchema(schema);
		try (Connection connection = openConnection()) {
			IntegrationDatabase.requirePmis(connection);
			try (Statement statement = connection.createStatement()) {
				statement.execute("DROP SCHEMA " + schema + " CASCADE");
			}
		}
	}

	private static void assertTenantConstraints(Connection connection, String schema) throws Exception {
		useSchema(connection, schema);
		long tenantPk = insertReturningLong(connection, """
				INSERT INTO tenant (tenant_code, tenant_name, schema_name, tenant_status_code, default_language_code)
				VALUES ('acme-corp', 'Acme', 't_acme_corp', 'ACTIVE', 'ko-KR')
				RETURNING tenant_pk
				""");
		assertRejected(connection, """
				INSERT INTO tenant (tenant_code, tenant_name, schema_name, tenant_status_code, default_language_code)
				VALUES ('-acme', 'Acme', 't_bad_1', 'ACTIVE', 'ko-KR')
				""");
		assertRejected(connection, """
				INSERT INTO tenant (tenant_code, tenant_name, schema_name, tenant_status_code, default_language_code)
				VALUES ('acme-', 'Acme', 't_bad_2', 'ACTIVE', 'ko-KR')
				""");
		assertRejected(connection, """
				INSERT INTO tenant (tenant_code, tenant_name, schema_name, tenant_status_code, default_language_code)
				VALUES ('acme--corp', 'Acme', 't_bad_3', 'ACTIVE', 'ko-KR')
				""");
		assertRejected(connection, """
				INSERT INTO tenant (tenant_code, tenant_name, schema_name, tenant_status_code, default_language_code)
				VALUES ('beta', 'Beta', 'beta', 'ACTIVE', 'ko-KR')
				""");
		execute(connection, """
				INSERT INTO tenant_status_history (tenant_pk, from_status_code, to_status_code, change_datetime)
				VALUES (?, NULL, 'ACTIVE', CURRENT_TIMESTAMP)
				""", tenantPk);
		assertRejected(connection, """
				INSERT INTO tenant_status_history (tenant_pk, from_status_code, to_status_code, change_datetime)
				VALUES (%d, 'UNKNOWN', 'ACTIVE', CURRENT_TIMESTAMP)
				""".formatted(tenantPk));
		execute(connection, """
				INSERT INTO tenant_domain (tenant_pk, domain_name, domain_type_code, primary_yn, use_yn)
				VALUES (?, 'old.example', 'SUBDOMAIN', 'Y', 'N')
				""", tenantPk);
		execute(connection, """
				INSERT INTO tenant_domain (tenant_pk, domain_name, domain_type_code, primary_yn, use_yn)
				VALUES (?, 'primary.example', 'CUSTOM', 'Y', 'Y')
				""", tenantPk);
		assertRejected(connection, """
				INSERT INTO tenant_domain (tenant_pk, domain_name, domain_type_code, primary_yn, use_yn)
				VALUES (%d, 'other.example', 'SUBDOMAIN', 'Y', 'Y')
				""".formatted(tenantPk));
		execute(connection, """
				INSERT INTO tenant_domain (tenant_pk, domain_name, domain_type_code, primary_yn, use_yn)
				VALUES (?, 'old.example', 'SUBDOMAIN', 'N', 'Y')
				""", tenantPk);
		assertRejected(connection, """
				INSERT INTO tenant_domain (tenant_pk, domain_name, domain_type_code, primary_yn, use_yn)
				VALUES (%d, 'old.example', 'CUSTOM', 'N', 'Y')
				""".formatted(tenantPk));
		long otherPk = insertReturningLong(connection, """
				INSERT INTO tenant (tenant_code, tenant_name, schema_name, tenant_status_code, default_language_code)
				VALUES ('beta-corp', 'Beta', 't_beta_corp', 'ACTIVE', 'ko-KR')
				RETURNING tenant_pk
				""");
		execute(connection, """
				INSERT INTO tenant_domain (tenant_pk, domain_name, domain_type_code, primary_yn, use_yn)
				VALUES (?, 'moved.example', 'CUSTOM', 'N', 'N')
				""", tenantPk);
		execute(connection, """
				INSERT INTO tenant_domain (tenant_pk, domain_name, domain_type_code, primary_yn, use_yn)
				VALUES (?, 'moved.example', 'CUSTOM', 'Y', 'Y')
				""", otherPk);

		assertRejected(connection, """
				INSERT INTO platform_admin (login_id, admin_name, email, password_hash, admin_status_code)
				VALUES ('Admin', '운영자', 'admin@example.com', 'hash', 'ACTIVE')
				""");
		long adminPk = insertReturningLong(connection, """
				INSERT INTO platform_admin (login_id, admin_name, email, password_hash, admin_status_code)
				VALUES ('admin', '운영자', 'admin@example.com', 'hash', 'ACTIVE')
				RETURNING platform_admin_pk
				""");
		assertRejected(connection, """
				INSERT INTO platform_admin (login_id, admin_name, email, password_hash, admin_status_code)
				VALUES ('admin', '다른운영자', 'other@example.com', 'hash', 'ACTIVE')
				""");
		execute(connection, """
				INSERT INTO platform_admin_login_history (login_id_input, login_result_code, login_datetime)
				VALUES ('missing', 'FAIL_NOT_FOUND', CURRENT_TIMESTAMP)
				""");
		execute(connection, """
				INSERT INTO platform_admin_refresh_token (platform_admin_pk, token_hash, token_family_id, issued_datetime, expire_datetime)
				VALUES (?, 'hash-1', 'family-1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", adminPk);
		assertRejected(connection, """
				INSERT INTO platform_admin_refresh_token (platform_admin_pk, token_hash, token_family_id, issued_datetime, expire_datetime)
				VALUES (%d, 'hash-1', 'family-2', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""".formatted(adminPk));
	}

	private static void useSchema(Connection connection, String schema) throws Exception {
		requireTestSchema(schema);
		execute(connection, "SET search_path TO " + schema);
	}

	private static void execute(Connection connection, String sql, long... parameters) throws Exception {
		if (parameters.length == 0) {
			try (Statement statement = connection.createStatement()) {
				statement.execute(sql);
			}
			return;
		}
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			for (int i = 0; i < parameters.length; i++) {
				statement.setLong(i + 1, parameters[i]);
			}
			statement.execute();
		}
	}

	private static long insertReturningLong(Connection connection, String sql) throws Exception {
		try (Statement statement = connection.createStatement();
				ResultSet resultSet = statement.executeQuery(sql)) {
			if (!resultSet.next()) {
				throw new IllegalStateException("INSERT RETURNING 결과가 없습니다.");
			}
			return resultSet.getLong(1);
		}
	}

	private static void assertRejected(Connection connection, String sql) {
		org.junit.jupiter.api.Assertions.assertThrows(java.sql.SQLException.class, () -> {
			if (connection.getAutoCommit()) {
				execute(connection, sql);
				return;
			}
			java.sql.Savepoint savepoint = connection.setSavepoint();
			try {
				execute(connection, sql);
			}
			catch (java.sql.SQLException ex) {
				connection.rollback(savepoint);
				throw ex;
			}
		});
	}

	private static void requireTestSchema(String schema) {
		if (schema == null || !schema.matches(SCHEMA_PATTERN) || "pmis".equals(schema) || "public".equals(schema)
				|| schema.startsWith("pg_")) {
			throw new IllegalArgumentException("테스트 스키마명이 아닙니다.");
		}
	}

}
