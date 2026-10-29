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

import com.tommy.common.global.exception.ApiException;
import com.tommy.common.global.security.JwtTokenService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@ConditionalOnWebApplication
@Order(SecurityFilterProperties.DEFAULT_FILTER_ORDER + 5)
public class SupportAuditFilter extends OncePerRequestFilter {

	static final String END_PATH = "/api/mdm/v1/auth/support-access/end";

	private final SupportAuditStore auditStore;

	public SupportAuditFilter(SupportAuditStore auditStore) {
		this.auditStore = auditStore;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		Jwt jwt = authentication != null && authentication.getPrincipal() instanceof Jwt value ? value : null;
		Long accessPk = jwt == null ? null : SupportAccessRules.claimLong(jwt, "sup");
		String path = request.getRequestURI();
		boolean audit = jwt != null && JwtTokenService.TENANT_TYPE.equals(jwt.getClaimAsString("typ")) && accessPk != null
				&& SupportAccessRules.writeMethod(request.getMethod()) && !END_PATH.equals(path);
		if (!audit) {
			filterChain.doFilter(request, response);
			return;
		}
		long actionPk;
		try {
			actionPk = auditStore.insert(accessPk, request.getMethod(), path, SupportAccessRules.sensitive(request.getMethod(), path));
		}
		catch (RuntimeException ex) {
			throw new ApiException(503, "SUPPORT.AUDIT_UNAVAILABLE", "지원 접속 감사를 기록하지 못했습니다.", 2);
		}
		try {
			filterChain.doFilter(request, response);
		}
		finally {
			int statusCode = response.getStatus();
			auditStore.complete(actionPk, statusCode == 0 ? 500 : statusCode);
		}
	}

}
