package com.tommy.common.global.security;

import java.io.IOException;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.annotation.Order;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterProperties;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.tommy.common.global.exception.ApiException;
import com.tommy.common.global.multitenancy.TenantContext;
import com.tommy.common.global.response.ApiResponse;
import com.tommy.common.platform.PlatformAccountStatus;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.ObjectMapper;

@Component
@ConditionalOnWebApplication
@Order(SecurityFilterProperties.DEFAULT_FILTER_ORDER + 2)
public class AccountStatusFilter extends OncePerRequestFilter {

	private final AccountStatusCache cache;

	private final PlatformAccountStatus platformAccountStatus;

	private final ObjectMapper objectMapper;

	public AccountStatusFilter(@Lazy AccountStatusCache cache, @Lazy PlatformAccountStatus platformAccountStatus,
			@Lazy ObjectMapper objectMapper) {
		this.cache = cache;
		this.platformAccountStatus = platformAccountStatus;
		this.objectMapper = objectMapper;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
			if (JwtTokenService.TENANT_TYPE.equals(jwt.getClaimAsString("typ"))) {
				long userPk = Long.parseLong(jwt.getSubject());
				cache.requireActive(TenantContext.schema(), userPk);
			}
			else if (JwtTokenService.PLATFORM_TYPE.equals(jwt.getClaimAsString("typ"))
					&& !platformAccountStatus.active(Long.parseLong(jwt.getSubject()))) {
				write(response, new ApiException(401, "AUTH.ACCOUNT_DISABLED", "사용할 수 없는 계정입니다.", 2));
				return;
			}
		}
		filterChain.doFilter(request, response);
	}

	private void write(HttpServletResponse response, ApiException exception) throws IOException {
		response.setStatus(exception.status());
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		objectMapper.writeValue(response.getWriter(),
				ApiResponse.fail(exception.errorCode(), exception.getMessage(), exception.showType(), null, null));
	}

}
