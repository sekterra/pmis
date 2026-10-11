package com.tommy.common.global.security;

import java.io.IOException;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.tommy.common.global.response.ApiResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.ObjectMapper;

@Component
@ConditionalOnWebApplication
public class ApiAuthenticationEntryPoint implements AuthenticationEntryPoint {

	private final ObjectMapper objectMapper;

	public ApiAuthenticationEntryPoint(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
			throws IOException {
		String code = "AUTH.TOKEN_REJECTED";
		String message = "토큰이 유효하지 않습니다.";
		Throwable current = exception;
		boolean malformed = false;
		boolean expired = false;
		boolean missing = exception instanceof InsufficientAuthenticationException;
		while (current != null) {
			if (current instanceof JwtValidationException) {
				expired = true;
			}
			String text = current.getMessage() == null ? "" : current.getMessage();
			if (text.contains("malformed")) {
				malformed = true;
			}
			if (text.contains("expired")) {
				expired = true;
			}
			current = current.getCause();
		}
		if (missing || malformed) {
			code = "AUTH.UNAUTHORIZED";
			message = "인증이 필요합니다.";
		}
		else if (expired) {
			code = "AUTH.TOKEN_EXPIRED";
			message = "토큰이 만료되었습니다.";
		}
		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");
		objectMapper.writeValue(response.getWriter(), ApiResponse.fail(code, message, 2, UUID.randomUUID().toString(), null));
	}

}
