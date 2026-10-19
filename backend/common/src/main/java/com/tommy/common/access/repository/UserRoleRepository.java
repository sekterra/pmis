package com.tommy.common.access.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tommy.common.access.entity.UserRole;

public interface UserRoleRepository extends JpaRepository<UserRole, Long> {

	List<UserRole> findByUserAccount_UserPk(Long userPk);

	Optional<UserRole> findByUserAccount_UserPkAndRole_RolePk(Long userPk, Long rolePk);

	boolean existsByRole_RolePkAndUseYn(Long rolePk, String useYn);

	@Query("select ur.userAccount.userPk from UserRole ur where ur.role.rolePk = :rolePk and ur.useYn = 'Y'")
	List<Long> findActiveUserPks(@Param("rolePk") Long rolePk);

}
