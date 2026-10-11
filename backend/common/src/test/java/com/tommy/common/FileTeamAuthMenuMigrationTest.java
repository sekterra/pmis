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

import com.tommy.common.auth.PermissionRules;

/**
 * 테넌트 V1·V2·V3를 빈 스키마 it_&lt;랜덤&gt; 에만 적용하고 파일/팀/권한/메뉴 ERD 컬럼과 제약을 확인한다.
 * 스키마 pmis 와 그 외 스키마는 변경하지 않는다. 환경변수가 없으면 skip 한다.
 */
@Tag("integration")
class FileTeamAuthMenuMigrationTest {

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
			assertFileTeamAuthConstraints(connection, schema);
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
				.load()
				.migrate();
	}

	private static void assertMatchesErd(Connection connection, String schema) throws Exception {
		Set<String> expectedTables = new TreeSet<>();
		expectedTables.addAll(tableNames(latestErd("erd-model-i18n-common-code-")));
		expectedTables.addAll(tableNames(latestErd("erd-model-org-user-")));
		List<Map<String, Object>> entities = entities(latestErd("erd-model-file-team-auth-menu-"));
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

	private static void assertFileTeamAuthConstraints(Connection connection, String schema) throws Exception {
		useSchema(connection, schema);
		long storagePk = insertReturningLong(connection, """
				INSERT INTO file_storage (storage_code, storage_name, storage_type_code, base_path, default_yn)
				VALUES ('DEFAULT', '기본', 'LOCAL', '/data', 'Y')
				RETURNING storage_pk
				""");
		assertRejected(connection, """
				INSERT INTO file_storage (storage_code, storage_name, storage_type_code, base_path, default_yn)
				VALUES ('ARCHIVE', '보관', 'LOCAL', '/archive', 'Y')
				""");
		execute(connection, """
				INSERT INTO file_storage (storage_code, storage_name, storage_type_code, base_path, default_yn, use_yn)
				VALUES ('OLD', '미사용', 'LOCAL', '/old', 'Y', 'N')
				""");
		long filePk = insertReturningLong(connection, """
				INSERT INTO file (storage_pk, storage_key, original_file_name, file_size, file_status_code, upload_user_pk, upload_datetime)
				VALUES (%d, 'key-1', 'a.pdf', 10, 'ACTIVE', 999999, CURRENT_TIMESTAMP)
				RETURNING file_pk
				""".formatted(storagePk));
		assertRejected(connection, """
				INSERT INTO file (storage_pk, storage_key, original_file_name, file_size, file_status_code, upload_datetime)
				VALUES (%d, 'key-1', 'b.pdf', 10, 'ACTIVE', CURRENT_TIMESTAMP)
				""".formatted(storagePk));
		assertRejected(connection, """
				INSERT INTO file (storage_pk, storage_key, original_file_name, file_size, file_status_code, upload_datetime)
				VALUES (%d, 'key-2', 'c.pdf', -1, 'ACTIVE', CURRENT_TIMESTAMP)
				""".formatted(storagePk));
		execute(connection, """
				INSERT INTO file_attachment (file_pk, target_table_name, target_key)
				VALUES (?, 'person', '1')
				""", filePk);
		assertRejected(connection, """
				INSERT INTO file_attachment (file_pk, target_table_name, target_key)
				VALUES (%d, 'person', '1')
				""".formatted(filePk));

		long personPk = insertReturningLong(connection, """
				INSERT INTO person (person_name)
				VALUES ('팀원')
				RETURNING person_pk
				""");
		execute(connection, "UPDATE person SET profile_file_pk = ? WHERE person_pk = ?", filePk, personPk);
		assertRejected(connection, "UPDATE person SET profile_file_pk = 0 WHERE person_pk = " + personPk);

		long teamPk = insertReturningLong(connection, """
				INSERT INTO team (team_code, team_name, team_type_code, start_date, end_date)
				VALUES ('ALPHA', '알파', 'PROJECT', DATE '2026-01-01', DATE '2026-12-31')
				RETURNING team_pk
				""");
		assertRejected(connection, """
				INSERT INTO team (team_code, team_name, team_type_code, start_date, end_date)
				VALUES ('BETA', '베타', 'PROJECT', DATE '2026-12-31', DATE '2026-01-01')
				""");
		execute(connection, """
				INSERT INTO team_member (team_pk, person_pk, team_role_code, start_date)
				VALUES (?, ?, 'LEADER', DATE '2026-01-01')
				""", teamPk, personPk);
		assertRejected(connection, """
				INSERT INTO team_member (team_pk, person_pk, team_role_code, start_date)
				VALUES (%d, %d, 'MEMBER', DATE '2026-02-01')
				""".formatted(teamPk, personPk));
		execute(connection, """
				INSERT INTO team_member (team_pk, person_pk, team_role_code, start_date, use_yn)
				VALUES (?, ?, 'ADVISOR', DATE '2026-02-01', 'N')
				""", teamPk, personPk);
		assertRejected(connection, """
				INSERT INTO team_member (team_pk, person_pk, team_role_code, start_date, end_date)
				VALUES (%d, %d, 'MEMBER', DATE '2026-03-01', DATE '2026-02-01')
				""".formatted(teamPk, personPk));

		long userPk = insertReturningLong(connection, """
				INSERT INTO user_account (account_type_code, login_id, user_status_code)
				VALUES ('SYSTEM', 'file-admin', 'ACTIVE')
				RETURNING user_pk
				""");
		assertRejected(connection, """
				INSERT INTO role (role_code, role_name)
				VALUES ('viewer', '조회')
				""");
		long rolePk = insertReturningLong(connection, """
				INSERT INTO role (role_code, role_name)
				VALUES ('VIEWER', '조회')
				RETURNING role_pk
				""");
		long otherRolePk = insertReturningLong(connection, """
				INSERT INTO role (role_code, role_name)
				VALUES ('EDITOR', '편집')
				RETURNING role_pk
				""");
		execute(connection, """
				INSERT INTO user_role (user_pk, role_pk, valid_from_date, valid_to_date)
				VALUES (?, ?, DATE '2026-01-01', DATE '2026-12-31')
				""", userPk, rolePk);
		assertRejected(connection, """
				INSERT INTO user_role (user_pk, role_pk, valid_from_date, valid_to_date)
				VALUES (%d, %d, DATE '2026-12-31', DATE '2026-01-01')
				""".formatted(userPk, otherRolePk));
		assertRejected(connection, """
				INSERT INTO user_role (user_pk, role_pk)
				VALUES (%d, %d)
				""".formatted(userPk, rolePk));

		long menuPk = insertReturningLong(connection, """
				INSERT INTO menu (solution_code, menu_code, menu_name, menu_type_code)
				VALUES ('MDM', 'MDM.CODE_GROUP', '공통코드', 'PAGE')
				RETURNING menu_pk
				""");
		assertRejected(connection, """
				INSERT INTO menu (solution_code, menu_code, menu_name, menu_type_code, visible_yn)
				VALUES ('MDM', 'MDM.HIDDEN', '숨김', 'PAGE', 'X')
				""");
		execute(connection, """
				INSERT INTO menu (solution_code, menu_code, menu_name, menu_type_code)
				VALUES ('MDM', 'MDM.UNKNOWN_TYPE', '형식없음', 'NOT_A_CODE')
				""");
		long createPermissionPk = insertReturningLong(connection, """
				INSERT INTO permission (menu_pk, action_code, permission_code, permission_name)
				VALUES (%d, 'CREATE', 'MDM.CODE_GROUP:CREATE', '등록')
				RETURNING permission_pk
				""".formatted(menuPk));
		assertRejected(connection, """
				INSERT INTO permission (menu_pk, action_code, permission_code, permission_name)
				VALUES (%d, 'CREATE', 'MDM.CODE_GROUP:CREATE2', '등록중복')
				""".formatted(menuPk));
		execute(connection, """
				INSERT INTO role_permission (role_pk, permission_pk)
				VALUES (?, ?)
				""", rolePk, createPermissionPk);
		org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
				() -> PermissionRules.requireReadWhenGrantingOther(false, "CREATE"));
	}

	@SuppressWarnings("unchecked")
	private static List<Map<String, Object>> castList(Object value) {
		return (List<Map<String, Object>>) value;
	}

	private static Set<String> tableNames(Path erd) throws Exception {
		Set<String> names = new TreeSet<>();
		for (Map<String, Object> entity : entities(erd)) {
			names.add(String.valueOf(entity.get("physicalName")));
		}
		return names;
	}

	private static List<Map<String, Object>> entities(Path erd) throws Exception {
		Map<String, Object> root = JsonParserFactory.getJsonParser().parseMap(Files.readString(erd));
		return castList(root.get("entities"));
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

	private static void dropSchema(String schema) throws Exception {
		requireTestSchema(schema);
		try (Connection connection = openConnection()) {
			IntegrationDatabase.requirePmis(connection);
			try (Statement statement = connection.createStatement()) {
				statement.execute("DROP SCHEMA " + schema + " CASCADE");
			}
		}
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
