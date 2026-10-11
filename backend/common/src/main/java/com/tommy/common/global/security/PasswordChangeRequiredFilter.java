package com.tommy.common.global.security;

import java.io.IOException;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.tommy.common.global.exception.ApiException;
import com.tommy.common.global.response.ApiResponse;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.ObjectMapper;

@Component
@ConditionalOnWebApplication
@Order(org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterProperties.DEFAULT_FILTER_ORDER + 3)
public class PasswordChangeRequiredFilter extends OncePerRequestFilter {

	private final ObjectMapper objectMapper;

	public PasswordChangeRequiredFilter(@Lazy ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt
				&& Boolean.TRUE.equals(jwt.getClaim("pwd_change_required")) && !allowed(request, jwt.getClaimAsString("typ"))) {
			ApiException exception = new ApiException(403, "AUTH.PASSWORD_CHANGE_REQUIRED", "비밀번호를 변경해야 합니다.", 2);
			if (JwtTokenService.PLATFORM_TYPE.equals(jwt.getClaimAsString("typ"))) {
				response.setStatus(exception.status());
				response.setContentType("application/json");
				response.setCharacterEncoding("UTF-8");
				objectMapper.writeValue(response.getWriter(),
						ApiResponse.fail(exception.errorCode(), exception.getMessage(), exception.showType(), null, null));
				return;
			}
			throw exception;
		}
		filterChain.doFilter(request, response);
	}

	private static boolean allowed(HttpServletRequest request, String type) {
		String uri = request.getRequestURI();
		String method = request.getMethod();
		if (JwtTokenService.PLATFORM_TYPE.equals(type)) {
			if ("POST".equals(method) && ("/api/platform/v1/auth/password/change".equals(uri) || "/api/platform/v1/auth/logout".equals(uri)
					|| "/api/platform/v1/auth/refresh".equals(uri))) {
				return true;
			}
			return "GET".equals(method) && "/api/platform/v1/auth/me".equals(uri);
		}
		if (!JwtTokenService.TENANT_TYPE.equals(type)) {
			return true;
		}
		if ("POST".equals(method) && ("/api/mdm/v1/auth/password/change".equals(uri) || "/api/mdm/v1/auth/logout".equals(uri)
				|| "/api/mdm/v1/auth/refresh".equals(uri))) {
			return true;
		}
		return "GET".equals(method) && "/api/mdm/v1/auth/me".equals(uri);
	}

}
