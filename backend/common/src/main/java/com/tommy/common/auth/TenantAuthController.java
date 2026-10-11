package com.tommy.common.auth;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tommy.common.global.response.ApiResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

@RestController
@RequestMapping("/api/mdm/v1/auth")
public class TenantAuthController {

	private final TenantAuthService authService;

	public TenantAuthController(TenantAuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/login")
	public ApiResponse<SessionResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest http,
			HttpServletResponse response) {
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
	public ApiResponse<TenantAuthService.Me> me() {
		return ApiResponse.ok(authService.me());
	}

	private static ApiResponse<SessionResponse> session(TenantAuthService.AuthSession session) {
		return ApiResponse.ok(new SessionResponse(session.accessToken(), "Bearer", session.expiresIn(), session.passwordChangeRequired()));
	}

	public record LoginRequest(@NotBlank String loginId, @NotBlank String password) {
	}

	public record PasswordChangeRequest(@NotBlank String currentPassword, @NotBlank String newPassword) {
	}

	public record SessionResponse(String accessToken, String tokenType, long expiresIn, boolean passwordChangeRequired) {
	}

}
