package com.tommy.common.menu.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tommy.common.menu.entity.Menu;

public interface MenuRepository extends JpaRepository<Menu, Long>, JpaSpecificationExecutor<Menu> {

	Optional<Menu> findByMenuCode(String menuCode);

	boolean existsByParentMenuPkAndUseYn(Long parentMenuPk, String useYn);

	@Query("""
			select count(m) > 0 from Menu m
			where m.routePath = :routePath and m.menuTypeCode = 'PAGE' and m.useYn = 'Y'
			  and (:menuPk is null or m.menuPk <> :menuPk)
			""")
	boolean routeTaken(@Param("routePath") String routePath, @Param("menuPk") Long menuPk);

}
