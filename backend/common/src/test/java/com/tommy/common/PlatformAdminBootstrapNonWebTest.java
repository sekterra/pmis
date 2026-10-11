package com.tommy.common;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.sql.DataSource;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.context.WebApplicationContext;

import com.tommy.common.global.multitenancy.TenantResolutionFilter;
import com.tommy.common.global.security.AccountStatusFilter;
import com.tommy.common.global.security.ApiAuthenticationEntryPoint;
import com.tommy.common.global.security.ApiSecurityConfig;
import com.tommy.common.global.security.PasswordChangeRequiredFilter;
import com.tommy.common.global.security.PasswordHasher;
import com.tommy.common.global.security.PermissionInterceptor;
import com.tommy.common.platform.PlatformAdminBootstrapRunner;
import com.tommy.common.platform.PlatformAdminBootstrapService;

import jakarta.persistence.EntityManagerFactory;

/**
 * 공유 스키마 pmis.platform_admin 에 계정을 넣지 않는다.
 * 부트스트랩 SQL 이 pmis.platform_admin 으로 고정되어 테스트 스키마로 격리할 수 없다.
 */
@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("dev")
class PlatformAdminBootstrapNonWebTest {

	@Autowired
	private ApplicationContext context;

	@DynamicPropertySource
	static void datasource(DynamicPropertyRegistry registry) {
		TestcontainersConfiguration.registerDatasource(registry);
		registry.add("mdm.bootstrap.platform-admin", () -> "false");
	}

	@Test
	void startsNonWebContextWithoutServletSecurity() {
		assertFalse(context instanceof WebApplicationContext);
		assertNotNull(context.getBean(DataSource.class));
		assertNotNull(context.getBean(EntityManagerFactory.class));
		assertNotNull(context.getBean(PlatformAdminBootstrapService.class));
		assertNotNull(context.getBean(PasswordHasher.class));
		assertTrue(context.getBeansOfType(ApiSecurityConfig.class).isEmpty());
		assertTrue(context.getBeansOfType(SecurityFilterChain.class).isEmpty());
		assertTrue(context.getBeansOfType(PasswordChangeRequiredFilter.class).isEmpty());
		assertTrue(context.getBeansOfType(AccountStatusFilter.class).isEmpty());
		assertTrue(context.getBeansOfType(TenantResolutionFilter.class).isEmpty());
		assertTrue(context.getBeansOfType(PermissionInterceptor.class).isEmpty());
		assertTrue(context.getBeansOfType(ApiAuthenticationEntryPoint.class).isEmpty());
		assertTrue(context.getBeansOfType(PlatformAdminBootstrapRunner.class).isEmpty());
	}

}
