package com.tommy.common.supportaccess;

import java.util.List;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tommy.common.global.exception.ApiException;
import com.tommy.common.global.response.ApiResponse;
import com.tommy.common.global.response.PageResponse;
import com.tommy.common.global.security.RequirePermission;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

@RestController
public class TenantSupportAccessController {

	private final SupportAccessService supportAccessService;

	public TenantSupportAccessController(SupportAccessService supportAccessService) {
		this.supportAccessService = supportAccessService;
	}

	@PostMapping("/api/mdm/v1/auth/support-access/exchange")
	public ResponseEntity<ApiResponse<SupportAccessService.Exchanged>> exchange(@Valid @RequestBody ExchangeRequest body,
			HttpServletRequest request) {
		requireCsrf(request);
		return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiResponse.ok(supportAccessService.exchange(body.ticket(), request)));
	}

	@PostMapping("/api/mdm/v1/auth/support-access/end")
	public ApiResponse<Void> end(HttpServletRequest request) {
		requireCsrf(request);
		supportAccessService.end(currentJwt());
		return ApiResponse.ok(null);
	}

	@GetMapping("/api/mdm/v1/support-accesses")
	@RequirePermission(menu = "SUPPORT_ACCESS", action = "READ")
	public ApiResponse<PageResponse<SupportAccessService.AccessItem>> list(@RequestParam(required = false) String status,
			@RequestParam(required = false) String from, @RequestParam(required = false) String to,
			@RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size,
			@RequestParam(required = false) List<String> sort) {
		return ApiResponse.ok(supportAccessService.tenantList(status, from, to, page, size, sort));
	}

	@GetMapping("/api/mdm/v1/support-accesses/{supportAccessId}/actions")
	@RequirePermission(menu = "SUPPORT_ACCESS", action = "READ")
	public ApiResponse<PageResponse<SupportAccessService.ActionItem>> actions(@PathVariable long supportAccessId,
			@RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size) {
		return ApiResponse.ok(supportAccessService.tenantActions(supportAccessId, page, size));
	}

	private static void requireCsrf(HttpServletRequest request) {
		if (!"1".equals(request.getHeader("X-MDM-Request"))) {
			throw new ApiException(403, "AUTH.CSRF", "요청을 확인할 수 없습니다.", 2);
		}
	}

	private static Jwt currentJwt() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
			throw new ApiException(401, "AUTH.UNAUTHORIZED", "인증이 필요합니다.", 2);
		}
		return jwt;
	}

	public record ExchangeRequest(@NotBlank String ticket) {
	}

}
