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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
 * 공용 V1과 테넌트 V1~V3를 빈 스키마 it_&lt;랜덤&gt; 하나에 순서대로 적용한다.
 * 버전 번호가 겹치므로 테넌트 Flyway 이력 테이블만 이름을 달리하고, 이미 테이블이 있는 스키마는 baseline 0 으로 이어서 적용한다. 스키마명은 SQL에 쓰지 않는다.
 */
@Tag("integration")
class AllMigrationTest {

	private static final String SCHEMA_PATTERN = "^it_[a-z0-9]{16}$";
	private static final String TENANT_HISTORY = "flyway_tenant_schema_history";

	@Test
	void appliesPublicAndTenantMigrations() throws Exception {
		Assumptions.assumeTrue(TestcontainersConfiguration.isConfigured());

		String schema = "it_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
		assertTrue(schema.matches(SCHEMA_PATTERN));
		boolean created = false;
		try (Connection connection = openConnection()) {
			IntegrationDatabase.requirePmis(connection);
			createSchema(connection, schema);
			created = true;
			migrate(schema, "classpath:db/migration/public", "flyway_schema_history", false);
			migrate(schema, "classpath:db/migration/tenant", TENANT_HISTORY, true);
			assertSchema(connection, schema);
		}
		finally {
			if (created) {
				dropSchema(schema);
			}
		}
	}

	private static void assertSchema(Connection connection, String schema) throws Exception {
		Set<String> publicTables = tableNames(latestErd("erd-model-tenant-"));
		Set<String> tenantTables = new TreeSet<>();
		tenantTables.addAll(tableNames(latestErd("erd-model-i18n-common-code-")));
		tenantTables.addAll(tableNames(latestErd("erd-model-org-user-")));
		tenantTables.addAll(tableNames(latestErd("erd-model-file-team-auth-menu-")));
		assertEquals(31, tenantTables.size());

		Set<String> actualTables = queryStrings(connection, """
				SELECT table_name
				FROM information_schema.tables
				WHERE table_schema = ? AND table_type = 'BASE TABLE'
				  AND table_name <> 'flyway_schema_history' AND table_name <> ?
				""", schema, TENANT_HISTORY);
		Set<String> expected = new TreeSet<>();
		expected.addAll(publicTables);
		expected.addAll(tenantTables);
		assertEquals(expected, new TreeSet<>(actualTables));

		Set<String> objects = queryStrings(connection, """
				SELECT c.conname
				FROM pg_constraint c
				JOIN pg_namespace n ON n.oid = c.connamespace
				WHERE n.nspname = ?
				UNION
				SELECT indexname
				FROM pg_indexes
				WHERE schemaname = ?
				""", schema, schema);
		assertTrue(objects.contains("fk_tenant_domain_tenant"));
		assertTrue(objects.contains("fk_company_nation"));
		assertTrue(objects.contains("fk_user_account_support_language"));
		assertTrue(objects.contains("fk_person_file"));
		assertTrue(objects.contains("fk_team_member_person"));
		assertTrue(objects.contains("fk_user_role_user"));
		assertTrue(objects.contains("ck_role_role_code"));
		assertTrue(objects.contains("uk_file_storage_default_yn"));
		assertTrue(objects.contains("ix_user_login_history_login_datetime"));
		assertTrue(objects.contains("ix_menu_parent_menu_pk"));
	}

	private static void migrate(String schema, String location, String historyTable, boolean baselineExisting) {
		requireTestSchema(schema);
		org.flywaydb.core.api.configuration.FluentConfiguration configuration = Flyway.configure()
				.dataSource(
						System.getenv("MDM_TEST_DB_URL"),
						System.getenv("MDM_TEST_DB_USERNAME"),
						System.getenv("MDM_TEST_DB_PASSWORD"))
				.schemas(schema)
				.defaultSchema(schema)
				.table(historyTable)
				.createSchemas(false)
				.cleanDisabled(true)
				.locations(location);
		if (baselineExisting) {
			configuration.baselineVersion("0").baselineOnMigrate(true);
		}
		configuration.load().migrate();
	}

	@SuppressWarnings("unchecked")
	private static List<Map<String, Object>> castList(Object value) {
		return (List<Map<String, Object>>) value;
	}

	private static Set<String> tableNames(Path erd) throws Exception {
		Map<String, Object> root = JsonParserFactory.getJsonParser().parseMap(Files.readString(erd));
		Set<String> names = new TreeSet<>();
		for (Map<String, Object> entity : castList(root.get("entities"))) {
			names.add(String.valueOf(entity.get("physicalName")));
		}
		return names;
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

	private static Path latestErd(String prefix) throws Exception {
		Path directory = Path.of("doc", "erd");
		try (Stream<Path> files = Files.list(directory)) {
			return files
					.filter(path -> {
						String name = path.getFileName().toString();
						return name.startsWith(prefix) && name.endsWith(".json");
					})
					.max(Comparator.comparing(path -> path.getFileName().toString()))
					.orElseThrow(() -> new IllegalStateException(prefix + "*.json 이 없습니다."));
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

	private static void dropSchema(String schema) throws Exception {
		requireTestSchema(schema);
		try (Connection connection = openConnection()) {
			IntegrationDatabase.requirePmis(connection);
			try (Statement statement = connection.createStatement()) {
				statement.execute("DROP SCHEMA " + schema + " CASCADE");
			}
		}
	}

	private static void requireTestSchema(String schema) {
		if (schema == null || !schema.matches(SCHEMA_PATTERN) || "pmis".equals(schema) || "public".equals(schema)
				|| schema.startsWith("pg_")) {
			throw new IllegalArgumentException("테스트 스키마명이 아닙니다.");
		}
	}

}
