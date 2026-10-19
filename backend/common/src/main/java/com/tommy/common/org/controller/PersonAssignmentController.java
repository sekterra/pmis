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
import com.tommy.common.org.dto.AssignmentRequest;
import com.tommy.common.org.dto.AssignmentResponse;
import com.tommy.common.org.dto.EndAssignmentRequest;
import com.tommy.common.org.service.PersonAssignmentService;

import jakarta.validation.Valid;

@RestController
public class PersonAssignmentController {

	private static final Set<String> SORTS = Set.of("startDate", "employeeNo", "assignmentPk");

	private static final List<Sort.Order> DEFAULT_SORT = List.of(Sort.Order.desc("startDate"), Sort.Order.desc("assignmentPk"));

	private final PersonAssignmentService assignments;

	public PersonAssignmentController(PersonAssignmentService assignments) {
		this.assignments = assignments;
	}

	@GetMapping("/api/mdm/v1/people/{personPk}/assignments")
	@RequirePermission(menu = "PERSON", action = "READ")
	public ApiResponse<PageResponse<AssignmentResponse>> list(
			@PathVariable long personPk,
			@RequestParam(required = false) String useYn,
			@RequestParam(required = false) Integer page,
			@RequestParam(required = false) Integer size,
			@RequestParam(required = false) List<String> sort) {
		return ApiResponse.ok(assignments.list(personPk, useYn, PageRequests.parse(page, size, sort, SORTS, DEFAULT_SORT, "assignmentPk")));
	}

	@PostMapping("/api/mdm/v1/people/{personPk}/assignments")
	@RequirePermission(menu = "PERSON", action = "CREATE")
	public ApiResponse<AssignmentResponse> create(@PathVariable long personPk, @Valid @RequestBody AssignmentRequest request) {
		return ApiResponse.ok(assignments.create(personPk, request));
	}

	@PutMapping("/api/mdm/v1/person-assignments/{assignmentPk}")
	@RequirePermission(menu = "PERSON", action = "UPDATE")
	public ApiResponse<AssignmentResponse> update(@PathVariable long assignmentPk, @Valid @RequestBody AssignmentRequest request) {
		return ApiResponse.ok(assignments.update(assignmentPk, request));
	}

	@PostMapping("/api/mdm/v1/person-assignments/{assignmentPk}/end")
	@RequirePermission(menu = "PERSON", action = "UPDATE")
	public ApiResponse<AssignmentResponse> end(@PathVariable long assignmentPk,
			@RequestBody(required = false) EndAssignmentRequest request) {
		return ApiResponse.ok(assignments.end(assignmentPk, request));
	}

	@DeleteMapping("/api/mdm/v1/person-assignments/{assignmentPk}")
	@RequirePermission(menu = "PERSON", action = "DELETE")
	public ApiResponse<Void> delete(@PathVariable long assignmentPk) {
		assignments.delete(assignmentPk);
		return ApiResponse.ok(null);
	}

	@PostMapping("/api/mdm/v1/person-assignments/{assignmentPk}/restore")
	@RequirePermission(menu = "PERSON", action = "UPDATE")
	public ApiResponse<AssignmentResponse> restore(@PathVariable long assignmentPk) {
		return ApiResponse.ok(assignments.restore(assignmentPk));
	}

}
