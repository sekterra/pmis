package com.tommy.common.org.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tommy.common.org.entity.Dept;

public interface DeptRepository extends JpaRepository<Dept, Long>, JpaSpecificationExecutor<Dept> {

	boolean existsByCompany_CompanyPkAndDeptCode(Long companyPk, String deptCode);

	@Query("""
			select count(d) from Dept d
			where d.deptCode = :deptCode and d.company.companyPk <> :companyPk
			""")
	long countInOtherCompany(@Param("deptCode") String deptCode, @Param("companyPk") Long companyPk);

	Optional<Dept> findByCompany_CompanyPkAndDeptCode(Long companyPk, String deptCode);

	boolean existsByParentDept_DeptPkAndUseYn(Long parentDeptPk, String useYn);

	boolean existsByCompany_CompanyPkAndUseYn(Long companyPk, String useYn);

	@Query("""
			select d from Dept d
			left join fetch d.parentDept
			join fetch d.company
			where d.company.companyPk = :companyPk
			  and d.validFromDate <= :asOf
			  and (d.validToDate is null or d.validToDate >= :asOf)
			  and (:includeClosed = true or d.useYn = 'Y')
			""")
	List<Dept> findTree(@Param("companyPk") Long companyPk, @Param("asOf") LocalDate asOf,
			@Param("includeClosed") boolean includeClosed);

}
