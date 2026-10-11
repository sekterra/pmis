package com.tommy.common.i18n.controller;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tommy.common.global.response.ApiResponse;
import com.tommy.common.global.security.RequirePermission;
import com.tommy.common.i18n.dto.SupportLanguageRequest;
import com.tommy.common.i18n.dto.SupportLanguageResponse;
import com.tommy.common.i18n.service.SupportLanguageService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/mdm/v1/support-languages")
public class SupportLanguageController {

	private final SupportLanguageService languages;

	public SupportLanguageController(SupportLanguageService languages) {
		this.languages = languages;
	}

	@GetMapping
	@RequirePermission(menu = "I18N", action = "READ")
	public ApiResponse<List<SupportLanguageResponse>> list(@RequestParam(required = false) String useYn) {
		return ApiResponse.ok(languages.list(useYn));
	}

	@GetMapping("/{code}")
	@RequirePermission(menu = "I18N", action = "READ")
	public ApiResponse<SupportLanguageResponse> get(@PathVariable String code) {
		return ApiResponse.ok(languages.get(code));
	}

	@PostMapping
	@RequirePermission(menu = "I18N", action = "CREATE")
	public ApiResponse<SupportLanguageResponse> create(@Valid @RequestBody SupportLanguageRequest request) {
		return ApiResponse.ok(languages.create(request));
	}

	@PutMapping("/{code}")
	@RequirePermission(menu = "I18N", action = "UPDATE")
	public ApiResponse<SupportLanguageResponse> update(@PathVariable String code, @Valid @RequestBody SupportLanguageRequest request) {
		return ApiResponse.ok(languages.update(code, request));
	}

	@DeleteMapping("/{code}")
	@RequirePermission(menu = "I18N", action = "DELETE")
	public ApiResponse<Void> deactivate(@PathVariable String code) {
		languages.deactivate(code);
		return ApiResponse.ok(null);
	}

	@PostMapping("/{code}/restore")
	@RequirePermission(menu = "I18N", action = "UPDATE")
	public ApiResponse<SupportLanguageResponse> restore(@PathVariable String code) {
		return ApiResponse.ok(languages.restore(code));
	}

}
