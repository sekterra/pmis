package com.tommy.common.supportaccess;

import java.io.IOException;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterProperties;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.tommy.common.global.security.JwtTokenService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@ConditionalOnWebApplication
@Order(SecurityFilterProperties.DEFAULT_FILTER_ORDER + 4)
public class SupportSessionFilter extends OncePerRequestFilter {

	private final SupportAccessService supportAccessService;

	public SupportSessionFilter(SupportAccessService supportAccessService) {
		this.supportAccessService = supportAccessService;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt
				&& JwtTokenService.TENANT_TYPE.equals(jwt.getClaimAsString("typ"))
				&& SupportAccessRules.claimLong(jwt, "sup") != null
				&& !SupportAuditFilter.END_PATH.equals(request.getRequestURI())) {
			supportAccessService.requireSession(jwt);
		}
		filterChain.doFilter(request, response);
	}

}
