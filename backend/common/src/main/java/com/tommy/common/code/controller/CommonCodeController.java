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

import com.tommy.common.code.dto.CodeHistoryResponse;
import com.tommy.common.code.dto.CommonCodeRequest;
import com.tommy.common.code.dto.CommonCodeResponse;
import com.tommy.common.code.service.CommonCodeService;
import com.tommy.common.global.paging.PageRequests;
import com.tommy.common.global.response.ApiResponse;
import com.tommy.common.global.response.PageResponse;
import com.tommy.common.global.security.RequirePermission;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/mdm/v1/common-codes")
public class CommonCodeController {

	private static final Set<String> SORTS = Set.of("codeValue", "codeName", "sortOrder", "codePk");

	private static final List<Sort.Order> DEFAULT_SORT = List.of(
			Sort.Order.asc("sortOrder"), Sort.Order.asc("codeValue"), Sort.Order.asc("codePk"));

	private static final Set<String> HISTORY_SORTS = Set.of("changeDatetime", "historyPk");

	private static final List<Sort.Order> HISTORY_SORT = List.of(Sort.Order.desc("changeDatetime"), Sort.Order.desc("historyPk"));

	private final CommonCodeService commonCodeService;

	public CommonCodeController(CommonCodeService commonCodeService) {
		this.commonCodeService = commonCodeService;
	}

	@GetMapping
	@RequirePermission(menu = "COMMON_CODE", action = "READ")
	public ApiResponse<PageResponse<CommonCodeResponse>> list(
			@RequestParam(required = false) String groupCode,
			@RequestParam(required = false) String keyword,
			@RequestParam(required = false) String useYn,
			@RequestParam(required = false) Integer page,
			@RequestParam(required = false) Integer size,
			@RequestParam(required = false) List<String> sort) {
		return ApiResponse.ok(commonCodeService.list(groupCode, keyword, useYn,
				PageRequests.parse(page, size, sort, SORTS, DEFAULT_SORT, "codePk")));
	}

	@GetMapping("/{codePk}")
	@RequirePermission(menu = "COMMON_CODE", action = "READ")
	public ApiResponse<CommonCodeResponse> get(@PathVariable long codePk) {
		return ApiResponse.ok(commonCodeService.get(codePk));
	}

	@GetMapping("/{codePk}/history")
	@RequirePermission(menu = "COMMON_CODE", action = "READ")
	public ApiResponse<PageResponse<CodeHistoryResponse>> history(
			@PathVariable long codePk,
			@RequestParam(required = false) Integer page,
			@RequestParam(required = false) Integer size,
			@RequestParam(required = false) List<String> sort) {
		return ApiResponse.ok(commonCodeService.history(codePk,
				PageRequests.parse(page, size, sort, HISTORY_SORTS, HISTORY_SORT, "historyPk")));
	}

	@PostMapping
	@RequirePermission(menu = "COMMON_CODE", action = "CREATE")
	public ApiResponse<CommonCodeResponse> create(@Valid @RequestBody CommonCodeRequest request) {
		return ApiResponse.ok(commonCodeService.create(request));
	}

	@PutMapping("/{codePk}")
	@RequirePermission(menu = "COMMON_CODE", action = "UPDATE")
	public ApiResponse<CommonCodeResponse> update(@PathVariable long codePk, @Valid @RequestBody CommonCodeRequest request) {
		return ApiResponse.ok(commonCodeService.update(codePk, request));
	}

	@DeleteMapping("/{codePk}")
	@RequirePermission(menu = "COMMON_CODE", action = "DELETE")
	public ApiResponse<Void> delete(@PathVariable long codePk, @RequestParam(required = false) String changeReason) {
		commonCodeService.delete(codePk, changeReason);
		return ApiResponse.ok(null);
	}

	@PostMapping("/{codePk}/restore")
	@RequirePermission(menu = "COMMON_CODE", action = "UPDATE")
	public ApiResponse<CommonCodeResponse> restore(@PathVariable long codePk,
			@RequestParam(required = false) String changeReason) {
		return ApiResponse.ok(commonCodeService.restore(codePk, changeReason));
	}

}
