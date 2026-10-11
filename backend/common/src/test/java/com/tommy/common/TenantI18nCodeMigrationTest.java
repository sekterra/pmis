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
 * 테넌트 V1만 빈 스키마 it_&lt;랜덤&gt; 에 적용하고 ERD 테이블/컬럼명과 비교한다. V2 이후는 포함하지 않는다.
 * 스키마 pmis 와 그 외 스키마는 변경하지 않는다. 환경변수가 없으면 skip 한다.
 */
@Tag("integration")
class TenantI18nCodeMigrationTest {

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
			assertI18nAndCodeConstraints(connection, schema);
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
				.locations("classpath:db/migration/tenant")
				.target("1")
				.load()
				.migrate();
	}

	private static void assertMatchesErd(Connection connection, String schema) throws Exception {
		Path erd = latestI18nErd();
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

	private static Path latestI18nErd() throws Exception {
		Path directory = Path.of("doc", "erd");
		try (Stream<Path> files = Files.list(directory)) {
			return files
					.filter(path -> {
						String name = path.getFileName().toString();
						return name.startsWith("erd-model-i18n-common-code-") && name.endsWith(".json");
					})
					.max(Comparator.comparing(path -> path.getFileName().toString()))
					.orElseThrow(() -> new IllegalStateException("doc/erd/erd-model-i18n-common-code-*.json 이 없습니다."));
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

	private static void assertI18nAndCodeConstraints(Connection connection, String schema) throws Exception {
		useSchema(connection, schema);
		execute(connection, """
				INSERT INTO support_language (support_language_code, language_name, default_yn, use_yn)
				VALUES ('en-US', 'English', 'Y', 'N')
				""");
		execute(connection, """
				INSERT INTO support_language (support_language_code, language_name, default_yn, use_yn)
				VALUES ('ko-KR', 'Korean', 'Y', 'Y')
				""");
		assertRejected(connection, """
				INSERT INTO support_language (support_language_code, language_name, default_yn, use_yn)
				VALUES ('ja-JP', 'Japanese', 'Y', 'Y')
				""");
		execute(connection, """
				INSERT INTO nation (nation_code, nation_name, default_language_code)
				VALUES ('KR', 'Korea', 'ko-KR')
				""");
		assertRejected(connection, """
				INSERT INTO nation (nation_code, nation_name, default_language_code)
				VALUES ('kr', 'Korea', 'ko-KR')
				""");
		long useYnGroupPk = insertReturningLong(connection, """
				INSERT INTO code_group (code_group_code, code_group_name)
				VALUES ('USE_YN', '사용여부')
				RETURNING code_group_pk
				""");
		assertRejected(connection, """
				INSERT INTO code_group (code_group_code, code_group_name)
				VALUES ('use_yn', '사용여부')
				""");
		long codePk = insertReturningLong(connection, """
				INSERT INTO common_code (code_group_pk, code_value, code_name, valid_from_date, valid_to_date)
				VALUES (%d, 'A', 'A', NULL, NULL)
				RETURNING code_pk
				""".formatted(useYnGroupPk));
		execute(connection, """
				INSERT INTO common_code (code_group_pk, code_value, code_name, valid_from_date, valid_to_date)
				VALUES (?, 'B', 'B', DATE '2026-01-01', DATE '2026-01-02')
				""", useYnGroupPk);
		assertRejected(connection, """
				INSERT INTO common_code (code_group_pk, code_value, code_name, valid_from_date, valid_to_date)
				VALUES (%d, 'C', 'C', DATE '2026-01-02', DATE '2026-01-01')
				""".formatted(useYnGroupPk));
		long otherGroupPk = insertReturningLong(connection, """
				INSERT INTO code_group (code_group_code, code_group_name)
				VALUES ('OTHER', '다른그룹')
				RETURNING code_group_pk
				""");
		execute(connection, """
				INSERT INTO common_code (code_group_pk, parent_code_pk, code_value, code_name)
				VALUES (?, ?, 'D', '다른그룹의 자식')
				""", otherGroupPk, codePk);
		org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
				() -> com.tommy.common.code.CommonCodeRules.requireSameCodeGroup(2L, 1L));
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
