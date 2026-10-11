package com.tommy.common.i18n.controller;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tommy.common.global.paging.PageRequests;
import com.tommy.common.global.response.ApiResponse;
import com.tommy.common.global.response.PageResponse;
import com.tommy.common.global.security.RequirePermission;
import com.tommy.common.i18n.dto.NationItem;
import com.tommy.common.i18n.dto.NationRequest;
import com.tommy.common.i18n.dto.NationResponse;
import com.tommy.common.i18n.service.NationService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/mdm/v1/nations")
public class NationController {

	private static final java.util.Set<String> SORTS = java.util.Set.of("nationCode", "nationName", "sortOrder", "nationPk");

	private static final List<Sort.Order> DEFAULT_SORT = List.of(Sort.Order.asc("nationCode"), Sort.Order.asc("nationPk"));

	private final NationService nations;

	public NationController(NationService nations) {
		this.nations = nations;
	}

	@GetMapping(params = "!managed")
	public ApiResponse<List<NationItem>> listActive() {
		return ApiResponse.ok(nations.listActive());
	}

	@GetMapping(params = "managed")
	@RequirePermission(menu = "I18N", action = "READ")
	public ApiResponse<PageResponse<NationResponse>> list(
			@RequestParam(required = false) Integer page,
			@RequestParam(required = false) Integer size,
			@RequestParam(required = false) List<String> sort) {
		return ApiResponse.ok(nations.list(PageRequests.parse(page, size, sort, SORTS, DEFAULT_SORT, "nationPk")));
	}

	@GetMapping("/{nationCode}")
	@RequirePermission(menu = "I18N", action = "READ")
	public ApiResponse<NationResponse> get(@PathVariable String nationCode) {
		return ApiResponse.ok(nations.get(nationCode));
	}

	@PostMapping
	@RequirePermission(menu = "I18N", action = "CREATE")
	public ApiResponse<NationResponse> create(@Valid @RequestBody NationRequest request) {
		return ApiResponse.ok(nations.create(request));
	}

	@PutMapping("/{nationCode}")
	@RequirePermission(menu = "I18N", action = "UPDATE")
	public ApiResponse<NationResponse> update(@PathVariable String nationCode, @Valid @RequestBody NationRequest request) {
		return ApiResponse.ok(nations.update(nationCode, request));
	}

	@DeleteMapping("/{nationCode}")
	@RequirePermission(menu = "I18N", action = "DELETE")
	public ApiResponse<Void> deactivate(@PathVariable String nationCode) {
		nations.deactivate(nationCode);
		return ApiResponse.ok(null);
	}

	@PostMapping("/{nationCode}/restore")
	@RequirePermission(menu = "I18N", action = "UPDATE")
	public ApiResponse<NationResponse> restore(@PathVariable String nationCode) {
		return ApiResponse.ok(nations.restore(nationCode));
	}

}
