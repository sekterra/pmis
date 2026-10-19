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
import com.tommy.common.org.dto.DeptCloseRequest;
import com.tommy.common.org.dto.DeptHistoryResponse;
import com.tommy.common.org.dto.DeptRequest;
import com.tommy.common.org.dto.DeptResponse;
import com.tommy.common.org.dto.DeptUpdateRequest;
import com.tommy.common.org.entity.Company;
import com.tommy.common.org.entity.Dept;
import com.tommy.common.org.entity.DeptHistory;
import com.tommy.common.org.repository.CompanyRepository;
import com.tommy.common.org.repository.DeptHistoryRepository;
import com.tommy.common.org.repository.DeptRepository;
import com.tommy.common.org.repository.PersonAssignmentRepository;
import com.tommy.common.org.support.DeptPaths;
import com.tommy.common.org.support.DeptTreeOrder;
import com.tommy.common.org.support.OrgExceptions;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.Predicate;

@Service
public class DeptService {

	private final DeptRepository depts;

	private final DeptHistoryRepository histories;

	private final CompanyRepository companies;

	private final PersonAssignmentRepository assignments;

	private final EntityTextService texts;

	private final OrgCodes codes;

	private final CurrentTenantUser currentUser;

	private final TenantClock clock;

	private final EntityManager entityManager;

	public DeptService(DeptRepository depts, DeptHistoryRepository histories, CompanyRepository companies,
			PersonAssignmentRepository assignments, EntityTextService texts, OrgCodes codes, CurrentTenantUser currentUser,
			TenantClock clock, EntityManager entityManager) {
		this.depts = depts;
		this.histories = histories;
		this.companies = companies;
		this.assignments = assignments;
		this.texts = texts;
		this.codes = codes;
		this.currentUser = currentUser;
		this.clock = clock;
		this.entityManager = entityManager;
	}

	@Transactional(readOnly = true)
	public PageResponse<DeptResponse> list(String companyCode, String keyword, String deptTypeCode, String useYn,
			Long parentDeptPk, Pageable pageable) {
		Page<Dept> page = depts.findAll(filter(companyCode, keyword, deptTypeCode, useYn, parentDeptPk), pageable);
		return new PageResponse<>(page.getContent().stream().map(dept -> response(dept, false)).toList(),
				page.getTotalElements(), pageable.getPageNumber() + 1, pageable.getPageSize());
	}

	@Transactional(readOnly = true)
	public List<DeptResponse> tree(String companyCode, LocalDate asOfDate, boolean includeClosed) {
		String code = OrgExceptions.trimToNull(companyCode);
		if (code == null) {
			throw OrgExceptions.field("COMMON.VALIDATION_FAILED", "회사 코드가 필요합니다.", "companyCode");
		}
		Company company = companies.findByCompanyCode(code)
				.orElseThrow(() -> OrgExceptions.of(404, "COMPANY.NOT_FOUND", "회사를 찾을 수 없습니다."));
		LocalDate asOf = asOfDate == null ? clock.today() : asOfDate;
		boolean closed = asOfDate != null || includeClosed;
		List<Dept> rows = depts.findTree(company.getCompanyPk(), asOf, closed);
		List<DeptTreeOrder.Node> nodes = rows.stream()
				.map(dept -> new DeptTreeOrder.Node(dept.getDeptPk(),
						dept.getParentDept() == null ? null : dept.getParentDept().getDeptPk(), dept.getDeptCode(),
						dept.getSortOrder()))
				.toList();
		List<Long> order = DeptTreeOrder.preorder(nodes).stream().map(DeptTreeOrder.Node::deptPk).toList();
		List<DeptResponse> result = new ArrayList<>();
		for (Long deptPk : order) {
			rows.stream().filter(dept -> dept.getDeptPk().equals(deptPk)).findFirst()
					.ifPresent(dept -> result.add(response(dept, false)));
		}
		return result;
	}

	@Transactional(readOnly = true)
	public DeptResponse get(long deptPk) {
		return response(require(deptPk), true);
	}

	@Transactional(readOnly = true)
	public PageResponse<DeptHistoryResponse> history(long deptPk, Pageable pageable) {
		require(deptPk);
		Page<DeptHistory> page = histories.findAll((root, query, builder) -> builder.equal(root.get("dept").get("deptPk"), deptPk),
				pageable);
		return new PageResponse<>(page.getContent().stream().map(DeptService::history).toList(), page.getTotalElements(),
				pageable.getPageNumber() + 1, pageable.getPageSize());
	}

	@Transactional
	public DeptResponse create(DeptRequest request) {
		Company company = activeCompany(request.companyCode());
		if (depts.existsByCompany_CompanyPkAndDeptCode(company.getCompanyPk(), request.deptCode())) {
			throw OrgExceptions.of(409, "DEPT.DUPLICATE", "이미 있는 부서 코드입니다.");
		}
		codes.require("DEPT_TYPE", request.deptTypeCode(), "deptTypeCode");
		LocalDate from = request.validFromDate() == null ? clock.today() : request.validFromDate();
		OrgExceptions.requireDateOrder(from, request.validToDate(), "DEPT.INVALID_DATE", "시작일은 종료일보다 늦을 수 없습니다.");
		Dept parent = parent(company, request.parentDeptCode(), null);
		Long userPk = currentUser.pkOrNull();
		Dept dept = Dept.create(company, request.deptCode(), request.deptName().trim(), userPk);
		dept.assignParent(parent);
		dept.change(request.deptName().trim(), request.deptTypeCode(), request.sortOrder(), from, request.validToDate(),
				OrgExceptions.trimToNull(request.description()), userPk);
		depts.saveAndFlush(dept);
		String path = DeptPaths.path(parent == null ? null : parent.getDeptPath(), dept.getDeptPk());
		dept.locate(DeptPaths.level(path), path);
		histories.save(DeptHistory.record(dept, "CREATE", from, null, dept.getDeptName(), null,
				parent == null ? null : parent.getDeptPk(), null, userPk));
		texts.upsert(EntityTextService.DEPT, EntityTextService.DEPT_NAME, dept.getDeptPk(), request.translations());
		return response(dept, true);
	}

	@Transactional
	public DeptResponse update(long deptPk, DeptUpdateRequest request) {
		Dept dept = require(deptPk);
		codes.require("DEPT_TYPE", request.deptTypeCode(), "deptTypeCode");
		LocalDate from = request.validFromDate() == null ? dept.getValidFromDate() : request.validFromDate();
		OrgExceptions.requireDateOrder(from, request.validToDate(), "DEPT.INVALID_DATE", "시작일은 종료일보다 늦을 수 없습니다.");
		String beforeName = dept.getDeptName();
		Long beforeParentPk = dept.getParentDept() == null ? null : dept.getParentDept().getDeptPk();
		String oldPath = dept.getDeptPath();
		int oldLevel = dept.getLevelNo() == null ? DeptPaths.level(oldPath) : dept.getLevelNo();
		Dept parent = parent(dept.getCompany(), request.parentDeptCode(), dept);
		Long afterParentPk = parent == null ? null : parent.getDeptPk();
		boolean renamed = !beforeName.equals(request.deptName().trim());
		boolean moved = !java.util.Objects.equals(beforeParentPk, afterParentPk);
		Long userPk = currentUser.pkOrNull();
		dept.assignParent(parent);
		dept.change(request.deptName().trim(), request.deptTypeCode(), request.sortOrder(), from, request.validToDate(),
				OrgExceptions.trimToNull(request.description()), userPk);
		if (moved) {
			depts.saveAndFlush(dept);
			String newPath = DeptPaths.path(parent == null ? null : parent.getDeptPath(), dept.getDeptPk());
			relocate(oldPath, newPath, DeptPaths.level(newPath) - oldLevel);
			entityManager.clear();
			dept = require(deptPk);
		}
		LocalDate effective = request.effectiveDate() == null ? clock.today() : request.effectiveDate();
		String reason = OrgExceptions.trimToNull(request.changeReason());
		if (renamed) {
			histories.save(DeptHistory.record(dept, "RENAME", effective, beforeName, dept.getDeptName(), beforeParentPk,
					beforeParentPk, reason, userPk));
		}
		if (moved) {
			histories.save(DeptHistory.record(dept, "MOVE", effective, dept.getDeptName(), dept.getDeptName(), beforeParentPk,
					afterParentPk, reason, userPk));
		}
		texts.upsert(EntityTextService.DEPT, EntityTextService.DEPT_NAME, deptPk, request.translations());
		return response(dept, true);
	}

	@Transactional
	public void delete(long deptPk, DeptCloseRequest request) {
		Dept dept = require(deptPk);
		if (depts.existsByParentDept_DeptPkAndUseYn(deptPk, "Y")) {
			throw OrgExceptions.of(409, "DEPT.HAS_CHILD", "사용 중인 하위 부서가 있습니다.");
		}
		if (assignments.existsByDept_DeptPkAndUseYnAndEndDateIsNull(deptPk, "Y")) {
			throw OrgExceptions.of(409, "DEPT.HAS_ACTIVE_ASSIGNMENT", "현재 소속이 있습니다.");
		}
		LocalDate effective = request == null || request.effectiveDate() == null ? clock.today() : request.effectiveDate();
		OrgExceptions.requireDateOrder(dept.getValidFromDate(), effective, "DEPT.INVALID_DATE", "시작일은 종료일보다 늦을 수 없습니다.");
		Long userPk = currentUser.pkOrNull();
		Long parentPk = dept.getParentDept() == null ? null : dept.getParentDept().getDeptPk();
		dept.close(effective, userPk);
		histories.save(DeptHistory.record(dept, "CLOSE", effective, dept.getDeptName(), dept.getDeptName(), parentPk, parentPk,
				request == null ? null : OrgExceptions.trimToNull(request.changeReason()), userPk));
	}

	@Transactional
	public DeptResponse restore(long deptPk) {
		Dept dept = require(deptPk);
		if (dept.getParentDept() != null && !dept.getParentDept().inUse()) {
			throw OrgExceptions.of(409, "DEPT.PARENT_INACTIVE", "상위 부서가 사용 중이 아닙니다.");
		}
		Long userPk = currentUser.pkOrNull();
		Long parentPk = dept.getParentDept() == null ? null : dept.getParentDept().getDeptPk();
		dept.reopen(userPk);
		histories.save(DeptHistory.record(dept, "REOPEN", clock.today(), dept.getDeptName(), dept.getDeptName(), parentPk,
				parentPk, null, userPk));
		return response(dept, true);
	}

	private void relocate(String oldPath, String newPath, int delta) {
		entityManager.createNativeQuery("""
				UPDATE dept
				SET dept_path = :newPrefix || substring(dept_path from :start),
				    level_no = coalesce(level_no, 0) + :delta
				WHERE dept_path = :oldPrefix OR dept_path LIKE :likePrefix
				""")
				.setParameter("newPrefix", newPath)
				.setParameter("start", oldPath.length() + 1)
				.setParameter("delta", delta)
				.setParameter("oldPrefix", oldPath)
				.setParameter("likePrefix", oldPath + "/%")
				.executeUpdate();
	}

	private Dept parent(Company company, String parentDeptCode, Dept self) {
		String code = OrgExceptions.trimToNull(parentDeptCode);
		if (code == null) {
			return null;
		}
		Dept parent = depts.findByCompany_CompanyPkAndDeptCode(company.getCompanyPk(), code).orElse(null);
		if (parent == null) {
			if (depts.countInOtherCompany(code, company.getCompanyPk()) > 0) {
				throw OrgExceptions.of(409, "DEPT.PARENT_COMPANY_MISMATCH", "다른 회사의 부서를 상위로 둘 수 없습니다.");
			}
			throw OrgExceptions.of(404, "DEPT.NOT_FOUND", "상위 부서를 찾을 수 없습니다.");
		}
		if (!parent.getCompany().getCompanyPk().equals(company.getCompanyPk())) {
			throw OrgExceptions.of(409, "DEPT.PARENT_COMPANY_MISMATCH", "다른 회사의 부서를 상위로 둘 수 없습니다.");
		}
		if (!parent.inUse()) {
			throw OrgExceptions.of(409, "DEPT.PARENT_INACTIVE", "상위 부서가 사용 중이 아닙니다.");
		}
		if (self != null && DeptPaths.isSelfOrDescendant(parent.getDeptPath(), self.getDeptPath())) {
			throw OrgExceptions.of(409, "DEPT.PARENT_CYCLE", "부서 계층이 순환합니다.");
		}
		return parent;
	}

	private Company activeCompany(String companyCode) {
		Company company = companies.findByCompanyCode(companyCode)
				.orElseThrow(() -> OrgExceptions.of(404, "COMPANY.NOT_FOUND", "회사를 찾을 수 없습니다."));
		if (!company.inUse()) {
			throw OrgExceptions.field("COMMON.VALIDATION_FAILED", "사용 중인 회사가 아닙니다.", "companyCode");
		}
		return company;
	}

	private Dept require(long deptPk) {
		return depts.findById(deptPk).orElseThrow(() -> OrgExceptions.of(404, "DEPT.NOT_FOUND", "부서를 찾을 수 없습니다."));
	}

	private DeptResponse response(Dept dept, boolean withTranslations) {
		Dept parent = dept.getParentDept();
		return new DeptResponse(dept.getDeptPk(), dept.getCompany().getCompanyPk(), dept.getCompany().getCompanyCode(),
				parent == null ? null : parent.getDeptPk(), parent == null ? null : parent.getDeptCode(), dept.getDeptCode(),
				dept.getDeptName(), dept.getDeptTypeCode(), dept.getLevelNo(), dept.getDeptPath(), dept.getSortOrder(),
				dept.getValidFromDate(), dept.getValidToDate(), dept.getDescription(), dept.getUseYn(),
				dept.getCreateDatetime(), dept.getCreateUserPk(), dept.getUpdateDatetime(), dept.getUpdateUserPk(),
				withTranslations ? texts.list(EntityTextService.DEPT, EntityTextService.DEPT_NAME, dept.getDeptPk()) : null);
	}

	private static DeptHistoryResponse history(DeptHistory history) {
		return new DeptHistoryResponse(history.getHistoryPk(), history.getDept().getDeptPk(), history.getChangeTypeCode(),
				history.getEffectiveDate(), history.getBeforeDeptName(), history.getAfterDeptName(),
				history.getBeforeParentDeptPk(), history.getAfterParentDeptPk(), history.getChangeReason(),
				history.getChangeDatetime(), history.getChangeUserPk());
	}

	private static Specification<Dept> filter(String companyCode, String keyword, String deptTypeCode, String useYn,
			Long parentDeptPk) {
		return (root, query, builder) -> {
			List<Predicate> predicates = new ArrayList<>();
			if (companyCode != null && !companyCode.isBlank()) {
				predicates.add(builder.equal(root.get("company").get("companyCode"), companyCode));
			}
			String text = OrgExceptions.trimToNull(keyword);
			if (text != null) {
				String pattern = "%" + text.toLowerCase() + "%";
				predicates.add(builder.or(builder.like(builder.lower(root.get("deptCode")), pattern),
						builder.like(builder.lower(root.get("deptName")), pattern)));
			}
			if (deptTypeCode != null && !deptTypeCode.isBlank()) {
				predicates.add(builder.equal(root.get("deptTypeCode"), deptTypeCode));
			}
			if (useYn != null && !useYn.isBlank()) {
				predicates.add(builder.equal(root.get("useYn"), useYn));
			}
			if (parentDeptPk != null) {
				predicates.add(builder.equal(root.get("parentDept").get("deptPk"), parentDeptPk));
			}
			return predicates.isEmpty() ? builder.conjunction() : builder.and(predicates.toArray(Predicate[]::new));
		};
	}

}
