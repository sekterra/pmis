package com.tommy.common.code.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tommy.common.code.entity.CommonCodeLang;

public interface CommonCodeLangRepository extends JpaRepository<CommonCodeLang, Long> {

	List<CommonCodeLang> findByCommonCodeCodePkIn(Collection<Long> codePks);

	Optional<CommonCodeLang> findByCommonCodeCodePkAndSupportLanguageCode(Long codePk, String supportLanguageCode);

}
