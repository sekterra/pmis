package com.tommy.common.global.multitenancy;

import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import com.tommy.common.global.security.JwtTokenService;

import jakarta.servlet.http.HttpServletRequest;

@Component
public class SecurityContextTenantClaimReader implements TenantClaimReader {

	@Override
	public Optional<String> readTenantCode(HttpServletRequest request) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
			return Optional.empty();
		}
		if (!JwtTokenService.TENANT_TYPE.equals(jwt.getClaimAsString("typ"))) {
			return Optional.empty();
		}
		String tenant = jwt.getClaimAsString("tenant");
		if (tenant == null || tenant.isBlank()) {
			return Optional.empty();
		}
		return Optional.of(tenant.trim());
	}

}
