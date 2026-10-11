package com.tommy.common.code.controller;

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

import com.tommy.common.code.dto.CodeGroupRequest;
import com.tommy.common.code.dto.CodeGroupResponse;
import com.tommy.common.code.service.CodeGroupService;
import com.tommy.common.global.paging.PageRequests;
import com.tommy.common.global.response.ApiResponse;
import com.tommy.common.global.response.PageResponse;
import com.tommy.common.global.security.RequirePermission;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/mdm/v1/code-groups")
public class CodeGroupController {

	private static final Set<String> SORTS = Set.of("codeGroupCode", "codeGroupName", "sortOrder", "solutionCode", "codeGroupPk");

	private static final List<Sort.Order> DEFAULT_SORT = List.of(Sort.Order.asc("codeGroupCode"), Sort.Order.asc("codeGroupPk"));

	private final CodeGroupService codeGroupService;

	public CodeGroupController(CodeGroupService codeGroupService) {
		this.codeGroupService = codeGroupService;
	}

	@GetMapping
	@RequirePermission(menu = "COMMON_CODE", action = "READ")
	public ApiResponse<PageResponse<CodeGroupResponse>> list(
			@RequestParam(required = false) String keyword,
			@RequestParam(required = false) String solutionCode,
			@RequestParam(required = false) String useYn,
			@RequestParam(required = false) Integer page,
			@RequestParam(required = false) Integer size,
			@RequestParam(required = false) List<String> sort) {
		return ApiResponse.ok(codeGroupService.list(keyword, solutionCode, useYn,
				PageRequests.parse(page, size, sort, SORTS, DEFAULT_SORT, "codeGroupPk")));
	}

	@GetMapping("/{groupCode}")
	@RequirePermission(menu = "COMMON_CODE", action = "READ")
	public ApiResponse<CodeGroupResponse> get(@PathVariable String groupCode) {
		return ApiResponse.ok(codeGroupService.get(groupCode));
	}

	@PostMapping
	@RequirePermission(menu = "COMMON_CODE", action = "CREATE")
	public ApiResponse<CodeGroupResponse> create(@Valid @RequestBody CodeGroupRequest request) {
		return ApiResponse.ok(codeGroupService.create(request));
	}

	@PutMapping("/{groupCode}")
	@RequirePermission(menu = "COMMON_CODE", action = "UPDATE")
	public ApiResponse<CodeGroupResponse> update(@PathVariable String groupCode, @Valid @RequestBody CodeGroupRequest request) {
		return ApiResponse.ok(codeGroupService.update(groupCode, request));
	}

	@DeleteMapping("/{groupCode}")
	@RequirePermission(menu = "COMMON_CODE", action = "DELETE")
	public ApiResponse<Void> delete(@PathVariable String groupCode) {
		codeGroupService.delete(groupCode);
		return ApiResponse.ok(null);
	}

	@PostMapping("/{groupCode}/restore")
	@RequirePermission(menu = "COMMON_CODE", action = "UPDATE")
	public ApiResponse<CodeGroupResponse> restore(@PathVariable String groupCode) {
		return ApiResponse.ok(codeGroupService.restore(groupCode));
	}

}
