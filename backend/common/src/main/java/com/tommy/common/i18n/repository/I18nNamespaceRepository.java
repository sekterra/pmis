package com.tommy.common.i18n.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.tommy.common.i18n.entity.I18nNamespace;

public interface I18nNamespaceRepository extends JpaRepository<I18nNamespace, Long>, JpaSpecificationExecutor<I18nNamespace> {

	Optional<I18nNamespace> findByNamespaceCode(String namespaceCode);

	boolean existsByNamespaceCode(String namespaceCode);

}
