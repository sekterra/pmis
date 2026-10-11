package com.tommy.common.i18n.controller;

import java.util.List;
import java.util.Set;

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
import com.tommy.common.i18n.dto.NamespaceRequest;
import com.tommy.common.i18n.dto.NamespaceResponse;
import com.tommy.common.i18n.service.I18nNamespaceService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/mdm/v1/i18n-namespaces")
public class I18nNamespaceController {

	private static final Set<String> SORTS = Set.of("namespaceCode", "namespaceName", "namespacePk");

	private static final List<Sort.Order> DEFAULT_SORT = List.of(Sort.Order.asc("namespaceCode"), Sort.Order.asc("namespacePk"));

	private final I18nNamespaceService namespaces;

	public I18nNamespaceController(I18nNamespaceService namespaces) {
		this.namespaces = namespaces;
	}

	@GetMapping
	@RequirePermission(menu = "I18N", action = "READ")
	public ApiResponse<PageResponse<NamespaceResponse>> list(
			@RequestParam(required = false) String keyword,
			@RequestParam(required = false) String useYn,
			@RequestParam(required = false) Integer page,
			@RequestParam(required = false) Integer size,
			@RequestParam(required = false) List<String> sort) {
		return ApiResponse.ok(namespaces.list(keyword, useYn, PageRequests.parse(page, size, sort, SORTS, DEFAULT_SORT, "namespacePk")));
	}

	@GetMapping("/{namespaceCode}")
	@RequirePermission(menu = "I18N", action = "READ")
	public ApiResponse<NamespaceResponse> get(@PathVariable String namespaceCode) {
		return ApiResponse.ok(namespaces.get(namespaceCode));
	}

	@PostMapping
	@RequirePermission(menu = "I18N", action = "CREATE")
	public ApiResponse<NamespaceResponse> create(@Valid @RequestBody NamespaceRequest request) {
		return ApiResponse.ok(namespaces.create(request));
	}

	@PutMapping("/{namespaceCode}")
	@RequirePermission(menu = "I18N", action = "UPDATE")
	public ApiResponse<NamespaceResponse> update(@PathVariable String namespaceCode, @Valid @RequestBody NamespaceRequest request) {
		return ApiResponse.ok(namespaces.update(namespaceCode, request));
	}

	@DeleteMapping("/{namespaceCode}")
	@RequirePermission(menu = "I18N", action = "DELETE")
	public ApiResponse<Void> deactivate(@PathVariable String namespaceCode) {
		namespaces.deactivate(namespaceCode);
		return ApiResponse.ok(null);
	}

	@PostMapping("/{namespaceCode}/restore")
	@RequirePermission(menu = "I18N", action = "UPDATE")
	public ApiResponse<NamespaceResponse> restore(@PathVariable String namespaceCode) {
		return ApiResponse.ok(namespaces.restore(namespaceCode));
	}

}
