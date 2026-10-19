package com.tommy.common.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import com.tommy.common.global.security.CurrentTenantUser;

@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {

	@Bean
	public AuditorAware<Long> auditorAware(CurrentTenantUser currentTenantUser) {
		return currentTenantUser::getCurrentAuditor;
	}

}
