package com.tommy.common.menu.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tommy.common.menu.entity.Menu;

public interface MenuRepository extends JpaRepository<Menu, Long> {

	Optional<Menu> findByMenuCode(String menuCode);

}
