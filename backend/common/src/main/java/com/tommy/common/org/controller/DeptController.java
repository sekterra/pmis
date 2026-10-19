package com.tommy.common.org.controller;

import java.time.LocalDate;
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
import com.tommy.common.org.dto.DeptCloseRequest;
import com.tommy.common.org.dto.DeptHistoryResponse;
import com.tommy.common.org.dto.DeptRequest;
import com.tommy.common.org.dto.DeptResponse;
import com.tommy.common.org.dto.DeptUpdateRequest;
import com.tommy.common.org.service.DeptService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/mdm/v1/departments")
public class DeptController {

	private static final Set<String> SORTS = Set.of("deptCode", "deptName", "levelNo", "sortOrder", "deptPk");

	private static final List<Sort.Order> DEFAULT_SORT = List.of(Sort.Order.asc("deptCode"), Sort.Order.asc("deptPk"));

	private static final Set<String> HISTORY_SORTS = Set.of("changeDatetime", "effectiveDate", "historyPk");

	private static final List<Sort.Order> HISTORY_SORT = List.of(Sort.Order.desc("changeDatetime"), Sort.Order.desc("historyPk"));

	private final DeptService depts;

	public DeptController(DeptService depts) {
		this.depts = depts;
	}

	@GetMapping
	@RequirePermission(menu = "DEPT", action = "READ")
	public ApiResponse<PageResponse<DeptResponse>> list(
			@RequestParam(required = false) String companyCode,
			@RequestParam(required = false) String keyword,
			@RequestParam(required = false) String deptTypeCode,
			@RequestParam(required = false) String useYn,
			@RequestParam(required = false) Long parentDeptPk,
			@RequestParam(required = false) Integer page,
			@RequestParam(required = false) Integer size,
			@RequestParam(required = false) List<String> sort) {
		return ApiResponse.ok(depts.list(companyCode, keyword, deptTypeCode, useYn, parentDeptPk,
				PageRequests.parse(page, size, sort, SORTS, DEFAULT_SORT, "deptPk")));
	}

	@GetMapping("/tree")
	@RequirePermission(menu = "DEPT", action = "READ")
	public ApiResponse<List<DeptResponse>> tree(
			@RequestParam(required = false) String companyCode,
			@RequestParam(required = false) LocalDate asOfDate,
			@RequestParam(required = false, defaultValue = "false") boolean includeClosed) {
		return ApiResponse.ok(depts.tree(companyCode, asOfDate, includeClosed));
	}

	@GetMapping("/{deptPk}")
	@RequirePermission(menu = "DEPT", action = "READ")
	public ApiResponse<DeptResponse> get(@PathVariable long deptPk) {
		return ApiResponse.ok(depts.get(deptPk));
	}

	@GetMapping("/{deptPk}/history")
	@RequirePermission(menu = "DEPT", action = "READ")
	public ApiResponse<PageResponse<DeptHistoryResponse>> history(
			@PathVariable long deptPk,
			@RequestParam(required = false) Integer page,
			@RequestParam(required = false) Integer size,
			@RequestParam(required = false) List<String> sort) {
		return ApiResponse.ok(depts.history(deptPk, PageRequests.parse(page, size, sort, HISTORY_SORTS, HISTORY_SORT, "historyPk")));
	}

	@PostMapping
	@RequirePermission(menu = "DEPT", action = "CREATE")
	public ApiResponse<DeptResponse> create(@Valid @RequestBody DeptRequest request) {
		return ApiResponse.ok(depts.create(request));
	}

	@PutMapping("/{deptPk}")
	@RequirePermission(menu = "DEPT", action = "UPDATE")
	public ApiResponse<DeptResponse> update(@PathVariable long deptPk, @Valid @RequestBody DeptUpdateRequest request) {
		return ApiResponse.ok(depts.update(deptPk, request));
	}

	@DeleteMapping("/{deptPk}")
	@RequirePermission(menu = "DEPT", action = "DELETE")
	public ApiResponse<Void> delete(@PathVariable long deptPk, @RequestBody(required = false) DeptCloseRequest request) {
		depts.delete(deptPk, request);
		return ApiResponse.ok(null);
	}

	@PostMapping("/{deptPk}/restore")
	@RequirePermission(menu = "DEPT", action = "UPDATE")
	public ApiResponse<DeptResponse> restore(@PathVariable long deptPk) {
		return ApiResponse.ok(depts.restore(deptPk));
	}

}
