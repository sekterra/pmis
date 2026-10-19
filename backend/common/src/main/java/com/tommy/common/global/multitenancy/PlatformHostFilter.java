package com.tommy.common.global.multitenancy;

import java.io.IOException;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterProperties;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.tommy.common.global.response.ApiResponse;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.ObjectMapper;

@Component
@ConditionalOnWebApplication
@Order(SecurityFilterProperties.DEFAULT_FILTER_ORDER - 1)
public class PlatformHostFilter extends OncePerRequestFilter {

	private final PlatformHostProperties hosts;

	private final ObjectMapper objectMapper;

	public PlatformHostFilter(PlatformHostProperties hosts, ObjectMapper objectMapper) {
		this.hosts = hosts;
		this.objectMapper = objectMapper;
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		String uri = request.getRequestURI();
		return !hosts.restricted() || uri == null || !uri.startsWith("/api/platform/");
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		if (hosts.matches(PlatformHostProperties.requestHost(request))) {
			filterChain.doFilter(request, response);
			return;
		}
		response.setStatus(404);
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		objectMapper.writeValue(response.getWriter(),
				ApiResponse.fail("TENANT.NOT_FOUND", "테넌트를 찾을 수 없습니다.", 2, null, null));
	}

}
