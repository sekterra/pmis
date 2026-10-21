package com.tommy.common.i18n.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tommy.common.global.query.LikeEscape;
import com.tommy.common.global.response.PageResponse;
import com.tommy.common.i18n.I18nExceptions;
import com.tommy.common.i18n.dto.NamespaceRequest;
import com.tommy.common.i18n.dto.NamespaceResponse;
import com.tommy.common.i18n.entity.I18nNamespace;
import com.tommy.common.i18n.repository.I18nNamespaceRepository;

import jakarta.persistence.criteria.Predicate;

@Service
@Transactional(readOnly = true)
public class I18nNamespaceService {

	private final I18nNamespaceRepository repository;
	private final BundleService bundles;

	public I18nNamespaceService(I18nNamespaceRepository repository, BundleService bundles) {
		this.repository = repository;
		this.bundles = bundles;
	}

	public PageResponse<NamespaceResponse> list(String keyword, String useYn, Pageable pageable) {
		Page<I18nNamespace> page = repository.findAll(filter(keyword, useYn), pageable);
		return new PageResponse<>(page.getContent().stream().map(this::response).toList(), page.getTotalElements(),
				pageable.getPageNumber() + 1, pageable.getPageSize());
	}

	public NamespaceResponse get(String namespaceCode) {
		return response(require(namespaceCode));
	}

	@Transactional
	public NamespaceResponse create(NamespaceRequest request) {
		if (repository.existsByNamespaceCode(request.namespaceCode())) {
			throw I18nExceptions.duplicate();
		}
		I18nNamespace namespace = repository.save(I18nNamespace.create(
				request.namespaceCode(), request.namespaceName(), request.solutionCode(), request.description()));
		flush();
		return response(namespace);
	}

	@Transactional
	public NamespaceResponse update(String namespaceCode, NamespaceRequest request) {
		if (!namespaceCode.equals(request.namespaceCode())) {
			throw I18nExceptions.immutable("네임스페이스 코드는 바꿀 수 없습니다.");
		}
		I18nNamespace namespace = require(namespaceCode);
		namespace.change(request.namespaceName(), request.solutionCode(), request.description());
		bundles.evictNamespace(namespaceCode);
		return response(namespace);
	}

	@Transactional
	public void deactivate(String namespaceCode) {
		require(namespaceCode).deactivate();
		bundles.evictNamespace(namespaceCode);
	}

	@Transactional
	public NamespaceResponse restore(String namespaceCode) {
		I18nNamespace namespace = require(namespaceCode);
		namespace.activate();
		bundles.evictNamespace(namespaceCode);
		return response(namespace);
	}

	private I18nNamespace require(String namespaceCode) {
		return repository.findByNamespaceCode(namespaceCode).orElseThrow(I18nExceptions::namespaceNotFound);
	}

	private void flush() {
		try {
			repository.flush();
		}
		catch (DataIntegrityViolationException ex) {
			throw I18nExceptions.duplicate();
		}
	}

	private static Specification<I18nNamespace> filter(String keyword, String useYn) {
		return (root, query, builder) -> {
			List<Predicate> predicates = new ArrayList<>();
			if (keyword != null && !keyword.isBlank()) {
				predicates.add(builder.or(
						LikeEscape.contains(builder, root.get("namespaceCode"), keyword),
						LikeEscape.contains(builder, root.get("namespaceName"), keyword)));
			}
			if (useYn != null && !useYn.isBlank()) {
				predicates.add(builder.equal(root.get("useYn"), useYn));
			}
			return builder.and(predicates.toArray(Predicate[]::new));
		};
	}

	private NamespaceResponse response(I18nNamespace namespace) {
		return new NamespaceResponse(namespace.getNamespacePk(), namespace.getNamespaceCode(), namespace.getNamespaceName(),
				namespace.getSolutionCode(), namespace.getDescription(), namespace.getUseYn());
	}

}
