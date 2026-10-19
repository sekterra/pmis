package com.tommy.common.access.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import com.tommy.common.access.entity.Permission;

public interface PermissionRepository extends JpaRepository<Permission, Long>, JpaSpecificationExecutor<Permission> {

	Optional<Permission> findByPermissionCode(String permissionCode);

	boolean existsByMenu_MenuPkAndActionCode(Long menuPk, String actionCode);

	List<Permission> findByUseYnOrderByMenu_SortOrderAscSortOrderAscPermissionPkAsc(String useYn);

	List<Permission> findByPermissionCodeIn(Collection<String> permissionCodes);

	List<Permission> findByMenu_MenuPk(Long menuPk);

	@Query("""
			select p.menu.menuPk, count(p) from Permission p where p.useYn = 'Y' group by p.menu.menuPk
			""")
	List<Object[]> countInUseByMenu();

}
