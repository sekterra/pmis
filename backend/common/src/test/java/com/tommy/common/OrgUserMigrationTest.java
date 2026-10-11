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
 * 테넌트 V1·V2만 빈 스키마 it_&lt;랜덤&gt; 에 적용하고 회사/사람 ERD 컬럼과 제약을 확인한다. V3 이후는 포함하지 않는다.
 * 스키마 pmis 와 그 외 스키마는 변경하지 않는다. 환경변수가 없으면 skip 한다.
 */
@Tag("integration")
class OrgUserMigrationTest {

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
			assertOrgUserConstraints(connection, schema);
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
				.target("2")
				.load()
				.migrate();
	}

	private static void assertMatchesErd(Connection connection, String schema) throws Exception {
		Set<String> expectedTables = new TreeSet<>();
		expectedTables.addAll(tableNames(latestErd("erd-model-i18n-common-code-")));
		List<Map<String, Object>> orgEntities = entities(latestErd("erd-model-org-user-"));
		for (Map<String, Object> entity : orgEntities) {
			expectedTables.add(String.valueOf(entity.get("physicalName")));
		}

		Set<String> actualTables = queryStrings(connection, """
				SELECT table_name
				FROM information_schema.tables
				WHERE table_schema = ? AND table_type = 'BASE TABLE' AND table_name <> 'flyway_schema_history'
				""", schema);
		assertEquals(expectedTables, new TreeSet<>(actualTables));

		for (Map<String, Object> entity : orgEntities) {
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

	private static void assertOrgUserConstraints(Connection connection, String schema) throws Exception {
		useSchema(connection, schema);
		execute(connection, """
				INSERT INTO support_language (support_language_code, language_name)
				VALUES ('ko-KR', 'Korean')
				""");
		long nationPk = insertReturningLong(connection, """
				INSERT INTO nation (nation_code, nation_name, default_language_code)
				VALUES ('KR', 'Korea', 'ko-KR')
				RETURNING nation_pk
				""");
		long companyPk = insertReturningLong(connection, """
				INSERT INTO company (company_code, company_name, company_type_code, nation_pk, business_registration_no)
				VALUES ('acme', 'Acme', 'OWN', %d, '123-45-67890')
				RETURNING company_pk
				""".formatted(nationPk));
		long childCompanyPk = insertReturningLong(connection, """
				INSERT INTO company (parent_company_pk, company_code, company_name, company_type_code)
				VALUES (%d, 'acme-sub', 'Acme Sub', 'AFFILIATE')
				RETURNING company_pk
				""".formatted(companyPk));
		assertTrue(childCompanyPk > 0);
		assertRejected(connection, """
				INSERT INTO company (company_code, company_name, company_type_code)
				VALUES ('acme', 'Dup', 'OTHER')
				""");
		assertRejected(connection, """
				INSERT INTO company (company_code, company_name, company_type_code, business_registration_no)
				VALUES ('other', 'Other', 'OTHER', '123-45-67890')
				""");
		execute(connection, """
				INSERT INTO company (company_code, company_name, company_type_code)
				VALUES ('null-brn-1', 'Null 1', 'OTHER')
				""");
		execute(connection, """
				INSERT INTO company (company_code, company_name, company_type_code)
				VALUES ('null-brn-2', 'Null 2', 'OTHER')
				""");
		assertRejected(connection, """
				INSERT INTO company (company_code, company_name, company_type_code, nation_pk)
				VALUES ('bad-nation', 'Bad', 'OTHER', 0)
				""");

		long deptPk = insertReturningLong(connection, """
				INSERT INTO dept (company_pk, dept_code, dept_name, valid_from_date, valid_to_date)
				VALUES (%d, 'D1', '개발', DATE '2026-01-01', DATE '2026-12-31')
				RETURNING dept_pk
				""".formatted(companyPk));
		assertRejected(connection, """
				INSERT INTO dept (company_pk, dept_code, dept_name, valid_from_date, valid_to_date)
				VALUES (%d, 'D2', '역전기간', DATE '2026-12-31', DATE '2026-01-01')
				""".formatted(companyPk));
		assertRejected(connection, """
				INSERT INTO dept (company_pk, dept_code, dept_name, valid_from_date)
				VALUES (%d, 'D1', '중복코드', DATE '2026-01-01')
				""".formatted(companyPk));

		long personPk = insertReturningLong(connection, """
				INSERT INTO person (person_name)
				VALUES ('홍길동')
				RETURNING person_pk
				""");
		insertReturningLong(connection, """
				INSERT INTO person_assignment (person_pk, company_pk, dept_pk, employee_no, employment_type_code, assignment_status_code, primary_yn, start_date)
				VALUES (%d, %d, %d, 'E001', 'REGULAR', 'ACTIVE', 'Y', DATE '2026-01-01')
				RETURNING assignment_pk
				""".formatted(personPk, companyPk, deptPk));
		assertRejected(connection, """
				INSERT INTO person_assignment (person_pk, company_pk, employee_no, employment_type_code, assignment_status_code, primary_yn, start_date)
				VALUES (%d, %d, 'E002', 'REGULAR', 'ACTIVE', 'Y', DATE '2026-02-01')
				""".formatted(personPk, companyPk));
		execute(connection, """
				INSERT INTO person_assignment (person_pk, company_pk, employee_no, employment_type_code, assignment_status_code, primary_yn, start_date, use_yn)
				VALUES (?, ?, 'E-UNUSED', 'REGULAR', 'ACTIVE', 'Y', DATE '2026-02-01', 'N')
				""", personPk, companyPk);
		long unusedOnlyPersonPk = insertReturningLong(connection, """
				INSERT INTO person (person_name)
				VALUES ('논리삭제만')
				RETURNING person_pk
				""");
		execute(connection, """
				INSERT INTO person_assignment (person_pk, company_pk, employee_no, employment_type_code, assignment_status_code, primary_yn, start_date, use_yn)
				VALUES (?, ?, 'E-OLD-N', 'REGULAR', 'ACTIVE', 'Y', DATE '2026-01-01', 'N')
				""", unusedOnlyPersonPk, companyPk);
		execute(connection, """
				INSERT INTO person_assignment (person_pk, company_pk, employee_no, employment_type_code, assignment_status_code, primary_yn, start_date)
				VALUES (?, ?, 'E-NEW-Y', 'REGULAR', 'ACTIVE', 'Y', DATE '2026-02-01')
				""", unusedOnlyPersonPk, companyPk);
		execute(connection, """
				INSERT INTO person_assignment (person_pk, company_pk, employee_no, employment_type_code, assignment_status_code, primary_yn, start_date, end_date)
				VALUES (?, ?, 'E001-OLD', 'REGULAR', 'RESIGNED', 'Y', DATE '2025-01-01', DATE '2025-12-31')
				""", personPk, companyPk);
		assertRejected(connection, """
				INSERT INTO person_assignment (person_pk, company_pk, employee_no, employment_type_code, assignment_status_code, start_date, end_date)
				VALUES (%d, %d, 'E003', 'REGULAR', 'ACTIVE', DATE '2026-02-01', DATE '2026-01-01')
				""".formatted(personPk, companyPk));
		assertRejected(connection, """
				INSERT INTO person_assignment (person_pk, company_pk, employee_no, employment_type_code, assignment_status_code, start_date)
				VALUES (%d, %d, 'E001', 'CONTRACT', 'ACTIVE', DATE '2026-03-01')
				""".formatted(personPk, companyPk));
		execute(connection, """
				INSERT INTO person_assignment (person_pk, company_pk, employment_type_code, assignment_status_code, start_date)
				VALUES (?, ?, 'EXTERNAL', 'ACTIVE', DATE '2026-03-01')
				""", personPk, companyPk);
		execute(connection, """
				INSERT INTO person_assignment (person_pk, company_pk, employment_type_code, assignment_status_code, start_date)
				VALUES (?, ?, 'EXTERNAL', 'ACTIVE', DATE '2026-04-01')
				""", personPk, companyPk);

		long userPk = insertReturningLong(connection, """
				INSERT INTO user_account (person_pk, account_type_code, login_id, user_status_code, support_language_code)
				VALUES (%d, 'NORMAL', 'hong', 'ACTIVE', 'ko-KR')
				RETURNING user_pk
				""".formatted(personPk));
		assertRejected(connection, """
				INSERT INTO user_account (person_pk, account_type_code, login_id, user_status_code)
				VALUES (%d, 'NORMAL', 'hong2', 'ACTIVE')
				""".formatted(personPk));
		assertRejected(connection, """
				INSERT INTO user_account (account_type_code, login_id, user_status_code)
				VALUES ('SYSTEM', 'hong', 'ACTIVE')
				""");
		assertRejected(connection, """
				INSERT INTO user_account (account_type_code, login_id, user_status_code)
				VALUES ('SYSTEM', 'Hong', 'ACTIVE')
				""");
		execute(connection, """
				INSERT INTO user_account (account_type_code, login_id, user_status_code)
				VALUES ('SYSTEM', 'system-a', 'ACTIVE')
				""");
		execute(connection, """
				INSERT INTO user_account (account_type_code, login_id, user_status_code)
				VALUES ('SYSTEM', 'system-b', 'ACTIVE')
				""");
		assertRejected(connection, """
				INSERT INTO user_account (account_type_code, login_id, user_status_code, support_language_code)
				VALUES ('SYSTEM', 'bad-lang', 'ACTIVE', 'xx-XX')
				""");
		execute(connection, """
				INSERT INTO user_login_history (user_pk, login_id_input, login_result_code, login_datetime)
				VALUES (?, 'hong', 'SUCCESS', CURRENT_TIMESTAMP)
				""", userPk);
		execute(connection, """
				INSERT INTO user_login_history (login_id_input, login_result_code, login_datetime)
				VALUES ('missing', 'FAIL_NOT_FOUND', CURRENT_TIMESTAMP)
				""");
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
