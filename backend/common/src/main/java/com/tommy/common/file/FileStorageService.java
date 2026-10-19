package com.tommy.common.file;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tommy.common.file.dto.FileDtos.StorageRequest;
import com.tommy.common.file.dto.FileDtos.StorageResponse;
import com.tommy.common.file.dto.FileDtos.StorageUpdateRequest;
import com.tommy.common.file.entity.FileStorage;
import com.tommy.common.file.repository.FileStorageRepository;
import com.tommy.common.file.repository.StoredFileRepository;
import com.tommy.common.global.exception.ApiException;
import com.tommy.common.global.response.PageResponse;
import com.tommy.common.global.security.CurrentTenantUser;
import com.tommy.common.org.service.OrgCodes;
import com.tommy.common.org.support.OrgExceptions;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.Predicate;
import tools.jackson.databind.ObjectMapper;

@Service
public class FileStorageService {

	private final FileStorageRepository storages;

	private final StoredFileRepository files;

	private final OrgCodes codes;

	private final CurrentTenantUser currentUser;

	private final EntityManager entityManager;

	private final ObjectMapper objectMapper;

	private final FileProperties properties;

	public FileStorageService(FileStorageRepository storages, StoredFileRepository files, OrgCodes codes,
			CurrentTenantUser currentUser, EntityManager entityManager, ObjectMapper objectMapper, FileProperties properties) {
		this.storages = storages;
		this.files = files;
		this.codes = codes;
		this.currentUser = currentUser;
		this.entityManager = entityManager;
		this.objectMapper = objectMapper;
		this.properties = properties;
	}

	@Transactional(readOnly = true)
	public PageResponse<StorageResponse> list(String keyword, String storageTypeCode, String useYn, Pageable pageable) {
		Page<FileStorage> page = storages.findAll(filter(keyword, storageTypeCode, useYn), pageable);
		return new PageResponse<>(page.getContent().stream().map(row -> response(row, false)).toList(), page.getTotalElements(),
				pageable.getPageNumber() + 1, pageable.getPageSize());
	}

	@Transactional(readOnly = true)
	public StorageResponse get(long storagePk) {
		return response(require(storagePk), true);
	}

	@Transactional
	public StorageResponse create(StorageRequest request) {
		String code = request.storageCode().trim();
		FileRules.assertCode(code, "storageCode");
		Long actor = currentUser.pkOrNull();
		FileStorage storage = FileStorage.create(code, request.storageName().trim(), request.storageTypeCode().trim(),
				request.basePath().trim(), actor);
		apply(storage, request.storageName(), request.storageTypeCode(), request.basePath(), request.connectionConfig(),
				request.secretRef(), request.maxFileSizeMb(), request.allowedExtensions(), request.description(), actor);
		boolean asDefault = "Y".equals(yn(request.defaultYn(), "N"));
		try {
			storages.saveAndFlush(storage);
		}
		catch (DataIntegrityViolationException ex) {
			throw new ApiException(409, "FILE.STORAGE_DUPLICATE", "이미 있는 저장소 코드입니다.", 2);
		}
		if (asDefault) {
			makeDefault(storage, actor);
		}
		return response(storage, true);
	}

	@Transactional
	public StorageResponse update(long storagePk, StorageUpdateRequest request) {
		FileStorage storage = require(storagePk);
		if (!storage.inUse()) {
			throw new ApiException(409, "FILE.STORAGE_INACTIVE", "사용 중이 아닌 저장소입니다.", 2);
		}
		Long actor = currentUser.pkOrNull();
		apply(storage, request.storageName(), request.storageTypeCode(), request.basePath(), request.connectionConfig(),
				request.secretRef(), request.maxFileSizeMb(), request.allowedExtensions(), request.description(), actor);
		storages.save(storage);
		if (request.defaultYn() != null) {
			if ("Y".equals(yn(request.defaultYn(), storage.getDefaultYn()))) {
				makeDefault(storage, actor);
			}
			else {
				storage.markDefault(false, actor);
			}
		}
		return response(storage, true);
	}

	@Transactional
	public void delete(long storagePk) {
		FileStorage storage = require(storagePk);
		if (!storage.inUse()) {
			throw new ApiException(409, "FILE.STORAGE_INACTIVE", "사용 중이 아닌 저장소입니다.", 2);
		}
		if (files.existsByStorage_StoragePkAndUseYnAndFileStatusCodeNot(storagePk, "Y", "DELETED")) {
			throw new ApiException(409, "FILE.STORAGE_IN_USE", "파일을 가진 저장소입니다.", 2);
		}
		if (storage.defaultStorage()) {
			throw new ApiException(409, "FILE.DEFAULT_STORAGE", "기본 저장소는 삭제할 수 없습니다.", 2);
		}
		if (storages.countByUseYn("Y") <= 1) {
			throw new ApiException(409, "FILE.LAST_STORAGE", "마지막 저장소는 삭제할 수 없습니다.", 2);
		}
		storage.deactivate(currentUser.pkOrNull());
	}

	@Transactional
	public StorageResponse restore(long storagePk) {
		FileStorage storage = require(storagePk);
		Long actor = currentUser.pkOrNull();
		storage.activate(actor);
		if (storage.defaultStorage()) {
			makeDefault(storage, actor);
		}
		return response(storage, true);
	}

	@Transactional
	public FileStorage requireWritable(String storageCode) {
		if (storageCode != null && !storageCode.isBlank()) {
			FileStorage storage = storages.findByStorageCode(storageCode.trim())
					.orElseThrow(() -> new ApiException(404, "FILE.STORAGE_NOT_FOUND", "저장소를 찾을 수 없습니다.", 2));
			if (!storage.inUse()) {
				throw new ApiException(409, "FILE.STORAGE_INACTIVE", "사용 중이 아닌 저장소입니다.", 2);
			}
			return storage;
		}
		return storages.findFirstByDefaultYnAndUseYn("Y", "Y").orElseGet(this::lazyDefault);
	}

	public PathBase locate(FileStorage storage, String storageKey) {
		if (!"LOCAL".equals(storage.getStorageTypeCode())) {
			throw new ApiException(501, "FILE.STORAGE_TYPE_UNSUPPORTED", "이 저장소 유형은 아직 지원하지 않습니다.", 2);
		}
		java.nio.file.Path base = FileRules.resolveBase(properties.getLocalRoot(), storage.getBasePath());
		return new PathBase(FileRules.resolveFile(base, tenantCode(), storageKey));
	}

	private FileStorage lazyDefault() {
		if (properties.getLocalRoot() == null || properties.getLocalRoot().isBlank()) {
			throw new ApiException(409, "FILE.NO_DEFAULT_STORAGE", "기본 저장소가 없습니다.", 2);
		}
		Long actor = currentUser.pkOrNull();
		FileStorage existing = storages.findByStorageCode("DEFAULT").orElse(null);
		if (existing != null) {
			if (!existing.inUse()) {
				existing.activate(actor);
			}
			if (!"LOCAL".equals(existing.getStorageTypeCode())) {
				throw new ApiException(409, "FILE.NO_DEFAULT_STORAGE", "기본 저장소가 없습니다.", 2);
			}
			makeDefault(existing, actor);
			return existing;
		}
		FileStorage created = FileStorage.create("DEFAULT", "기본 저장소", "LOCAL", ".", actor);
		created.change("기본 저장소", "LOCAL", ".", null, null, null, null, null, actor);
		try {
			storages.saveAndFlush(created);
		}
		catch (DataIntegrityViolationException ex) {
			throw new ApiException(409, "FILE.STORAGE_DUPLICATE", "이미 있는 저장소 코드입니다.", 2);
		}
		makeDefault(created, actor);
		return created;
	}

	private void makeDefault(FileStorage storage, Long actor) {
		entityManager.createNativeQuery("""
				UPDATE file_storage
				SET default_yn = 'N', update_datetime = CURRENT_TIMESTAMP, update_user_pk = ?
				WHERE default_yn = 'Y' AND use_yn = 'Y' AND storage_pk <> ?
				""").setParameter(1, actor).setParameter(2, storage.getStoragePk()).executeUpdate();
		entityManager.flush();
		entityManager.refresh(storage);
		storage.markDefault(true, actor);
		storages.saveAndFlush(storage);
	}

	private void apply(FileStorage storage, String name, String type, String basePath, String connectionConfig, String secretRef,
			Integer maxFileSizeMb, String allowedExtensions, String description, Long actor) {
		String typeCode = type.trim();
		codes.require("STORAGE_TYPE", typeCode, "storageTypeCode");
		if (!codesKnown(typeCode)) {
			throw OrgExceptions.field("ORG.INVALID_CODE_VALUE", "사용할 수 없는 코드 값입니다.", "storageTypeCode");
		}
		String path = basePath.trim();
		FileRules.assertBasePath(path);
		if (!java.nio.file.Path.of(path).isAbsolute()) {
			if (properties.getLocalRoot() == null || properties.getLocalRoot().isBlank()) {
				throw new ApiException(400, "FILE.INVALID_PATH", "저장 경로가 올바르지 않습니다.", 2);
			}
			FileRules.resolveBase(properties.getLocalRoot(), path);
		}
		String config = OrgExceptions.trimToNull(connectionConfig);
		FileRules.assertConfig(config, objectMapper);
		String secret = OrgExceptions.trimToNull(secretRef);
		FileRules.assertSecretRef(secret);
		if (maxFileSizeMb != null && maxFileSizeMb < 1) {
			throw OrgExceptions.field("COMMON.VALIDATION_FAILED", "최대 크기는 1 이상이어야 합니다.", "maxFileSizeMb");
		}
		storage.change(name.trim(), typeCode, basePath.trim(), config, secret, maxFileSizeMb,
				FileRules.normalizeExtensions(allowedExtensions), OrgExceptions.trimToNull(description), actor);
	}

	private boolean codesKnown(String typeCode) {
		return typeCode != null && !typeCode.isBlank();
	}

	private FileStorage require(long storagePk) {
		return storages.findById(storagePk)
				.orElseThrow(() -> new ApiException(404, "FILE.STORAGE_NOT_FOUND", "저장소를 찾을 수 없습니다.", 2));
	}

	private String tenantCode() {
		String schema = com.tommy.common.global.multitenancy.TenantContext.schema();
		if (schema == null || schema.isBlank()) {
			throw new ApiException(404, "TENANT.NOT_FOUND", "테넌트를 찾을 수 없습니다.", 2);
		}
		try {
			Object value = entityManager.createNativeQuery("SELECT tenant_code FROM pmis.tenant WHERE schema_name = ?")
					.setParameter(1, schema).getSingleResult();
			return String.valueOf(value);
		}
		catch (RuntimeException ex) {
			return schema;
		}
	}

	private StorageResponse response(FileStorage storage, boolean detail) {
		return new StorageResponse(storage.getStoragePk(), storage.getStorageCode(), storage.getStorageName(),
				storage.getStorageTypeCode(), storage.getBasePath(), detail ? storage.getConnectionConfig() : null,
				storage.getSecretRef(), storage.getDefaultYn(), storage.getMaxFileSizeMb(), storage.getAllowedExtensions(),
				storage.getDescription(), storage.getUseYn(), storage.getCreateDatetime(), storage.getCreateUserPk(),
				storage.getUpdateDatetime(), storage.getUpdateUserPk());
	}

	private static Specification<FileStorage> filter(String keyword, String storageTypeCode, String useYn) {
		return (root, query, builder) -> {
			List<Predicate> predicates = new ArrayList<>();
			String text = OrgExceptions.trimToNull(keyword);
			if (text != null) {
				String pattern = "%" + text.toLowerCase() + "%";
				predicates.add(builder.or(builder.like(builder.lower(root.get("storageCode")), pattern),
						builder.like(builder.lower(root.get("storageName")), pattern)));
			}
			if (OrgExceptions.trimToNull(storageTypeCode) != null) {
				predicates.add(builder.equal(root.get("storageTypeCode"), storageTypeCode.trim()));
			}
			if (OrgExceptions.trimToNull(useYn) != null) {
				predicates.add(builder.equal(root.get("useYn"), useYn.trim()));
			}
			return predicates.isEmpty() ? builder.conjunction() : builder.and(predicates.toArray(Predicate[]::new));
		};
	}

	private static String yn(String value, String fallback) {
		String token = OrgExceptions.trimToNull(value);
		if (token == null) {
			return fallback;
		}
		if (!Set.of("Y", "N").contains(token)) {
			throw OrgExceptions.field("COMMON.VALIDATION_FAILED", "여부는 Y 또는 N 입니다.", "defaultYn");
		}
		return token;
	}

	public record PathBase(java.nio.file.Path path) {
	}

}
