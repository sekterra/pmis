package com.tommy.common.org.controller;

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
import com.tommy.common.org.dto.CompanyRequest;
import com.tommy.common.org.dto.CompanyResponse;
import com.tommy.common.org.dto.CompanyUpdateRequest;
import com.tommy.common.org.service.CompanyService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/mdm/v1/companies")
public class CompanyController {

	private static final Set<String> SORTS = Set.of("companyCode", "companyName", "sortOrder", "companyPk");

	private static final List<Sort.Order> DEFAULT_SORT = List.of(Sort.Order.asc("companyCode"), Sort.Order.asc("companyPk"));

	private final CompanyService companies;

	public CompanyController(CompanyService companies) {
		this.companies = companies;
	}

	@GetMapping
	@RequirePermission(menu = "COMPANY", action = "READ")
	public ApiResponse<PageResponse<CompanyResponse>> list(
			@RequestParam(required = false) String keyword,
			@RequestParam(required = false) String companyTypeCode,
			@RequestParam(required = false) String useYn,
			@RequestParam(required = false) Integer page,
			@RequestParam(required = false) Integer size,
			@RequestParam(required = false) List<String> sort) {
		return ApiResponse.ok(companies.list(keyword, companyTypeCode, useYn,
				PageRequests.parse(page, size, sort, SORTS, DEFAULT_SORT, "companyPk")));
	}

	@GetMapping("/{companyPk}")
	@RequirePermission(menu = "COMPANY", action = "READ")
	public ApiResponse<CompanyResponse> get(@PathVariable long companyPk) {
		return ApiResponse.ok(companies.get(companyPk));
	}

	@PostMapping
	@RequirePermission(menu = "COMPANY", action = "CREATE")
	public ApiResponse<CompanyResponse> create(@Valid @RequestBody CompanyRequest request) {
		return ApiResponse.ok(companies.create(request));
	}

	@PutMapping("/{companyPk}")
	@RequirePermission(menu = "COMPANY", action = "UPDATE")
	public ApiResponse<CompanyResponse> update(@PathVariable long companyPk, @Valid @RequestBody CompanyUpdateRequest request) {
		return ApiResponse.ok(companies.update(companyPk, request));
	}

	@DeleteMapping("/{companyPk}")
	@RequirePermission(menu = "COMPANY", action = "DELETE")
	public ApiResponse<Void> delete(@PathVariable long companyPk) {
		companies.delete(companyPk);
		return ApiResponse.ok(null);
	}

	@PostMapping("/{companyPk}/restore")
	@RequirePermission(menu = "COMPANY", action = "UPDATE")
	public ApiResponse<CompanyResponse> restore(@PathVariable long companyPk) {
		return ApiResponse.ok(companies.restore(companyPk));
	}

}
