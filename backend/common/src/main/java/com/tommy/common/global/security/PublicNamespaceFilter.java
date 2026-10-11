package com.tommy.common.global.security;

import java.io.IOException;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.filter.OncePerRequestFilter;

import com.tommy.common.global.response.ApiResponse;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.ObjectMapper;

public class PublicNamespaceFilter extends OncePerRequestFilter {

	private final MdmSecurityProperties properties;

	private final ObjectMapper objectMapper;

	public PublicNamespaceFilter(MdmSecurityProperties properties, ObjectMapper objectMapper) {
		this.properties = properties;
		this.objectMapper = objectMapper;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		if (!"GET".equals(request.getMethod()) || authenticated() || allowed(request)) {
			filterChain.doFilter(request, response);
			return;
		}
		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");
		objectMapper.writeValue(response.getWriter(),
				ApiResponse.fail("AUTH.UNAUTHORIZED", "인증이 필요합니다.", 2, UUID.randomUUID().toString(), null));
	}

	private boolean allowed(HttpServletRequest request) {
		String uri = request.getRequestURI();
		if (uri == null || !uri.startsWith("/api/mdm/v1/i18n/bundles")) {
			return true;
		}
		if (uri.equals("/api/mdm/v1/i18n/bundles")) {
			String raw = request.getParameter("namespaces");
			if (raw == null || raw.isBlank()) {
				return false;
			}
			for (String part : raw.split(",")) {
				if (!properties.getPublicNamespaces().contains(part.trim())) {
					return false;
				}
			}
			return true;
		}
		String prefix = "/api/mdm/v1/i18n/bundles/";
		if (!uri.startsWith(prefix) || uri.length() == prefix.length()) {
			return false;
		}
		return properties.getPublicNamespaces().contains(uri.substring(prefix.length()));
	}

	private static boolean authenticated() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		return authentication != null && authentication.getPrincipal() instanceof Jwt && authentication.isAuthenticated();
	}

}
