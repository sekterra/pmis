package com.tommy.common.platform;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tommy.common.global.response.ApiResponse;
import com.tommy.common.global.security.JwtTokenService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

@RestController
@RequestMapping("/api/platform/v1/auth")
public class PlatformAuthController {

	private final PlatformAuthService authService;

	public PlatformAuthController(PlatformAuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/login")
	public ApiResponse<SessionResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest http, HttpServletResponse response) {
		return session(authService.login(request.loginId(), request.password(), http, response));
	}

	@PostMapping("/refresh")
	public ApiResponse<SessionResponse> refresh(HttpServletRequest http, HttpServletResponse response) {
		return session(authService.refresh(http, response));
	}

	@PostMapping("/logout")
	public ApiResponse<Void> logout(HttpServletRequest http, HttpServletResponse response) {
		authService.logout(http, response);
		return ApiResponse.ok(null);
	}

	@PostMapping("/password/change")
	public ApiResponse<SessionResponse> changePassword(@Valid @RequestBody PasswordChangeRequest request, HttpServletRequest http,
			HttpServletResponse response) {
		return session(authService.changePassword(request.currentPassword(), request.newPassword(), http, response));
	}

	@GetMapping("/me")
	public ApiResponse<PlatformAuthService.Me> me(HttpServletRequest http) {
		return ApiResponse.ok(authService.me(http));
	}

	public static long currentAdminPk() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)
				|| !JwtTokenService.PLATFORM_TYPE.equals(jwt.getClaimAsString("typ"))) {
			throw new com.tommy.common.global.exception.ApiException(401, "AUTH.UNAUTHORIZED", "인증이 필요합니다.", 2);
		}
		return Long.parseLong(jwt.getSubject());
	}

	private static ApiResponse<SessionResponse> session(PlatformAuthService.AuthSession session) {
		return ApiResponse.ok(new SessionResponse(session.accessToken(), "Bearer", session.expiresIn(), session.passwordChangeRequired()));
	}

	public record LoginRequest(@NotBlank String loginId, @NotBlank String password) {
	}

	public record PasswordChangeRequest(@NotBlank String currentPassword, @NotBlank String newPassword) {
	}

	public record SessionResponse(String accessToken, String tokenType, long expiresIn, boolean passwordChangeRequired) {
	}

}
