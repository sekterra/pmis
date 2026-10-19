package com.tommy.common.org.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tommy.common.global.response.PageResponse;
import com.tommy.common.global.security.CurrentTenantUser;
import com.tommy.common.global.time.TenantClock;
import com.tommy.common.org.dto.AssignmentRequest;
import com.tommy.common.org.dto.AssignmentResponse;
import com.tommy.common.org.dto.EndAssignmentRequest;
import com.tommy.common.org.entity.Company;
import com.tommy.common.org.entity.Dept;
import com.tommy.common.org.entity.Person;
import com.tommy.common.org.entity.PersonAssignment;
import com.tommy.common.org.repository.CompanyRepository;
import com.tommy.common.org.repository.DeptRepository;
import com.tommy.common.org.repository.PersonAssignmentRepository;
import com.tommy.common.org.repository.PersonRepository;
import com.tommy.common.org.support.OrgExceptions;
import com.tommy.common.org.support.PrimaryAssignmentPolicy;

import jakarta.persistence.criteria.Predicate;

@Service
public class PersonAssignmentService {

	private final PersonAssignmentRepository assignments;

	private final PersonRepository people;

	private final CompanyRepository companies;

	private final DeptRepository depts;

	private final OrgCodes codes;

	private final CurrentTenantUser currentUser;

	private final TenantClock clock;

	private final AssignmentPresenter presenter;

	public PersonAssignmentService(PersonAssignmentRepository assignments, PersonRepository people, CompanyRepository companies,
			DeptRepository depts, OrgCodes codes, CurrentTenantUser currentUser, TenantClock clock,
			AssignmentPresenter presenter) {
		this.assignments = assignments;
		this.people = people;
		this.companies = companies;
		this.depts = depts;
		this.codes = codes;
		this.currentUser = currentUser;
		this.clock = clock;
		this.presenter = presenter;
	}

	@Transactional(readOnly = true)
	public PageResponse<AssignmentResponse> list(long personPk, String useYn, Pageable pageable) {
		requirePerson(personPk);
		Page<PersonAssignment> page = assignments.findAll(filter(personPk, useYn), pageable);
		return new PageResponse<>(page.getContent().stream().map(presenter::response).toList(), page.getTotalElements(),
				pageable.getPageNumber() + 1, pageable.getPageSize());
	}

	@Transactional
	public AssignmentResponse create(long personPk, AssignmentRequest request) {
		Person person = requirePerson(personPk);
		PersonAssignment assignment = PersonAssignment.create(person, company(request.companyCode()), currentUser.pkOrNull());
		apply(assignment, request, 0L);
		assignments.saveAndFlush(assignment);
		return presenter.response(assignment);
	}

	@Transactional
	public AssignmentResponse update(long assignmentPk, AssignmentRequest request) {
		PersonAssignment assignment = require(assignmentPk);
		apply(assignment, request, assignmentPk);
		return presenter.response(assignment);
	}

	@Transactional
	public AssignmentResponse end(long assignmentPk, EndAssignmentRequest request) {
		PersonAssignment assignment = require(assignmentPk);
		if (!"Y".equals(assignment.getUseYn())) {
			throw OrgExceptions.of(409, "ASSIGNMENT.INACTIVE", "사용 중이 아닌 소속은 종료할 수 없습니다.");
		}
		if (assignment.getEndDate() != null) {
			throw OrgExceptions.of(409, "ASSIGNMENT.ALREADY_ENDED", "이미 종료된 소속입니다.");
		}
		LocalDate endDate = request == null || request.endDate() == null ? clock.today() : request.endDate();
		String status = request == null || request.assignmentStatusCode() == null || request.assignmentStatusCode().isBlank()
				? "RESIGNED"
				: request.assignmentStatusCode();
		codes.require("ASSIGNMENT_STATUS", status, "assignmentStatusCode");
		OrgExceptions.requireDateOrder(assignment.getStartDate(), endDate, "ASSIGNMENT.INVALID_DATE", "시작일은 종료일보다 늦을 수 없습니다.");
		if ("RESIGNED".equals(status) && endDate == null) {
			throw OrgExceptions.field("ASSIGNMENT.RESIGNED_REQUIRES_END", "퇴사 상태에는 종료일이 필요합니다.", "endDate");
		}
		assignment.end(endDate, status, currentUser.pkOrNull());
		return presenter.response(assignment);
	}

	@Transactional
	public void delete(long assignmentPk) {
		require(assignmentPk).deactivate(currentUser.pkOrNull());
	}

	@Transactional
	public AssignmentResponse restore(long assignmentPk) {
		PersonAssignment assignment = require(assignmentPk);
		assignment.activate(currentUser.pkOrNull());
		apply(assignment, new AssignmentRequest(assignment.getCompany().getCompanyCode(),
				assignment.getDept() == null ? null : assignment.getDept().getDeptCode(), assignment.getEmployeeNo(),
				assignment.getEmploymentTypeCode(), assignment.getPositionCode(), assignment.getJobTitleCode(),
				assignment.getAssignmentStatusCode(), assignment.getPrimaryYn(), assignment.getDeptHeadYn(),
				assignment.getStartDate(), assignment.getEndDate(), assignment.getDescription()), assignmentPk);
		return presenter.response(assignment);
	}

	private void apply(PersonAssignment assignment, AssignmentRequest request, long excludePk) {
		Company company = company(request.companyCode());
		Dept dept = dept(company, request.deptCode());
		String employeeNo = OrgExceptions.trimToNull(request.employeeNo());
		String status = request.assignmentStatusCode();
		codes.require("EMPLOYMENT_TYPE", request.employmentTypeCode(), "employmentTypeCode");
		codes.require("ASSIGNMENT_STATUS", status, "assignmentStatusCode");
		codes.require("POSITION", request.positionCode(), "positionCode");
		codes.require("JOB_TITLE", request.jobTitleCode(), "jobTitleCode");
		OrgExceptions.requireDateOrder(request.startDate(), request.endDate(), "ASSIGNMENT.INVALID_DATE", "시작일은 종료일보다 늦을 수 없습니다.");
		if ("RESIGNED".equals(status) && request.endDate() == null) {
			throw OrgExceptions.field("ASSIGNMENT.RESIGNED_REQUIRES_END", "퇴사 상태에는 종료일이 필요합니다.", "endDate");
		}
		if ("Y".equals(request.deptHeadYn()) && dept == null) {
			throw OrgExceptions.field("ASSIGNMENT.DEPT_HEAD_REQUIRES_DEPT", "부서장 여부는 부서가 있을 때만 쓸 수 있습니다.", "deptHeadYn");
		}
		boolean current = request.endDate() == null && "Y".equals(assignment.getUseYn());
		Long deptPk = dept == null ? null : dept.getDeptPk();
		if (current && assignments.countCurrentDuplicate(assignment.getPerson().getPersonPk(), company.getCompanyPk(), deptPk,
				excludePk) > 0) {
			throw OrgExceptions.of(409, "ASSIGNMENT.DUPLICATE", "같은 회사와 부서의 현재 소속이 있습니다.");
		}
		if (employeeNo != null && employeeTaken(company.getCompanyPk(), employeeNo, excludePk)) {
			throw OrgExceptions.of(409, "ASSIGNMENT.EMPLOYEE_NO_DUPLICATE", "이미 있는 사원번호입니다.");
		}
		long others = assignments.countOtherCurrent(assignment.getPerson().getPersonPk(), excludePk);
		boolean primary = PrimaryAssignmentPolicy.becomesPrimary("Y".equals(request.primaryYn()), current, others);
		if (PrimaryAssignmentPolicy.demotesExisting(primary, current, others)) {
			assignments.findCurrentPrimary(assignment.getPerson().getPersonPk())
					.filter(existing -> !existing.getAssignmentPk().equals(assignment.getAssignmentPk()))
					.ifPresent(existing -> existing.clearPrimary(currentUser.pkOrNull()));
			assignments.flush();
		}
		assignment.change(company, dept, employeeNo, request.employmentTypeCode(), OrgExceptions.trimToNull(request.positionCode()),
				OrgExceptions.trimToNull(request.jobTitleCode()), status, primary ? "Y" : "N", OrgExceptions.yn(request.deptHeadYn()),
				request.startDate(), request.endDate(), OrgExceptions.trimToNull(request.description()), currentUser.pkOrNull());
	}

	private boolean employeeTaken(Long companyPk, String employeeNo, long excludePk) {
		if (excludePk == 0L) {
			return assignments.existsByCompany_CompanyPkAndEmployeeNoAndUseYn(companyPk, employeeNo, "Y");
		}
		return assignments.existsByCompany_CompanyPkAndEmployeeNoAndUseYnAndAssignmentPkNot(companyPk, employeeNo, "Y", excludePk);
	}

	private Company company(String companyCode) {
		Company company = companies.findByCompanyCode(companyCode)
				.orElseThrow(() -> OrgExceptions.of(404, "COMPANY.NOT_FOUND", "회사를 찾을 수 없습니다."));
		if (!company.inUse()) {
			throw OrgExceptions.field("ASSIGNMENT.COMPANY_INACTIVE", "사용 중인 회사가 아닙니다.", "companyCode");
		}
		return company;
	}

	private Dept dept(Company company, String deptCode) {
		String code = OrgExceptions.trimToNull(deptCode);
		if (code == null) {
			return null;
		}
		Dept dept = depts.findByCompany_CompanyPkAndDeptCode(company.getCompanyPk(), code).orElse(null);
		if (dept == null) {
			if (depts.countInOtherCompany(code, company.getCompanyPk()) > 0) {
				throw OrgExceptions.of(409, "ASSIGNMENT.DEPT_COMPANY_MISMATCH", "부서가 회사와 맞지 않습니다.");
			}
			throw OrgExceptions.of(404, "DEPT.NOT_FOUND", "부서를 찾을 수 없습니다.");
		}
		if (!dept.inUse()) {
			throw OrgExceptions.field("ASSIGNMENT.DEPT_INACTIVE", "사용 중인 부서가 아닙니다.", "deptCode");
		}
		return dept;
	}

	private Person requirePerson(long personPk) {
		return people.findById(personPk).orElseThrow(() -> OrgExceptions.of(404, "PERSON.NOT_FOUND", "사람을 찾을 수 없습니다."));
	}

	private PersonAssignment require(long assignmentPk) {
		return assignments.findById(assignmentPk)
				.orElseThrow(() -> OrgExceptions.of(404, "ASSIGNMENT.NOT_FOUND", "소속을 찾을 수 없습니다."));
	}

	private static Specification<PersonAssignment> filter(long personPk, String useYn) {
		return (root, query, builder) -> {
			List<Predicate> predicates = new ArrayList<>();
			predicates.add(builder.equal(root.get("person").get("personPk"), personPk));
			if (useYn != null && !useYn.isBlank()) {
				predicates.add(builder.equal(root.get("useYn"), useYn));
			}
			return builder.and(predicates.toArray(Predicate[]::new));
		};
	}

}
