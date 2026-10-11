package com.tommy.common.i18n.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.tommy.common.i18n.entity.I18nMessage;

public interface I18nMessageRepository extends JpaRepository<I18nMessage, Long>, JpaSpecificationExecutor<I18nMessage> {

	List<I18nMessage> findByNamespaceNamespacePkAndUseYn(Long namespacePk, String useYn);

	Optional<I18nMessage> findByNamespaceNamespacePkAndMessageKeyAndUseYn(Long namespacePk, String messageKey, String useYn);

	boolean existsByNamespaceNamespacePkAndMessageKeyAndUseYn(Long namespacePk, String messageKey, String useYn);

}
