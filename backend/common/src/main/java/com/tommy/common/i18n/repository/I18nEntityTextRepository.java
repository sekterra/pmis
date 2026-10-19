package com.tommy.common.i18n.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tommy.common.i18n.entity.I18nEntityText;

public interface I18nEntityTextRepository extends JpaRepository<I18nEntityText, Long> {

	Optional<I18nEntityText> findByTargetTableNameAndTargetColumnNameAndTargetKeyAndSupportLanguageCode(
			String targetTableName, String targetColumnName, String targetKey, String supportLanguageCode);

	List<I18nEntityText> findByTargetTableNameAndTargetColumnNameAndTargetKeyAndUseYnOrderBySupportLanguageCodeAsc(
			String targetTableName, String targetColumnName, String targetKey, String useYn);

}
