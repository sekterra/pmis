package com.tommy.common.platform;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tommy.common.global.response.ApiResponse;
import com.tommy.common.global.response.PageResponse;
import com.tommy.common.tenant.provision.TenantUpgradeService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

@RestController
@RequestMapping("/api/platform/v1/tenants")
public class PlatformTenantController {

	private final PlatformTenantService tenantService;

	public PlatformTenantController(PlatformTenantService tenantService) {
		this.tenantService = tenantService;
	}

	@GetMapping
	public ApiResponse<PageResponse<PlatformTenantService.TenantSummary>> list(
			@RequestParam(required = false) String status,
			@RequestParam(required = false) String keyword,
			@RequestParam(required = false) Integer page,
			@RequestParam(required = false) Integer size,
			@RequestParam(required = false) List<String> sort,
			HttpServletRequest request) {
		return ApiResponse.ok(tenantService.list(status, keyword, page, size, sort, request));
	}

	@GetMapping("/{tenantCode}")
	public ApiResponse<PlatformTenantService.TenantDetail> get(@PathVariable String tenantCode, HttpServletRequest request) {
		return ApiResponse.ok(tenantService.get(tenantCode, request));
	}

	@PostMapping
	public ResponseEntity<ApiResponse<PlatformTenantService.CreatedTenant>> create(@Valid @RequestBody CreateRequest body, HttpServletRequest request) {
		PlatformTenantService.CreatedTenant created = tenantService.create(new PlatformTenantService.CreateTenant(body.tenantCode(),
				body.tenantName(), body.defaultLanguageCode(), body.timezoneId(), body.contractStartDate(), body.contractEndDate(),
				body.adminName(), body.adminEmail(), body.domains()), PlatformAuthController.currentAdminPk(), request);
		return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiResponse.ok(created));
	}

	@PostMapping("/upgrade")
	public ApiResponse<TenantUpgradeService.UpgradeBatchResult> upgradeAll() {
		return ApiResponse.ok(tenantService.upgradeAll(PlatformAuthController.currentAdminPk()));
	}

	@PostMapping("/{tenantCode}/provisioning/retry")
	public ResponseEntity<ApiResponse<PlatformTenantService.CreatedTenant>> retry(@PathVariable String tenantCode, HttpServletRequest request) {
		return ResponseEntity.ok().cacheControl(CacheControl.noStore())
				.body(ApiResponse.ok(tenantService.retry(tenantCode, PlatformAuthController.currentAdminPk(), request)));
	}

	@PostMapping("/{tenantCode}/admin-password/reissue")
	public ResponseEntity<ApiResponse<PlatformTenantService.ReissuedPasswordView>> reissue(@PathVariable String tenantCode,
			HttpServletRequest request) {
		return ResponseEntity.ok().cacheControl(CacheControl.noStore())
				.body(ApiResponse.ok(tenantService.reissue(tenantCode, PlatformAuthController.currentAdminPk(), request)));
	}

	@PostMapping("/{tenantCode}/suspend")
	public ApiResponse<Void> suspend(@PathVariable String tenantCode, @Valid @RequestBody ReasonRequest body, HttpServletRequest request) {
		tenantService.changeStatus(tenantCode, "SUSPENDED", body.changeReason(), PlatformAuthController.currentAdminPk(), request);
		return ApiResponse.ok(null);
	}

	@PostMapping("/{tenantCode}/resume")
	public ApiResponse<Void> resume(@PathVariable String tenantCode, @Valid @RequestBody ReasonRequest body, HttpServletRequest request) {
		tenantService.changeStatus(tenantCode, "ACTIVE", body.changeReason(), PlatformAuthController.currentAdminPk(), request);
		return ApiResponse.ok(null);
	}

	@GetMapping("/{tenantCode}/domains")
	public ApiResponse<List<PlatformTenantService.DomainView>> domains(@PathVariable String tenantCode, HttpServletRequest request) {
		return ApiResponse.ok(tenantService.domains(tenantCode, request));
	}

	@PostMapping("/{tenantCode}/domains")
	public ApiResponse<PlatformTenantService.DomainView> addDomain(@PathVariable String tenantCode, @Valid @RequestBody DomainRequest body,
			HttpServletRequest request) {
		return ApiResponse.ok(tenantService.addDomain(tenantCode, body.domainName(), PlatformAuthController.currentAdminPk(), request));
	}

	@DeleteMapping("/{tenantCode}/domains/{domainName:.+}")
	public ApiResponse<Void> deleteDomain(@PathVariable String tenantCode, @PathVariable String domainName, HttpServletRequest request) {
		tenantService.deleteDomain(tenantCode, domainName, PlatformAuthController.currentAdminPk(), request);
		return ApiResponse.ok(null);
	}

	@PostMapping("/{tenantCode}/upgrade")
	public ApiResponse<TenantUpgradeService.UpgradeTenantResult> upgrade(@PathVariable String tenantCode, HttpServletRequest request) {
		return ApiResponse.ok(tenantService.upgrade(tenantCode, PlatformAuthController.currentAdminPk(), request));
	}

	@GetMapping("/{tenantCode}/migrations")
	public ApiResponse<PageResponse<PlatformTenantService.MigrationView>> migrations(
			@PathVariable String tenantCode,
			@RequestParam(required = false) Integer page,
			@RequestParam(required = false) Integer size,
			@RequestParam(required = false) List<String> sort,
			HttpServletRequest request) {
		return ApiResponse.ok(tenantService.migrations(tenantCode, page, size, sort, request));
	}

	public record CreateRequest(
			@NotBlank String tenantCode,
			@NotBlank String tenantName,
			@NotBlank String defaultLanguageCode,
			String timezoneId,
			LocalDate contractStartDate,
			LocalDate contractEndDate,
			@NotBlank String adminName,
			@NotBlank String adminEmail,
			@NotEmpty List<String> domains) {
	}

	public record ReasonRequest(@NotBlank String changeReason) {
	}

	public record DomainRequest(@NotBlank String domainName) {
	}

}
