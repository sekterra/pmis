package com.tommy.common.i18n.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.tommy.common.i18n.entity.Nation;

public interface NationRepository extends JpaRepository<Nation, Long>, JpaSpecificationExecutor<Nation> {

	Optional<Nation> findByNationCode(String nationCode);

	boolean existsByNationCode(String nationCode);

	List<Nation> findByUseYnOrderBySortOrderAscNationCodeAsc(String useYn);

}
