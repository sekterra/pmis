package com.tommy.common.access.controller;

import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tommy.common.access.dto.PermissionCodesRequest;
import com.tommy.common.access.dto.PermissionGrantItem;
import com.tommy.common.access.dto.RoleRequest;
import com.tommy.common.access.dto.RoleResponse;
import com.tommy.common.access.dto.RoleUpdateRequest;
import com.tommy.common.access.service.RoleAdminService;
import com.tommy.common.global.paging.PageRequests;
import com.tommy.common.global.response.ApiResponse;
import com.tommy.common.global.response.PageResponse;
import com.tommy.common.global.security.RequirePermission;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/mdm/v1/roles")
public class RoleController {

	private static final Set<String> SORTS = Set.of("roleCode", "roleName", "sortOrder", "rolePk");

	private static final List<Sort.Order> DEFAULT_SORT = List.of(Sort.Order.asc("sortOrder"), Sort.Order.asc("rolePk"));

	private final RoleAdminService roles;

	public RoleController(RoleAdminService roles) {
		this.roles = roles;
	}

	@GetMapping
	@RequirePermission(menu = "ROLE", action = "READ")
	public ApiResponse<PageResponse<RoleResponse>> list(
			@RequestParam(required = false) String keyword,
			@RequestParam(required = false) String useYn,
			@RequestParam(required = false) Integer page,
			@RequestParam(required = false) Integer size,
			@RequestParam(required = false) List<String> sort) {
		return ApiResponse.ok(roles.list(keyword, useYn, PageRequests.parse(page, size, sort, SORTS, DEFAULT_SORT, "rolePk")));
	}

	@GetMapping("/{rolePk}")
	@RequirePermission(menu = "ROLE", action = "READ")
	public ApiResponse<RoleResponse> get(@PathVariable long rolePk) {
		return ApiResponse.ok(roles.get(rolePk));
	}

	@PostMapping
	@RequirePermission(menu = "ROLE", action = "CREATE")
	public ApiResponse<RoleResponse> create(@Valid @RequestBody RoleRequest request) {
		return ApiResponse.ok(roles.create(request));
	}

	@PutMapping("/{rolePk}")
	@RequirePermission(menu = "ROLE", action = "UPDATE")
	public ApiResponse<RoleResponse> update(@PathVariable long rolePk, @Valid @RequestBody RoleUpdateRequest request) {
		return ApiResponse.ok(roles.update(rolePk, request));
	}

	@DeleteMapping("/{rolePk}")
	@RequirePermission(menu = "ROLE", action = "DELETE")
	public ApiResponse<Void> delete(@PathVariable long rolePk) {
		roles.delete(rolePk);
		return ApiResponse.ok(null);
	}

	@PostMapping("/{rolePk}/restore")
	@RequirePermission(menu = "ROLE", action = "UPDATE")
	public ApiResponse<RoleResponse> restore(@PathVariable long rolePk) {
		return ApiResponse.ok(roles.restore(rolePk));
	}

	@GetMapping("/{rolePk}/permissions")
	@RequirePermission(menu = "ROLE", action = "READ")
	public ApiResponse<List<PermissionGrantItem>> permissions(@PathVariable long rolePk,
			@RequestParam(required = false) Boolean granted,
			@RequestParam(required = false) String lang,
			@RequestHeader(value = "Accept-Language", required = false) String acceptLanguage) {
		return ApiResponse.ok(roles.permissions(rolePk, granted, lang, acceptLanguage));
	}

	@PutMapping("/{rolePk}/permissions")
	@RequirePermission(menu = "ROLE", action = "UPDATE")
	public ApiResponse<List<PermissionGrantItem>> replacePermissions(@PathVariable long rolePk,
			@RequestBody PermissionCodesRequest request,
			@RequestParam(required = false) String lang,
			@RequestHeader(value = "Accept-Language", required = false) String acceptLanguage) {
		return ApiResponse.ok(roles.replacePermissions(rolePk, request, lang, acceptLanguage));
	}

}
