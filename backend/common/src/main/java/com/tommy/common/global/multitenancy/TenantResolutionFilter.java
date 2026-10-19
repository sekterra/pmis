package com.tommy.common.global.multitenancy;

import java.util.Optional;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.core.annotation.Order;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import tools.jackson.databind.ObjectMapper;
import com.tommy.common.global.exception.ApiException;
import com.tommy.common.global.response.ApiResponse;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@ConditionalOnWebApplication
@Order(SecurityFilterProperties.DEFAULT_FILTER_ORDER + 1)
public class TenantResolutionFilter extends OncePerRequestFilter {

	private final HostTenantResolver hostTenantResolver;
	private final JwtTenantResolver jwtTenantResolver;
	private final Optional<HeaderTenantResolver> headerTenantResolver;
	private final TenantDirectory directory;
	private final ObjectMapper objectMapper;

	public TenantResolutionFilter(
			HostTenantResolver hostTenantResolver,
			JwtTenantResolver jwtTenantResolver,
			Optional<HeaderTenantResolver> headerTenantResolver,
			TenantDirectory directory,
			ObjectMapper objectMapper) {
		this.hostTenantResolver = hostTenantResolver;
		this.jwtTenantResolver = jwtTenantResolver;
		this.headerTenantResolver = headerTenantResolver;
		this.directory = directory;
		this.objectMapper = objectMapper;
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		String uri = request.getRequestURI();
		return uri != null && (uri.startsWith("/actuator") || uri.startsWith("/api/platform/")
				|| "/api/mdm/v1/auth/jwks".equals(uri) || "/api/mdm/v1/context".equals(uri));
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws java.io.IOException, jakarta.servlet.ServletException {
		TenantContext.beginRequest();
		try {
			Optional<String> headerCode = headerTenantResolver.flatMap(resolver -> resolver.resolveTenantCode(request));
			String tenantCode = TenantRequestIdentifier.identify(
					hostTenantResolver.resolveTenantCode(request),
					jwtTenantResolver.resolveTenantCode(request),
					headerCode);
			TenantSnapshot snapshot = directory.findByTenantCode(tenantCode).orElse(null);
			TenantContext.setSchema(TenantAccess.requireActiveSchema(snapshot));
			filterChain.doFilter(request, response);
		}
		catch (ApiException ex) {
			if (response.isCommitted()) {
				throw ex;
			}
			response.setStatus(ex.status());
			response.setContentType("application/json");
			response.setCharacterEncoding("UTF-8");
			objectMapper.writeValue(response.getWriter(),
					ApiResponse.fail(ex.errorCode(), ex.getMessage(), ex.showType(), null, null));
		}
		finally {
			TenantContext.clear();
		}
	}

}
