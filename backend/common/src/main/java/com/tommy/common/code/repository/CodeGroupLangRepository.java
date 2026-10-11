package com.tommy.common.code.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tommy.common.code.entity.CodeGroupLang;

public interface CodeGroupLangRepository extends JpaRepository<CodeGroupLang, Long> {

	List<CodeGroupLang> findByCodeGroupCodeGroupPkIn(Collection<Long> codeGroupPks);

	Optional<CodeGroupLang> findByCodeGroupCodeGroupPkAndSupportLanguageCode(Long codeGroupPk, String supportLanguageCode);

}
