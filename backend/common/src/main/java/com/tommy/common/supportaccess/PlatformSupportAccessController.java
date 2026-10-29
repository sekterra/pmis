package com.tommy.common.supportaccess;

import java.util.List;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tommy.common.global.response.ApiResponse;
import com.tommy.common.global.response.PageResponse;

import jakarta.servlet.http.HttpServletRequest;

@RestController
public class PlatformSupportAccessController {

	private final SupportAccessService supportAccessService;

	public PlatformSupportAccessController(SupportAccessService supportAccessService) {
		this.supportAccessService = supportAccessService;
	}

	@PostMapping("/api/platform/v1/tenants/{tenantCode}/support-access")
	public ResponseEntity<ApiResponse<SupportAccessService.Issued>> issue(@PathVariable String tenantCode,
			@RequestBody(required = false) ReasonRequest body, HttpServletRequest request) {
		String reason = body == null ? null : body.reason();
		return ResponseEntity.ok().cacheControl(CacheControl.noStore())
				.body(ApiResponse.ok(supportAccessService.issue(tenantCode, reason, request)));
	}

	@GetMapping("/api/platform/v1/support-accesses")
	public ApiResponse<PageResponse<SupportAccessService.AccessItem>> list(@RequestParam(required = false) String tenantCode,
			@RequestParam(required = false) String platformLoginId, @RequestParam(required = false) String status,
			@RequestParam(required = false) String from, @RequestParam(required = false) String to,
			@RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size,
			@RequestParam(required = false) List<String> sort) {
		return ApiResponse.ok(supportAccessService.platformList(tenantCode, platformLoginId, status, from, to, page, size, sort));
	}

	@GetMapping("/api/platform/v1/support-accesses/{supportAccessId}/actions")
	public ApiResponse<PageResponse<SupportAccessService.ActionItem>> actions(@PathVariable long supportAccessId,
			@RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size) {
		return ApiResponse.ok(supportAccessService.platformActions(supportAccessId, page, size));
	}

	public record ReasonRequest(String reason) {
	}

}
