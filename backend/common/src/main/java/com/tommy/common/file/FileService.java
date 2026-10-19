package com.tommy.common.file;

import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.tommy.common.file.dto.FileDtos.AttachmentResponse;
import com.tommy.common.file.dto.FileDtos.CleanupResponse;
import com.tommy.common.file.dto.FileDtos.Download;
import com.tommy.common.file.dto.FileDtos.FileResponse;
import com.tommy.common.file.entity.FileAttachment;
import com.tommy.common.file.entity.FileStorage;
import com.tommy.common.file.entity.StoredFile;
import com.tommy.common.file.repository.FileAttachmentRepository;
import com.tommy.common.file.repository.StoredFileRepository;
import com.tommy.common.global.exception.ApiException;
import com.tommy.common.global.response.PageResponse;
import com.tommy.common.global.security.CurrentTenantUser;
import com.tommy.common.global.security.DbPermissionChecker;
import com.tommy.common.org.entity.Person;
import com.tommy.common.org.repository.PersonRepository;
import com.tommy.common.org.support.OrgExceptions;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.Predicate;

@Service
public class FileService {

	private static final Logger log = LoggerFactory.getLogger(FileService.class);

	private final StoredFileRepository files;

	private final FileAttachmentRepository attachments;

	private final PersonRepository people;

	private final FileStorageService storages;

	private final LocalFileStore store;

	private final FileProperties properties;

	private final CurrentTenantUser currentUser;

	private final DbPermissionChecker permissions;

	private final EntityManager entityManager;

	public FileService(StoredFileRepository files, FileAttachmentRepository attachments, PersonRepository people,
			FileStorageService storages, LocalFileStore store, FileProperties properties, CurrentTenantUser currentUser,
			DbPermissionChecker permissions, EntityManager entityManager) {
		this.files = files;
		this.attachments = attachments;
		this.people = people;
		this.storages = storages;
		this.store = store;
		this.properties = properties;
		this.currentUser = currentUser;
		this.permissions = permissions;
		this.entityManager = entityManager;
	}

	@Transactional
	public FileResponse upload(MultipartFile multipart, String storageCode) {
		if (multipart == null || multipart.isEmpty()) {
			throw OrgExceptions.field("COMMON.VALIDATION_FAILED", "파일이 필요합니다.", "file");
		}
		byte[] bytes;
		try {
			bytes = multipart.getBytes();
		}
		catch (java.io.IOException ex) {
			throw new ApiException(400, "COMMON.VALIDATION_FAILED", "파일을 읽지 못했습니다.", 2);
		}
		FileStorage storage = storages.requireWritable(storageCode);
		FileRules.assertSize(bytes.length, storage.getMaxFileSizeMb(), properties.getMaxSizeMb());
		String name = FileRules.sanitizeName(multipart.getOriginalFilename());
		String allowed = storage.getAllowedExtensions() != null ? storage.getAllowedExtensions() : properties.getAllowedExtensions();
		FileRules.assertExtensionAllowed(name, allowed);
		String extension = FileRules.extensionOf(name);
		FileRules.assertMime(extension, bytes);
		long actor = actor();
		LocalDateTime now = LocalDateTime.now();
		String key = FileRules.storageKey(now);
		java.nio.file.Path path = storages.locate(storage, key).path();
		store.write(path, bytes);
		StoredFile file = StoredFile.upload(storage, key, name, extension, FileRules.mimeType(extension, bytes), bytes.length,
				sha256(bytes), actor, now);
		try {
			files.saveAndFlush(file);
		}
		catch (RuntimeException ex) {
			store.deleteQuiet(path);
			throw ex;
		}
		return response(file, false);
	}

	@Transactional(readOnly = true)
	public PageResponse<FileResponse> list(String keyword, String fileStatusCode, Long uploadUserPk, LocalDate from, LocalDate to,
			String useYn, Pageable pageable) {
		permissions.check("FILE", "READ");
		Page<StoredFile> page = files.findAll(filter(keyword, fileStatusCode, uploadUserPk, from, to, useYn), pageable);
		return new PageResponse<>(page.getContent().stream().map(file -> response(file, false)).toList(), page.getTotalElements(),
				pageable.getPageNumber() + 1, pageable.getPageSize());
	}

	@Transactional(readOnly = true)
	public FileResponse get(long filePk) {
		permissions.check("FILE", "READ");
		StoredFile file = require(filePk);
		return response(file, true);
	}

	@Transactional(readOnly = true)
	public Download download(long filePk) {
		StoredFile file = require(filePk);
		if (!canRead(file)) {
			throw new ApiException(403, "AUTH.FORBIDDEN", "이 기능을 사용할 권한이 없습니다.", 2);
		}
		return stream(file);
	}

	@Transactional(readOnly = true)
	public Download streamStored(long filePk) {
		StoredFile file = require(filePk);
		if (!file.inUse() || "DELETED".equals(file.getFileStatusCode())) {
			throw new ApiException(404, "FILE.NOT_FOUND", "파일을 찾을 수 없습니다.", 2);
		}
		return stream(file);
	}

	@Transactional
	public void delete(long filePk) {
		permissions.check("FILE", "DELETE");
		StoredFile file = require(filePk);
		if (!file.inUse() || "DELETED".equals(file.getFileStatusCode())) {
			throw new ApiException(404, "FILE.NOT_FOUND", "파일을 찾을 수 없습니다.", 2);
		}
		if (attachments.existsByFile_FilePkAndUseYn(filePk, "Y") || people.existsByProfileFilePk(filePk)) {
			throw new ApiException(409, "FILE.IN_USE", "사용 중인 파일입니다.", 2);
		}
		file.markDeleted(LocalDateTime.now().plusDays(properties.getDeleteRetentionDays()), actor());
	}

	@Transactional
	public FileResponse restore(long filePk) {
		permissions.check("FILE", "UPDATE");
		StoredFile file = require(filePk);
		if (!"DELETED".equals(file.getFileStatusCode())) {
			throw new ApiException(409, "FILE.NOT_DELETED", "삭제된 파일이 아닙니다.", 2);
		}
		java.nio.file.Path path = storages.locate(file.getStorage(), file.getStorageKey()).path();
		if (!store.exists(path)) {
			log.warn("file missing on disk filePk={}", filePk);
			throw new ApiException(409, "FILE.PURGED", "파일이 이미 지워졌습니다.", 2);
		}
		boolean active = attachments.existsByFile_FilePk(filePk) || people.existsByProfileFilePk(filePk);
		file.restore(active ? "ACTIVE" : "TEMP", actor());
		return response(file, true);
	}

	@Transactional
	public CleanupResponse cleanup() {
		permissions.check("FILE", "DELETE");
		LocalDateTime now = LocalDateTime.now();
		int expired = 0;
		int purged = 0;
		int failures = 0;
		List<StoredFile> temps = entityManager.createQuery("""
				SELECT f FROM StoredFile f
				WHERE f.fileStatusCode = 'TEMP' AND f.useYn = 'Y' AND f.uploadDatetime < :cutoff
				""", StoredFile.class).setParameter("cutoff", now.minusHours(properties.getTempTtlHours())).getResultList();
		for (StoredFile file : temps) {
			if (!removeDisk(file)) {
				failures++;
				continue;
			}
			file.markDeleted(now, actor());
			expired++;
		}
		List<StoredFile> due = entityManager.createQuery("""
				SELECT f FROM StoredFile f
				WHERE f.fileStatusCode = 'DELETED' AND f.deleteScheduledDatetime IS NOT NULL AND f.deleteScheduledDatetime <= :now
				""", StoredFile.class).setParameter("now", now).getResultList();
		for (StoredFile file : due) {
			if (!removeDisk(file)) {
				failures++;
				continue;
			}
			purged++;
		}
		return new CleanupResponse(expired, purged, failures);
	}

	@Transactional
	public void assignProfile(long personPk, long filePk) {
		permissions.check("PERSON", "UPDATE");
		Person person = people.findById(personPk)
				.orElseThrow(() -> OrgExceptions.of(404, "PERSON.NOT_FOUND", "사람을 찾을 수 없습니다."));
		if (!person.inUse()) {
			throw OrgExceptions.of(409, "PERSON.INACTIVE", "사용 중이 아닌 사람입니다.");
		}
		StoredFile file = require(filePk);
		acceptForLink(file);
		if (!FileRules.imageExtension(file.getFileExtension())) {
			throw new ApiException(400, "FILE.NOT_IMAGE", "이미지 파일만 프로필로 쓸 수 있습니다.", 2);
		}
		person.assignProfile(filePk, actor());
		file.activate(actor());
	}

	@Transactional
	public void clearProfile(long personPk) {
		permissions.check("PERSON", "UPDATE");
		Person person = people.findById(personPk)
				.orElseThrow(() -> OrgExceptions.of(404, "PERSON.NOT_FOUND", "사람을 찾을 수 없습니다."));
		person.assignProfile(null, actor());
	}

	@Transactional(readOnly = true)
	public Download profile(long personPk) {
		if (!permissions.allows("PERSON", "READ")) {
			throw new ApiException(404, "FILE.NOT_FOUND", "파일을 찾을 수 없습니다.", 2);
		}
		Person person = people.findById(personPk).orElse(null);
		if (person == null || person.getProfileFilePk() == null) {
			throw new ApiException(404, "FILE.NOT_FOUND", "파일을 찾을 수 없습니다.", 2);
		}
		StoredFile file = files.findById(person.getProfileFilePk()).orElse(null);
		if (file == null || !file.inUse() || "DELETED".equals(file.getFileStatusCode())) {
			throw new ApiException(404, "FILE.NOT_FOUND", "파일을 찾을 수 없습니다.", 2);
		}
		Download download = stream(file);
		String inline = download.disposition().replace("attachment;", "inline;");
		return new Download(download.bytes(), download.mimeType(), inline);
	}

	public void acceptForLink(StoredFile file) {
		if (!file.inUse() || "DELETED".equals(file.getFileStatusCode())) {
			throw new ApiException(409, "FILE.NOT_ATTACHABLE", "첨부할 수 없는 파일입니다.", 2);
		}
		if ("TEMP".equals(file.getFileStatusCode())) {
			Long actor = currentUser.pkOrNull();
			if (actor == null || !actor.equals(file.getUploadUserPk())) {
				throw new ApiException(403, "FILE.NOT_OWNER", "임시 파일은 올린 사람만 연결할 수 있습니다.", 2);
			}
		}
		else if (!"ACTIVE".equals(file.getFileStatusCode())) {
			throw new ApiException(409, "FILE.NOT_ATTACHABLE", "첨부할 수 없는 파일입니다.", 2);
		}
	}

	public StoredFile require(long filePk) {
		return files.findById(filePk).orElseThrow(() -> new ApiException(404, "FILE.NOT_FOUND", "파일을 찾을 수 없습니다.", 2));
	}

	public void activate(StoredFile file) {
		file.activate(actor());
	}

	private boolean canRead(StoredFile file) {
		if (!file.inUse() || "DELETED".equals(file.getFileStatusCode())) {
			throw new ApiException(404, "FILE.NOT_FOUND", "파일을 찾을 수 없습니다.", 2);
		}
		if (permissions.allows("FILE", "READ")) {
			return true;
		}
		Long actor = currentUser.pkOrNull();
		return "TEMP".equals(file.getFileStatusCode()) && actor != null && actor.equals(file.getUploadUserPk());
	}

	private Download stream(StoredFile file) {
		java.nio.file.Path path = storages.locate(file.getStorage(), file.getStorageKey()).path();
		if (!store.exists(path)) {
			log.warn("file missing on disk filePk={}", file.getFilePk());
			throw new ApiException(404, "FILE.NOT_FOUND", "파일을 찾을 수 없습니다.", 2);
		}
		byte[] bytes = store.read(path);
		if (bytes == null) {
			log.warn("file missing on disk filePk={}", file.getFilePk());
			throw new ApiException(404, "FILE.NOT_FOUND", "파일을 찾을 수 없습니다.", 2);
		}
		String mime = file.getMimeType() == null || file.getMimeType().isBlank() ? "application/octet-stream" : file.getMimeType();
		return new Download(bytes, mime, FileRules.disposition(file.getOriginalFileName()));
	}

	private boolean removeDisk(StoredFile file) {
		try {
			java.nio.file.Path path = storages.locate(file.getStorage(), file.getStorageKey()).path();
			return store.delete(path);
		}
		catch (RuntimeException ex) {
			log.warn("file disk delete failed filePk={}", file.getFilePk());
			return false;
		}
	}

	private FileResponse response(StoredFile file, boolean detail) {
		List<AttachmentResponse> links = null;
		if (detail) {
			links = attachments.findByFile_FilePkAndUseYn(file.getFilePk(), "Y").stream().map(this::attachment).toList();
		}
		return new FileResponse(file.getFilePk(), file.getOriginalFileName(), file.getFileSize(), file.getMimeType(),
				file.getFileExtension(), file.getChecksumSha256(), file.getFileStatusCode(), file.getUploadUserPk(),
				file.getUploadDatetime(), file.getUseYn(), links);
	}

	private AttachmentResponse attachment(FileAttachment attachment) {
		StoredFile file = attachment.getFile();
		return new AttachmentResponse(attachment.getAttachmentPk(), file.getFilePk(), file.getOriginalFileName(), file.getFileSize(),
				file.getMimeType(), file.getFileExtension(), file.getChecksumSha256(), file.getFileStatusCode(),
				attachment.getAttachmentCategoryCode(), attachment.getSortOrder(), attachment.getDescription(), attachment.getUseYn());
	}

	private long actor() {
		Long pk = currentUser.pkOrNull();
		if (pk == null) {
			throw new ApiException(401, "AUTH.UNAUTHORIZED", "인증이 필요합니다.", 2);
		}
		return pk;
	}

	private static String sha256(byte[] bytes) {
		try {
			return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
		}
		catch (java.security.NoSuchAlgorithmException ex) {
			throw new IllegalStateException(ex);
		}
	}

	private Specification<StoredFile> filter(String keyword, String fileStatusCode, Long uploadUserPk, LocalDate from, LocalDate to,
			String useYn) {
		return (root, query, builder) -> {
			List<Predicate> predicates = new ArrayList<>();
			String text = OrgExceptions.trimToNull(keyword);
			if (text != null) {
				predicates.add(builder.like(builder.lower(root.get("originalFileName")), "%" + text.toLowerCase() + "%"));
			}
			if (OrgExceptions.trimToNull(fileStatusCode) != null) {
				predicates.add(builder.equal(root.get("fileStatusCode"), fileStatusCode.trim()));
			}
			if (uploadUserPk != null) {
				predicates.add(builder.equal(root.get("uploadUserPk"), uploadUserPk));
			}
			if (from != null) {
				predicates.add(builder.greaterThanOrEqualTo(root.get("uploadDatetime"), from.atStartOfDay()));
			}
			if (to != null) {
				predicates.add(builder.lessThan(root.get("uploadDatetime"), to.plusDays(1).atStartOfDay()));
			}
			if (OrgExceptions.trimToNull(useYn) != null) {
				predicates.add(builder.equal(root.get("useYn"), useYn.trim()));
			}
			return predicates.isEmpty() ? builder.conjunction() : builder.and(predicates.toArray(Predicate[]::new));
		};
	}

}
