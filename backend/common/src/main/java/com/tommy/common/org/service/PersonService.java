package com.tommy.common.org.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tommy.common.global.response.PageResponse;
import com.tommy.common.global.security.CurrentTenantUser;
import com.tommy.common.org.dto.AssignmentResponse;
import com.tommy.common.org.dto.PersonRequest;
import com.tommy.common.org.dto.PersonResponse;
import com.tommy.common.org.dto.PrimaryAssignmentSummary;
import com.tommy.common.org.entity.Person;
import com.tommy.common.org.entity.PersonAssignment;
import com.tommy.common.org.repository.PersonAssignmentRepository;
import com.tommy.common.org.repository.PersonRepository;
import com.tommy.common.org.support.OrgExceptions;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

@Service
public class PersonService {

	private final PersonRepository people;

	private final PersonAssignmentRepository assignments;

	private final EntityTextService texts;

	private final CurrentTenantUser currentUser;

	private final EntityManager entityManager;

	private final AssignmentPresenter presenter;

	public PersonService(PersonRepository people, PersonAssignmentRepository assignments, EntityTextService texts,
			CurrentTenantUser currentUser, EntityManager entityManager, AssignmentPresenter presenter) {
		this.people = people;
		this.assignments = assignments;
		this.texts = texts;
		this.currentUser = currentUser;
		this.entityManager = entityManager;
		this.presenter = presenter;
	}

	@Transactional(readOnly = true)
	public PageResponse<PersonResponse> list(String keyword, String companyCode, Long deptPk, String employmentTypeCode,
			String assignmentStatusCode, String useYn, Pageable pageable) {
		Page<Person> page = people.findAll(
				filter(keyword, companyCode, deptPk, employmentTypeCode, assignmentStatusCode, useYn), pageable);
		Map<Long, PrimaryAssignmentSummary> summaries = summaries(page.getContent());
		List<PersonResponse> items = page.getContent().stream()
				.map(person -> response(person, false, null, summaries.get(person.getPersonPk())))
				.toList();
		return new PageResponse<>(items, page.getTotalElements(), pageable.getPageNumber() + 1, pageable.getPageSize());
	}

	@Transactional(readOnly = true)
	public PersonResponse get(long personPk) {
		Person person = require(personPk);
		List<AssignmentResponse> current = assignments
				.findByPerson_PersonPkAndEndDateIsNullAndUseYnOrderByPrimaryYnDescAssignmentPkAsc(personPk, "Y").stream()
				.map(presenter::response)
				.toList();
		PrimaryAssignmentSummary primary = current.stream()
				.filter(item -> "Y".equals(item.primaryYn()))
				.findFirst()
				.map(item -> new PrimaryAssignmentSummary(item.companyCode(), item.companyName(), item.deptCode(),
						item.deptName(), item.positionCode(), item.jobTitleCode(), item.employeeNo(),
						item.assignmentStatusCode()))
				.orElse(null);
		return response(person, true, current, primary);
	}

	@Transactional
	public PersonResponse create(PersonRequest request) {
		Person person = Person.create(request.personName().trim(), currentUser.pkOrNull());
		person.change(request.personName().trim(), OrgExceptions.trimToNull(request.email()),
				OrgExceptions.trimToNull(request.mobilePhoneNo()), OrgExceptions.trimToNull(request.officePhoneNo()),
				OrgExceptions.trimToNull(request.description()), currentUser.pkOrNull());
		people.save(person);
		texts.upsert(EntityTextService.PERSON, EntityTextService.PERSON_NAME, person.getPersonPk(), request.translations());
		return response(person, true, List.of(), null);
	}

	@Transactional
	public PersonResponse update(long personPk, PersonRequest request) {
		Person person = require(personPk);
		person.change(request.personName().trim(), OrgExceptions.trimToNull(request.email()),
				OrgExceptions.trimToNull(request.mobilePhoneNo()), OrgExceptions.trimToNull(request.officePhoneNo()),
				OrgExceptions.trimToNull(request.description()), currentUser.pkOrNull());
		texts.upsert(EntityTextService.PERSON, EntityTextService.PERSON_NAME, personPk, request.translations());
		return get(personPk);
	}

	@Transactional
	public void delete(long personPk) {
		Person person = require(personPk);
		if (assignments.existsByPerson_PersonPkAndUseYnAndEndDateIsNull(personPk, "Y")) {
			throw OrgExceptions.of(409, "PERSON.HAS_ACTIVE_ASSIGNMENT", "현재 소속이 있습니다.");
		}
		if (hasActiveAccount(personPk)) {
			throw OrgExceptions.of(409, "PERSON.HAS_ACCOUNT", "사용 중인 계정이 있습니다.");
		}
		person.deactivate(currentUser.pkOrNull());
	}

	@Transactional
	public PersonResponse restore(long personPk) {
		Person person = require(personPk);
		person.activate(currentUser.pkOrNull());
		return get(personPk);
	}

	private boolean hasActiveAccount(long personPk) {
		Number count = (Number) entityManager.createNativeQuery("""
				SELECT count(*) FROM user_account WHERE person_pk = :personPk AND use_yn = 'Y'
				""").setParameter("personPk", personPk).getSingleResult();
		return count.longValue() > 0;
	}

	private Map<Long, PrimaryAssignmentSummary> summaries(List<Person> people) {
		if (people.isEmpty()) {
			return Map.of();
		}
		List<Long> ids = people.stream().map(Person::getPersonPk).toList();
		Map<Long, PrimaryAssignmentSummary> summaries = new HashMap<>();
		for (Object[] row : assignments.findCurrentPrimarySummaries(ids)) {
			summaries.put((Long) row[0], new PrimaryAssignmentSummary((String) row[1], (String) row[2], (String) row[3],
					(String) row[4], (String) row[5], (String) row[6], (String) row[7], (String) row[8]));
		}
		return summaries;
	}

	private Person require(long personPk) {
		return people.findById(personPk).orElseThrow(() -> OrgExceptions.of(404, "PERSON.NOT_FOUND", "사람을 찾을 수 없습니다."));
	}

	private PersonResponse response(Person person, boolean detail, List<AssignmentResponse> current,
			PrimaryAssignmentSummary primary) {
		return new PersonResponse(person.getPersonPk(), person.getPersonName(), person.getEmail(), person.getMobilePhoneNo(),
				person.getOfficePhoneNo(), person.getProfileFilePk(), person.getProfileFilePk() != null, person.getDescription(),
				person.getUseYn(),
				person.getCreateDatetime(), person.getCreateUserPk(), person.getUpdateDatetime(), person.getUpdateUserPk(),
				detail ? texts.list(EntityTextService.PERSON, EntityTextService.PERSON_NAME, person.getPersonPk()) : null,
				primary, detail ? current : null);
	}

	private static Specification<Person> filter(String keyword, String companyCode, Long deptPk, String employmentTypeCode,
			String assignmentStatusCode, String useYn) {
		return (root, query, builder) -> {
			List<Predicate> predicates = new ArrayList<>();
			String text = OrgExceptions.trimToNull(keyword);
			if (text != null) {
				String pattern = "%" + text.toLowerCase() + "%";
				Subquery<Integer> employee = query.subquery(Integer.class);
				Root<PersonAssignment> assignment = employee.from(PersonAssignment.class);
				employee.select(builder.literal(1)).where(
						builder.equal(assignment.get("person"), root),
						builder.isNull(assignment.get("endDate")),
						builder.equal(assignment.get("useYn"), "Y"),
						builder.like(builder.lower(assignment.get("employeeNo")), pattern));
				predicates.add(builder.or(
						builder.like(builder.lower(root.get("personName")), pattern),
						builder.like(builder.lower(root.get("email")), pattern),
						builder.like(builder.lower(root.get("mobilePhoneNo")), pattern),
						builder.exists(employee)));
			}
			if (present(companyCode) || deptPk != null || present(employmentTypeCode) || present(assignmentStatusCode)) {
				Subquery<Integer> primary = query.subquery(Integer.class);
				Root<PersonAssignment> assignment = primary.from(PersonAssignment.class);
				List<Predicate> match = new ArrayList<>();
				match.add(builder.equal(assignment.get("person"), root));
				match.add(builder.equal(assignment.get("primaryYn"), "Y"));
				match.add(builder.isNull(assignment.get("endDate")));
				match.add(builder.equal(assignment.get("useYn"), "Y"));
				if (present(companyCode)) {
					match.add(builder.equal(assignment.get("company").get("companyCode"), companyCode));
				}
				if (deptPk != null) {
					match.add(builder.equal(assignment.get("dept").get("deptPk"), deptPk));
				}
				if (present(employmentTypeCode)) {
					match.add(builder.equal(assignment.get("employmentTypeCode"), employmentTypeCode));
				}
				if (present(assignmentStatusCode)) {
					match.add(builder.equal(assignment.get("assignmentStatusCode"), assignmentStatusCode));
				}
				primary.select(builder.literal(1)).where(match.toArray(Predicate[]::new));
				predicates.add(builder.exists(primary));
			}
			if (present(useYn)) {
				predicates.add(builder.equal(root.get("useYn"), useYn));
			}
			return predicates.isEmpty() ? builder.conjunction() : builder.and(predicates.toArray(Predicate[]::new));
		};
	}

	private static boolean present(String value) {
		return value != null && !value.isBlank();
	}

}
