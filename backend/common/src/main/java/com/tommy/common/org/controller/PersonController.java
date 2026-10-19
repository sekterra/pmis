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
import com.tommy.common.org.dto.PersonRequest;
import com.tommy.common.org.dto.PersonResponse;
import com.tommy.common.org.service.PersonService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/mdm/v1/people")
public class PersonController {

	private static final Set<String> SORTS = Set.of("personName", "email", "personPk");

	private static final List<Sort.Order> DEFAULT_SORT = List.of(Sort.Order.asc("personName"), Sort.Order.asc("personPk"));

	private final PersonService people;

	public PersonController(PersonService people) {
		this.people = people;
	}

	@GetMapping
	@RequirePermission(menu = "PERSON", action = "READ")
	public ApiResponse<PageResponse<PersonResponse>> list(
			@RequestParam(required = false) String keyword,
			@RequestParam(required = false) String companyCode,
			@RequestParam(required = false) Long deptPk,
			@RequestParam(required = false) String employmentTypeCode,
			@RequestParam(required = false) String assignmentStatusCode,
			@RequestParam(required = false) String useYn,
			@RequestParam(required = false) Integer page,
			@RequestParam(required = false) Integer size,
			@RequestParam(required = false) List<String> sort) {
		return ApiResponse.ok(people.list(keyword, companyCode, deptPk, employmentTypeCode, assignmentStatusCode, useYn,
				PageRequests.parse(page, size, sort, SORTS, DEFAULT_SORT, "personPk")));
	}

	@GetMapping("/{personPk}")
	@RequirePermission(menu = "PERSON", action = "READ")
	public ApiResponse<PersonResponse> get(@PathVariable long personPk) {
		return ApiResponse.ok(people.get(personPk));
	}

	@PostMapping
	@RequirePermission(menu = "PERSON", action = "CREATE")
	public ApiResponse<PersonResponse> create(@Valid @RequestBody PersonRequest request) {
		return ApiResponse.ok(people.create(request));
	}

	@PutMapping("/{personPk}")
	@RequirePermission(menu = "PERSON", action = "UPDATE")
	public ApiResponse<PersonResponse> update(@PathVariable long personPk, @Valid @RequestBody PersonRequest request) {
		return ApiResponse.ok(people.update(personPk, request));
	}

	@DeleteMapping("/{personPk}")
	@RequirePermission(menu = "PERSON", action = "DELETE")
	public ApiResponse<Void> delete(@PathVariable long personPk) {
		people.delete(personPk);
		return ApiResponse.ok(null);
	}

	@PostMapping("/{personPk}/restore")
	@RequirePermission(menu = "PERSON", action = "UPDATE")
	public ApiResponse<PersonResponse> restore(@PathVariable long personPk) {
		return ApiResponse.ok(people.restore(personPk));
	}

}
