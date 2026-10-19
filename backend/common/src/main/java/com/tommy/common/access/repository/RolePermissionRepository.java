package com.tommy.common.access.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tommy.common.access.entity.RolePermission;

public interface RolePermissionRepository extends JpaRepository<RolePermission, Long> {

	List<RolePermission> findByRole_RolePk(Long rolePk);

	Optional<RolePermission> findByRole_RolePkAndPermission_PermissionPk(Long rolePk, Long permissionPk);

	List<RolePermission> findByPermission_PermissionPkAndUseYn(Long permissionPk, String useYn);

}
