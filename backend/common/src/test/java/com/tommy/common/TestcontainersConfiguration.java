package com.tommy.common;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;

/**
 * 원격 테스트 DB 설정. Docker/Testcontainers 는 사용하지 않는다.
 * 접속 정보는 MDM_TEST_DB_URL, MDM_TEST_DB_USERNAME, MDM_TEST_DB_PASSWORD 환경변수로만 받는다.
 */
@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {

	public static boolean isConfigured() {
		return isPresent("MDM_TEST_DB_URL")
				&& isPresent("MDM_TEST_DB_USERNAME")
				&& isPresent("MDM_TEST_DB_PASSWORD");
	}

	public static void registerDatasource(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", () -> System.getenv("MDM_TEST_DB_URL"));
		registry.add("spring.datasource.username", () -> System.getenv("MDM_TEST_DB_USERNAME"));
		registry.add("spring.datasource.password", () -> System.getenv("MDM_TEST_DB_PASSWORD"));
		registry.add("spring.flyway.enabled", () -> "false");
		registry.add("spring.datasource.hikari.maximum-pool-size", () -> "5");
	}

	private static boolean isPresent(String name) {
		String value = System.getenv(name);
		return value != null && !value.isBlank();
	}

}
