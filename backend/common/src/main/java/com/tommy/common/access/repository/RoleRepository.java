package com.tommy.common.access.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tommy.common.access.entity.Role;

public interface RoleRepository extends JpaRepository<Role, Long>, JpaSpecificationExecutor<Role> {

	Optional<Role> findByRoleCode(String roleCode);

	boolean existsByRoleCode(String roleCode);

	@Query("""
			select r.rolePk, count(ur)
			from UserRole ur join ur.role r join ur.userAccount u
			where r.rolePk in :rolePks and ur.useYn = 'Y' and u.useYn = 'Y' and u.loginId <> '$platform-support'
			  and (ur.validFromDate is null or ur.validFromDate <= :today)
			  and (ur.validToDate is null or ur.validToDate >= :today)
			group by r.rolePk
			""")
	List<Object[]> countCurrentUsers(@Param("rolePks") Collection<Long> rolePks, @Param("today") LocalDate today);

}
