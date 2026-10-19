package com.tommy.common.access.controller;

import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Sort;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tommy.common.access.dto.IssuedCredential;
import com.tommy.common.access.dto.RoleGrantRequest;
import com.tommy.common.access.dto.UserCreateRequest;
import com.tommy.common.access.dto.UserResponse;
import com.tommy.common.access.dto.UserRoleItem;
import com.tommy.common.access.dto.UserStatusRequest;
import com.tommy.common.access.dto.UserUpdateRequest;
import com.tommy.common.access.service.UserAccountService;
import com.tommy.common.global.paging.PageRequests;
import com.tommy.common.global.response.ApiResponse;
import com.tommy.common.global.response.PageResponse;
import com.tommy.common.global.security.RequirePermission;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/mdm/v1/users")
public class UserAccountController {

	private static final Set<String> SORTS = Set.of("loginId", "userStatusCode", "lastLoginDatetime", "createDatetime", "userPk");

	private static final List<Sort.Order> DEFAULT_SORT = List.of(Sort.Order.asc("loginId"), Sort.Order.asc("userPk"));

	private final UserAccountService users;

	public UserAccountController(UserAccountService users) {
		this.users = users;
	}

	@GetMapping
	@RequirePermission(menu = "USER", action = "READ")
	public ApiResponse<PageResponse<UserResponse>> list(
			@RequestParam(required = false) String keyword,
			@RequestParam(required = false) String userStatusCode,
			@RequestParam(required = false) String accountTypeCode,
			@RequestParam(required = false) String roleCode,
			@RequestParam(required = false) String useYn,
			@RequestParam(required = false) Integer page,
			@RequestParam(required = false) Integer size,
			@RequestParam(required = false) List<String> sort) {
		return ApiResponse.ok(users.list(keyword, userStatusCode, accountTypeCode, roleCode, useYn,
				PageRequests.parse(page, size, sort, SORTS, DEFAULT_SORT, "userPk")));
	}

	@GetMapping("/{userPk}")
	@RequirePermission(menu = "USER", action = "READ")
	public ApiResponse<UserResponse> get(@PathVariable long userPk) {
		return ApiResponse.ok(users.get(userPk));
	}

	@PostMapping
	@RequirePermission(menu = "USER", action = "CREATE")
	public ResponseEntity<ApiResponse<IssuedCredential>> create(@Valid @RequestBody UserCreateRequest request) {
		return secret(users.create(request));
	}

	@PutMapping("/{userPk}")
	@RequirePermission(menu = "USER", action = "UPDATE")
	public ApiResponse<UserResponse> update(@PathVariable long userPk, @Valid @RequestBody UserUpdateRequest request) {
		return ApiResponse.ok(users.update(userPk, request));
	}

	@PutMapping("/{userPk}/status")
	@RequirePermission(menu = "USER", action = "UPDATE")
	public ApiResponse<UserResponse> status(@PathVariable long userPk, @Valid @RequestBody UserStatusRequest request) {
		return ApiResponse.ok(users.changeStatus(userPk, request));
	}

	@PostMapping("/{userPk}/password/reset")
	@RequirePermission(menu = "USER", action = "UPDATE")
	public ResponseEntity<ApiResponse<IssuedCredential>> reset(@PathVariable long userPk) {
		return secret(users.resetPassword(userPk));
	}

	@DeleteMapping("/{userPk}")
	@RequirePermission(menu = "USER", action = "DELETE")
	public ApiResponse<Void> delete(@PathVariable long userPk) {
		users.delete(userPk);
		return ApiResponse.ok(null);
	}

	@PostMapping("/{userPk}/restore")
	@RequirePermission(menu = "USER", action = "UPDATE")
	public ApiResponse<UserResponse> restore(@PathVariable long userPk) {
		return ApiResponse.ok(users.restore(userPk));
	}

	@GetMapping("/{userPk}/roles")
	@RequirePermission(menu = "ROLE", action = "READ")
	public ApiResponse<List<UserRoleItem>> roles(@PathVariable long userPk, @RequestParam(required = false) String useYn) {
		return ApiResponse.ok(users.roles(userPk, useYn));
	}

	@PutMapping("/{userPk}/roles")
	@RequirePermission(menu = "ROLE", action = "UPDATE")
	public ApiResponse<List<UserRoleItem>> replaceRoles(@PathVariable long userPk,
			@Valid @RequestBody List<RoleGrantRequest> grants) {
		return ApiResponse.ok(users.replaceRoles(userPk, grants));
	}

	private static ResponseEntity<ApiResponse<IssuedCredential>> secret(IssuedCredential credential) {
		return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiResponse.ok(credential));
	}

}
