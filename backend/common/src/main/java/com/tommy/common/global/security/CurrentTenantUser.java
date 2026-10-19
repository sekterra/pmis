package com.tommy.common.global.security;

import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class CurrentTenantUser {

	public Optional<Long> getCurrentAuditor() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
			return Optional.empty();
		}
		if (!JwtTokenService.TENANT_TYPE.equals(jwt.getClaimAsString("typ"))) {
			return Optional.empty();
		}
		String subject = jwt.getSubject();
		if (subject == null || !subject.chars().allMatch(Character::isDigit) || subject.isEmpty()) {
			return Optional.empty();
		}
		try {
			return Optional.of(Long.valueOf(subject));
		}
		catch (NumberFormatException ex) {
			return Optional.empty();
		}
	}

	public Long pkOrNull() {
		return getCurrentAuditor().orElse(null);
	}

}
