package com.tommy.common.org.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tommy.common.org.entity.PersonAssignment;

public interface PersonAssignmentRepository extends JpaRepository<PersonAssignment, Long>, JpaSpecificationExecutor<PersonAssignment> {

	boolean existsByCompany_CompanyPkAndUseYnAndEndDateIsNull(Long companyPk, String useYn);

	boolean existsByDept_DeptPkAndUseYnAndEndDateIsNull(Long deptPk, String useYn);

	boolean existsByPerson_PersonPkAndUseYnAndEndDateIsNull(Long personPk, String useYn);

	boolean existsByCompany_CompanyPkAndEmployeeNoAndUseYn(Long companyPk, String employeeNo, String useYn);

	boolean existsByCompany_CompanyPkAndEmployeeNoAndUseYnAndAssignmentPkNot(Long companyPk, String employeeNo, String useYn,
			Long assignmentPk);

	@Query("""
			select count(a) from PersonAssignment a
			where a.person.personPk = :personPk and a.endDate is null and a.useYn = 'Y' and a.assignmentPk <> :excludePk
			""")
	long countOtherCurrent(@Param("personPk") Long personPk, @Param("excludePk") Long excludePk);

	@Query("""
			select a from PersonAssignment a
			where a.person.personPk = :personPk and a.primaryYn = 'Y' and a.endDate is null and a.useYn = 'Y'
			""")
	Optional<PersonAssignment> findCurrentPrimary(@Param("personPk") Long personPk);

	@Query("""
			select count(a) from PersonAssignment a
			where a.person.personPk = :personPk
			  and a.company.companyPk = :companyPk
			  and ((:deptPk is null and a.dept is null) or a.dept.deptPk = :deptPk)
			  and a.endDate is null and a.useYn = 'Y'
			  and a.assignmentPk <> :excludePk
			""")
	long countCurrentDuplicate(@Param("personPk") Long personPk, @Param("companyPk") Long companyPk,
			@Param("deptPk") Long deptPk, @Param("excludePk") Long excludePk);

	@Query("""
			select a.person.personPk, c.companyCode, c.companyName, d.deptCode, d.deptName,
			       a.positionCode, a.jobTitleCode, a.employeeNo, a.assignmentStatusCode
			from PersonAssignment a
			join a.company c
			left join a.dept d
			where a.person.personPk in :personPks
			  and a.primaryYn = 'Y' and a.endDate is null and a.useYn = 'Y'
			""")
	List<Object[]> findCurrentPrimarySummaries(@Param("personPks") Collection<Long> personPks);

	List<PersonAssignment> findByPerson_PersonPkAndEndDateIsNullAndUseYnOrderByPrimaryYnDescAssignmentPkAsc(Long personPk, String useYn);

}
