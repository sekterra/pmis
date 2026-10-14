package com.tommy.common.global.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Profiles;
import org.springframework.mock.env.MockEnvironment;

class LocalConfigTest {

	private static final String ENV_URL = "jdbc:postgresql://env.invalid:1/pmis";

	private static final String FILE_URL = "jdbc:postgresql://file.invalid:1/pmis";

	private final ApplicationContextRunner runner = new ApplicationContextRunner()
			.withInitializer(new ConfigDataApplicationContextInitializer());

	@Test
	void resolvesDatasourceFromEnvironmentWhenLocalFileIsAbsent() {
		runner.withPropertyValues("MDM_DB_URL=" + ENV_URL, "MDM_DB_USERNAME=env-user", "MDM_DB_PASSWORD=env-password")
				.run(context -> assertEquals(ENV_URL, context.getEnvironment().getProperty("spring.datasource.url")));
	}

	@Test
	void environmentOverridesLocalFile() throws Exception {
		Path file = Files.createTempFile("application-local", ".yaml");
		file.toFile().deleteOnExit();
		Files.writeString(file, """
				mdm:
				  db:
				    url: %s
				    username: file-user
				    password: file-password
				""".formatted(FILE_URL));
		runner.withPropertyValues("spring.config.import=" + file.toUri(), "MDM_DB_URL=" + ENV_URL,
				"MDM_DB_USERNAME=env-user", "MDM_DB_PASSWORD=env-password")
				.run(context -> {
					assertEquals(ENV_URL, context.getEnvironment().getProperty("spring.datasource.url"));
					assertEquals("env-user", context.getEnvironment().getProperty("spring.datasource.username"));
				});
	}

	@Test
	void rejectsMissingOrNonJdbcSettingsWithoutRevealingTheValue() {
		MockEnvironment empty = new MockEnvironment();
		IllegalStateException missing = assertThrows(IllegalStateException.class, () -> DbConnectionRequirements.require(empty));
		assertEquals(DbConnectionRequirements.MISSING, missing.getMessage());

		MockEnvironment bad = new MockEnvironment().withProperty("spring.datasource.url", "postgres://hidden.invalid/pmis")
				.withProperty("spring.datasource.username", "hidden-user")
				.withProperty("spring.datasource.password", "hidden-password");
		IllegalStateException rejected = assertThrows(IllegalStateException.class, () -> DbConnectionRequirements.require(bad));
		assertEquals(DbConnectionRequirements.MISSING, rejected.getMessage());
		assertFalse(rejected.getMessage().contains("hidden"));
		assertFalse(rejected.getMessage().contains("postgres"));

		FailureAnalysis analysis = new DatasourceFailureAnalyzer().analyze(new IllegalStateException("jdbc:postgresql://hidden.invalid/pmis"));
		assertTrue(analysis != null);
		assertFalse(analysis.getDescription().contains("hidden"));
		assertFalse(analysis.getCause().toString().contains("hidden"));
	}

	@Test
	void localProfileIncludesDev() {
		runner.withPropertyValues("spring.profiles.active=local").run(context -> {
			ConfigurableEnvironment environment = context.getEnvironment();
			assertTrue(environment.acceptsProfiles(Profiles.of("dev")));
			assertTrue(environment.acceptsProfiles(Profiles.of("local")));
		});
	}

	@Test
	void defaultStartupDoesNotEnableDevOrLocal() {
		runner.run(context -> {
			ConfigurableEnvironment environment = context.getEnvironment();
			assertFalse(environment.acceptsProfiles(Profiles.of("dev")));
			assertFalse(environment.acceptsProfiles(Profiles.of("local")));
			assertFalse(Arrays.asList(environment.getActiveProfiles()).contains("dev"));
			assertEquals(0, environment.getActiveProfiles().length);
		});
		assertThrows(IllegalStateException.class, () -> com.tommy.common.global.security.JwtSigningKeys.load("", "", false));
	}

}
