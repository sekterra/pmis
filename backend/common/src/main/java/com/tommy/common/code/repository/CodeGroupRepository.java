package com.tommy.common.code.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.tommy.common.code.entity.CodeGroup;

public interface CodeGroupRepository extends JpaRepository<CodeGroup, Long>, JpaSpecificationExecutor<CodeGroup> {

	Optional<CodeGroup> findByCodeGroupCode(String codeGroupCode);

	boolean existsByCodeGroupCode(String codeGroupCode);

}
