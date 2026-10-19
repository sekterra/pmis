package com.tommy.common.org.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tommy.common.global.response.PageResponse;
import com.tommy.common.global.security.CurrentTenantUser;
import com.tommy.common.i18n.entity.Nation;
import com.tommy.common.i18n.repository.NationRepository;
import com.tommy.common.org.dto.CompanyRequest;
import com.tommy.common.org.dto.CompanyResponse;
import com.tommy.common.org.dto.CompanyUpdateRequest;
import com.tommy.common.org.entity.Company;
import com.tommy.common.org.repository.CompanyRepository;
import com.tommy.common.org.repository.DeptRepository;
import com.tommy.common.org.repository.PersonAssignmentRepository;
import com.tommy.common.org.support.OrgExceptions;

import jakarta.persistence.criteria.Predicate;

@Service
public class CompanyService {

	private final CompanyRepository companies;

	private final DeptRepository depts;

	private final PersonAssignmentRepository assignments;

	private final NationRepository nations;

	private final EntityTextService texts;

	private final OrgCodes codes;

	private final CurrentTenantUser currentUser;

	public CompanyService(CompanyRepository companies, DeptRepository depts, PersonAssignmentRepository assignments,
			NationRepository nations, EntityTextService texts, OrgCodes codes, CurrentTenantUser currentUser) {
		this.companies = companies;
		this.depts = depts;
		this.assignments = assignments;
		this.nations = nations;
		this.texts = texts;
		this.codes = codes;
		this.currentUser = currentUser;
	}

	@Transactional(readOnly = true)
	public PageResponse<CompanyResponse> list(String keyword, String companyTypeCode, String useYn, Pageable pageable) {
		Page<Company> page = companies.findAll(filter(keyword, companyTypeCode, useYn), pageable);
		return new PageResponse<>(page.getContent().stream().map(company -> response(company, false)).toList(),
				page.getTotalElements(), pageable.getPageNumber() + 1, pageable.getPageSize());
	}

	@Transactional(readOnly = true)
	public CompanyResponse get(long companyPk) {
		return response(require(companyPk), true);
	}

	@Transactional
	public CompanyResponse create(CompanyRequest request) {
		if (companies.existsByCompanyCode(request.companyCode())) {
			throw OrgExceptions.of(409, "COMPANY.DUPLICATE", "이미 있는 회사 코드입니다.");
		}
		codes.require("COMPANY_TYPE", request.companyTypeCode(), "companyTypeCode");
		String businessNo = OrgExceptions.trimToNull(request.businessRegistrationNo());
		if (businessNo != null && companies.existsByBusinessRegistrationNoAndUseYn(businessNo, "Y")) {
			throw OrgExceptions.of(409, "COMPANY.DUPLICATE_BUSINESS_NO", "이미 있는 사업자등록번호입니다.");
		}
		Company parent = parent(request.parentCompanyCode(), request.companyCode(), null);
		Company company = Company.create(request.companyCode(), request.companyName().trim(), currentUser.pkOrNull());
		apply(company, request.companyName(), request.companyShortName(), request.companyTypeCode(), businessNo,
				request.corporateRegistrationNo(), request.representativeName(), request.postalCode(), request.address(),
				request.addressDetail(), request.phoneNo(), request.faxNo(), request.email(), request.websiteUrl(),
				request.sortOrder(), request.description(), parent, nation(request.nationCode()));
		companies.save(company);
		texts.upsert(EntityTextService.COMPANY, EntityTextService.COMPANY_NAME, company.getCompanyPk(), request.translations());
		return response(company, true);
	}

	@Transactional
	public CompanyResponse update(long companyPk, CompanyUpdateRequest request) {
		Company company = require(companyPk);
		codes.require("COMPANY_TYPE", request.companyTypeCode(), "companyTypeCode");
		String businessNo = OrgExceptions.trimToNull(request.businessRegistrationNo());
		if (businessNo != null && companies.existsByBusinessRegistrationNoAndUseYnAndCompanyPkNot(businessNo, "Y", companyPk)) {
			throw OrgExceptions.of(409, "COMPANY.DUPLICATE_BUSINESS_NO", "이미 있는 사업자등록번호입니다.");
		}
		Company parent = parent(request.parentCompanyCode(), company.getCompanyCode(), company);
		apply(company, request.companyName(), request.companyShortName(), request.companyTypeCode(), businessNo,
				request.corporateRegistrationNo(), request.representativeName(), request.postalCode(), request.address(),
				request.addressDetail(), request.phoneNo(), request.faxNo(), request.email(), request.websiteUrl(),
				request.sortOrder(), request.description(), parent, nation(request.nationCode()));
		texts.upsert(EntityTextService.COMPANY, EntityTextService.COMPANY_NAME, companyPk, request.translations());
		return response(company, true);
	}

	@Transactional
	public void delete(long companyPk) {
		Company company = require(companyPk);
		if (companies.existsByParentCompany_CompanyPkAndUseYn(companyPk, "Y")) {
			throw OrgExceptions.of(409, "COMPANY.HAS_CHILD_COMPANY", "사용 중인 하위 회사가 있습니다.");
		}
		if (depts.existsByCompany_CompanyPkAndUseYn(companyPk, "Y")) {
			throw OrgExceptions.of(409, "COMPANY.HAS_ACTIVE_DEPT", "사용 중인 부서가 있습니다.");
		}
		if (assignments.existsByCompany_CompanyPkAndUseYnAndEndDateIsNull(companyPk, "Y")) {
			throw OrgExceptions.of(409, "COMPANY.HAS_ACTIVE_ASSIGNMENT", "현재 소속이 있습니다.");
		}
		if ("OWN".equals(company.getCompanyTypeCode()) && company.inUse()
				&& companies.countByCompanyTypeCodeAndUseYnAndCompanyPkNot("OWN", "Y", companyPk) == 0) {
			throw OrgExceptions.of(409, "COMPANY.LAST_OWN", "마지막 자사는 삭제할 수 없습니다.");
		}
		company.deactivate(currentUser.pkOrNull());
	}

	@Transactional
	public CompanyResponse restore(long companyPk) {
		Company company = require(companyPk);
		if (company.getParentCompany() != null && !company.getParentCompany().inUse()) {
			throw OrgExceptions.of(409, "COMPANY.PARENT_INACTIVE", "상위 회사가 사용 중이 아닙니다.");
		}
		String businessNo = company.getBusinessRegistrationNo();
		if (businessNo != null && companies.existsByBusinessRegistrationNoAndUseYnAndCompanyPkNot(businessNo, "Y", companyPk)) {
			throw OrgExceptions.of(409, "COMPANY.DUPLICATE_BUSINESS_NO", "이미 있는 사업자등록번호입니다.");
		}
		company.activate(currentUser.pkOrNull());
		return response(company, true);
	}

	private void apply(Company company, String companyName, String companyShortName, String companyTypeCode, String businessNo,
			String corporateRegistrationNo, String representativeName, String postalCode, String address, String addressDetail,
			String phoneNo, String faxNo, String email, String websiteUrl, Integer sortOrder, String description, Company parent,
			Nation nation) {
		company.change(companyName.trim(), OrgExceptions.trimToNull(companyShortName), companyTypeCode, businessNo,
				OrgExceptions.trimToNull(corporateRegistrationNo), OrgExceptions.trimToNull(representativeName),
				OrgExceptions.trimToNull(postalCode), OrgExceptions.trimToNull(address), OrgExceptions.trimToNull(addressDetail),
				OrgExceptions.trimToNull(phoneNo), OrgExceptions.trimToNull(faxNo), OrgExceptions.trimToNull(email),
				OrgExceptions.trimToNull(websiteUrl), sortOrder, OrgExceptions.trimToNull(description), currentUser.pkOrNull());
		company.assignParent(parent);
		company.assignNation(nation);
	}

	private Company parent(String parentCompanyCode, String companyCode, Company self) {
		String code = OrgExceptions.trimToNull(parentCompanyCode);
		if (code == null) {
			return null;
		}
		if (code.equals(companyCode)) {
			throw OrgExceptions.of(409, "COMPANY.PARENT_CYCLE", "회사 계층이 순환합니다.");
		}
		Company parent = companies.findByCompanyCode(code)
				.orElseThrow(() -> OrgExceptions.of(404, "COMPANY.NOT_FOUND", "상위 회사를 찾을 수 없습니다."));
		if (!parent.inUse()) {
			throw OrgExceptions.of(409, "COMPANY.PARENT_INACTIVE", "상위 회사가 사용 중이 아닙니다.");
		}
		if (self != null) {
			Company cursor = parent;
			int guard = 0;
			while (cursor != null && guard++ < 50) {
				if (self.getCompanyPk().equals(cursor.getCompanyPk())) {
					throw OrgExceptions.of(409, "COMPANY.PARENT_CYCLE", "회사 계층이 순환합니다.");
				}
				cursor = cursor.getParentCompany();
			}
		}
		return parent;
	}

	private Nation nation(String nationCode) {
		String code = OrgExceptions.trimToNull(nationCode);
		if (code == null) {
			return null;
		}
		Nation nation = nations.findByNationCode(code).filter(Nation::inUse).orElse(null);
		if (nation == null) {
			throw OrgExceptions.field("COMMON.VALIDATION_FAILED", "국가를 찾을 수 없거나 사용 중이 아닙니다.", "nationCode");
		}
		return nation;
	}

	private Company require(long companyPk) {
		return companies.findById(companyPk)
				.orElseThrow(() -> OrgExceptions.of(404, "COMPANY.NOT_FOUND", "회사를 찾을 수 없습니다."));
	}

	private CompanyResponse response(Company company, boolean withTranslations) {
		Company parent = company.getParentCompany();
		Nation nation = company.getNation();
		return new CompanyResponse(company.getCompanyPk(), parent == null ? null : parent.getCompanyPk(),
				parent == null ? null : parent.getCompanyCode(), company.getCompanyCode(), company.getCompanyName(),
				company.getCompanyShortName(), company.getCompanyTypeCode(), company.getBusinessRegistrationNo(),
				company.getCorporateRegistrationNo(), company.getRepresentativeName(),
				nation == null ? null : nation.getNationPk(), nation == null ? null : nation.getNationCode(),
				company.getPostalCode(), company.getAddress(), company.getAddressDetail(), company.getPhoneNo(),
				company.getFaxNo(), company.getEmail(), company.getWebsiteUrl(), company.getSortOrder(),
				company.getDescription(), company.getUseYn(), company.getCreateDatetime(), company.getCreateUserPk(),
				company.getUpdateDatetime(), company.getUpdateUserPk(),
				withTranslations ? texts.list(EntityTextService.COMPANY, EntityTextService.COMPANY_NAME, company.getCompanyPk())
						: null);
	}

	private static Specification<Company> filter(String keyword, String companyTypeCode, String useYn) {
		return (root, query, builder) -> {
			List<Predicate> predicates = new java.util.ArrayList<>();
			String text = OrgExceptions.trimToNull(keyword);
			if (text != null) {
				String pattern = "%" + text.toLowerCase() + "%";
				predicates.add(builder.or(
						builder.like(builder.lower(root.get("companyCode")), pattern),
						builder.like(builder.lower(root.get("companyName")), pattern),
						builder.like(builder.lower(root.get("companyShortName")), pattern)));
			}
			if (companyTypeCode != null && !companyTypeCode.isBlank()) {
				predicates.add(builder.equal(root.get("companyTypeCode"), companyTypeCode));
			}
			if (useYn != null && !useYn.isBlank()) {
				predicates.add(builder.equal(root.get("useYn"), useYn));
			}
			return predicates.isEmpty() ? builder.conjunction() : builder.and(predicates.toArray(Predicate[]::new));
		};
	}

}
