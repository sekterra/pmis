package com.tommy.common;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.SpringApplication;

/**
 * 원격 테스트 DB로 기동하는 진입점. 환경변수가 없으면 기동하지 않는다.
 */
public class TestCommonApplication {

	public static void main(String[] args) {
		if (!TestcontainersConfiguration.isConfigured()) {
			System.err.println("MDM_TEST_DB_URL, MDM_TEST_DB_USERNAME, MDM_TEST_DB_PASSWORD 가 없어 실행하지 않습니다.");
			return;
		}
		Map<String, Object> properties = new HashMap<>();
		properties.put("spring.datasource.url", System.getenv("MDM_TEST_DB_URL"));
		properties.put("spring.datasource.username", System.getenv("MDM_TEST_DB_USERNAME"));
		properties.put("spring.datasource.password", System.getenv("MDM_TEST_DB_PASSWORD"));
		String flywayEnabled = System.getenv("MDM_FLYWAY_ENABLED");
		properties.put("spring.flyway.enabled", flywayEnabled == null || flywayEnabled.isBlank() ? "false" : flywayEnabled);
		SpringApplication application = new SpringApplication(CommonApplication.class);
		application.setDefaultProperties(properties);
		application.run(args);
	}

}
