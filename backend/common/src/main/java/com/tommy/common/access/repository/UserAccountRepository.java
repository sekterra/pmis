package com.tommy.common.access.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tommy.common.access.entity.UserAccount;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long>, JpaSpecificationExecutor<UserAccount> {

	boolean existsByLoginId(String loginId);

	Optional<UserAccount> findByLoginId(String loginId);

	boolean existsByPerson_PersonPkAndUseYnAndUserPkNot(Long personPk, String useYn, Long userPk);

	boolean existsByPerson_PersonPkAndUseYn(Long personPk, String useYn);

	@Query("""
			select count(u) from UserAccount u, UserRole ur
			where ur.userAccount = u and ur.role.roleCode = 'TENANT_ADMIN' and ur.role.useYn = 'Y' and ur.useYn = 'Y'
			  and u.useYn = 'Y' and u.userStatusCode = 'ACTIVE' and u.userPk <> :userPk
			  and (ur.validFromDate is null or ur.validFromDate <= :today)
			  and (ur.validToDate is null or ur.validToDate >= :today)
			""")
	long countOtherActiveAdmins(@Param("userPk") Long userPk, @Param("today") LocalDate today);

	@Query("""
			select u.userPk, r.roleCode
			from UserRole ur join ur.userAccount u join ur.role r
			where u.userPk in :userPks and ur.useYn = 'Y' and r.useYn = 'Y'
			  and (ur.validFromDate is null or ur.validFromDate <= :today)
			  and (ur.validToDate is null or ur.validToDate >= :today)
			""")
	List<Object[]> findCurrentRoleCodes(@Param("userPks") Collection<Long> userPks, @Param("today") LocalDate today);

}
