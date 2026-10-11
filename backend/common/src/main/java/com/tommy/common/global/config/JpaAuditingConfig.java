package com.tommy.common.global.config;

import java.util.Optional;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {

	@Bean
	public AuditorAware<Long> auditorAware() {
		// TODO: 사용자 체계가 도입되면 현재 사용자 PK를 반환하도록 교체한다.
		return () -> Optional.empty();
	}

}
