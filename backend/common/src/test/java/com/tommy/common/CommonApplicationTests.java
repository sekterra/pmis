package com.tommy.common;

import java.sql.Connection;
import java.sql.DriverManager;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * 컨텍스트 로딩 확인. 환경변수가 없거나 접속 DB가 pmis 가 아니면 컨텍스트를 띄우기 전에 skip 한다.
 * 확인은 읽기 전용 연결의 current_database() 만 사용한다. Flyway 는 켜지 않는다.
 */
@Tag("integration")
@EnabledIfEnvironmentVariable(named = "MDM_TEST_DB_URL", matches = ".+")
@EnabledIfEnvironmentVariable(named = "MDM_TEST_DB_USERNAME", matches = ".+")
@EnabledIfEnvironmentVariable(named = "MDM_TEST_DB_PASSWORD", matches = ".+")
@EnabledIf("databaseIsPmis")
@SpringBootTest
class CommonApplicationTests {

	@DynamicPropertySource
	static void datasource(DynamicPropertyRegistry registry) {
		TestcontainersConfiguration.registerDatasource(registry);
		TestJwtKey.register(registry);
	}

	static boolean databaseIsPmis() {
		if (!TestcontainersConfiguration.isConfigured()) {
			return false;
		}
		try (Connection connection = DriverManager.getConnection(
				System.getenv("MDM_TEST_DB_URL"),
				System.getenv("MDM_TEST_DB_USERNAME"),
				System.getenv("MDM_TEST_DB_PASSWORD"))) {
			connection.setReadOnly(true);
			IntegrationDatabase.requirePmis(connection);
			return true;
		}
		catch (Exception ex) {
			return false;
		}
	}

	@Test
	void contextLoads() {
	}

}
