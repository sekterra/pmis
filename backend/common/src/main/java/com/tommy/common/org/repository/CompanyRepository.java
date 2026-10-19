package com.tommy.common.org.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.tommy.common.org.entity.Company;

public interface CompanyRepository extends JpaRepository<Company, Long>, JpaSpecificationExecutor<Company> {

	Optional<Company> findByCompanyCode(String companyCode);

	boolean existsByCompanyCode(String companyCode);

	boolean existsByParentCompany_CompanyPkAndUseYn(Long parentCompanyPk, String useYn);

	boolean existsByCompanyPkAndUseYn(Long companyPk, String useYn);

	long countByCompanyTypeCodeAndUseYnAndCompanyPkNot(String companyTypeCode, String useYn, Long companyPk);

	boolean existsByBusinessRegistrationNoAndUseYn(String businessRegistrationNo, String useYn);

	boolean existsByBusinessRegistrationNoAndUseYnAndCompanyPkNot(String businessRegistrationNo, String useYn, Long companyPk);

}
