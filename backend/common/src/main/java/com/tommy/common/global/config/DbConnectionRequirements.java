package com.tommy.common.global.config;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;

public final class DbConnectionRequirements implements ApplicationContextInitializer<ConfigurableApplicationContext> {

	public static final String MISSING = "DB 접속 설정이 없습니다. config/application-local.yaml 또는 환경변수 MDM_DB_URL/MDM_DB_USERNAME/MDM_DB_PASSWORD를 설정하세요";

	@Override
	public void initialize(ConfigurableApplicationContext applicationContext) {
		require(applicationContext.getEnvironment());
	}

	public static void require(Environment environment) {
		String url = environment.getProperty("spring.datasource.url");
		String username = environment.getProperty("spring.datasource.username");
		String password = environment.getProperty("spring.datasource.password");
		if (isBlank(url) || isBlank(username) || isBlank(password) || !url.startsWith("jdbc:")) {
			throw new IllegalStateException(MISSING);
		}
	}

	private static boolean isBlank(String value) {
		return value == null || value.isBlank();
	}

}
