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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tommy.common.access.dto.PermissionRequest;
import com.tommy.common.access.dto.PermissionResponse;
import com.tommy.common.access.dto.PermissionUpdateRequest;
import com.tommy.common.access.service.PermissionAdminService;
import com.tommy.common.global.paging.PageRequests;
import com.tommy.common.global.response.ApiResponse;
import com.tommy.common.global.response.PageResponse;
import com.tommy.common.global.security.RequirePermission;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/mdm/v1/permissions")
public class PermissionController {

	private static final Set<String> SORTS = Set.of("permissionCode", "menuCode", "actionCode", "sortOrder", "permissionPk");

	private static final List<Sort.Order> DEFAULT_SORT = List.of(Sort.Order.asc("menuCode"), Sort.Order.asc("sortOrder"),
			Sort.Order.asc("permissionPk"));

	private final PermissionAdminService permissions;

	public PermissionController(PermissionAdminService permissions) {
		this.permissions = permissions;
	}

	@GetMapping
	@RequirePermission(menu = "PERMISSION", action = "READ")
	public ApiResponse<PageResponse<PermissionResponse>> list(
			@RequestParam(required = false) String menuCode,
			@RequestParam(required = false) String actionCode,
			@RequestParam(required = false) String keyword,
			@RequestParam(required = false) String useYn,
			@RequestParam(required = false) Integer page,
			@RequestParam(required = false) Integer size,
			@RequestParam(required = false) List<String> sort) {
		return ApiResponse.ok(permissions.list(menuCode, actionCode, keyword, useYn,
				PageRequests.parse(page, size, sort, SORTS, DEFAULT_SORT, "permissionPk")));
	}

	@GetMapping("/{permissionPk}")
	@RequirePermission(menu = "PERMISSION", action = "READ")
	public ApiResponse<PermissionResponse> get(@PathVariable long permissionPk) {
		return ApiResponse.ok(permissions.get(permissionPk));
	}

	@PostMapping
	@RequirePermission(menu = "PERMISSION", action = "CREATE")
	public ApiResponse<PermissionResponse> create(@Valid @RequestBody PermissionRequest request) {
		return ApiResponse.ok(permissions.create(request));
	}

	@PutMapping("/{permissionPk}")
	@RequirePermission(menu = "PERMISSION", action = "UPDATE")
	public ApiResponse<PermissionResponse> update(@PathVariable long permissionPk,
			@Valid @RequestBody PermissionUpdateRequest request) {
		return ApiResponse.ok(permissions.update(permissionPk, request));
	}

	@DeleteMapping("/{permissionPk}")
	@RequirePermission(menu = "PERMISSION", action = "DELETE")
	public ApiResponse<Void> delete(@PathVariable long permissionPk) {
		permissions.delete(permissionPk);
		return ApiResponse.ok(null);
	}

	@PostMapping("/{permissionPk}/restore")
	@RequirePermission(menu = "PERMISSION", action = "UPDATE")
	public ApiResponse<PermissionResponse> restore(@PathVariable long permissionPk) {
		return ApiResponse.ok(permissions.restore(permissionPk));
	}

}
