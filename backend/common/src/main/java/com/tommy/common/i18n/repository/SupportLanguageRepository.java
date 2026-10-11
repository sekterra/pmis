package com.tommy.common.i18n.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tommy.common.i18n.entity.SupportLanguage;

public interface SupportLanguageRepository extends JpaRepository<SupportLanguage, String> {

	List<SupportLanguage> findByUseYnOrderBySortOrderAscSupportLanguageCodeAsc(String useYn);

	List<SupportLanguage> findAllByOrderBySortOrderAscSupportLanguageCodeAsc();

	List<SupportLanguage> findByDefaultYnAndUseYn(String defaultYn, String useYn);

	boolean existsByFallbackLanguageCodeAndUseYn(String fallbackLanguageCode, String useYn);

}
