package com.tommy.common.file;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tommy.common.file.dto.FileDtos.AttachmentRequest;
import com.tommy.common.file.dto.FileDtos.AttachmentResponse;
import com.tommy.common.file.dto.FileDtos.Download;
import com.tommy.common.file.entity.FileAttachment;
import com.tommy.common.file.entity.StoredFile;
import com.tommy.common.file.repository.FileAttachmentRepository;
import com.tommy.common.global.exception.ApiException;
import com.tommy.common.global.security.CurrentTenantUser;
import com.tommy.common.global.security.DbPermissionChecker;
import com.tommy.common.org.service.OrgCodes;
import com.tommy.common.org.support.OrgExceptions;

import jakarta.persistence.EntityManager;

@Service
public class AttachmentService {

	private static final Map<String, String> TARGETS = Map.of(
			"person", "PERSON",
			"company", "COMPANY",
			"dept", "DEPT",
			"team", "TEAM");

	private static final Map<String, String> NOT_FOUND = Map.of(
			"person", "PERSON.NOT_FOUND",
			"company", "COMPANY.NOT_FOUND",
			"dept", "DEPT.NOT_FOUND",
			"team", "TEAM.NOT_FOUND");

	private final FileAttachmentRepository attachments;

	private final FileService files;

	private final OrgCodes codes;

	private final DbPermissionChecker permissions;

	private final CurrentTenantUser currentUser;

	private final EntityManager entityManager;

	public AttachmentService(FileAttachmentRepository attachments, FileService files, OrgCodes codes, DbPermissionChecker permissions,
			CurrentTenantUser currentUser, EntityManager entityManager) {
		this.attachments = attachments;
		this.files = files;
		this.codes = codes;
		this.permissions = permissions;
		this.currentUser = currentUser;
		this.entityManager = entityManager;
	}

	@Transactional
	public AttachmentResponse create(AttachmentRequest request) {
		String table = request.targetTableName().trim().toLowerCase();
		String menu = TARGETS.get(table);
		if (menu == null) {
			throw new ApiException(400, "FILE.TARGET_NOT_ALLOWED", "첨부할 수 없는 대상입니다.", 2);
		}
		permissions.check(menu, "UPDATE");
		requireTarget(table, request.targetKey().trim());
		String category = OrgExceptions.trimToNull(request.attachmentCategoryCode());
		codes.require("ATTACHMENT_CATEGORY", category, "attachmentCategoryCode");
		StoredFile file = files.require(request.fileId());
		files.acceptForLink(file);
		String key = request.targetKey().trim();
		FileAttachment existing = attachments.findByTargetTableNameAndTargetKeyAndFile_FilePk(table, key, file.getFilePk()).orElse(null);
		Long actor = currentUser.pkOrNull();
		if (existing != null) {
			if (existing.inUse()) {
				throw new ApiException(409, "FILE.ATTACHMENT_DUPLICATE", "이미 연결된 파일입니다.", 2);
			}
			existing.revive(category, request.sortOrder(), OrgExceptions.trimToNull(request.description()), actor);
		}
		else {
			existing = FileAttachment.create(file, table, key, category, request.sortOrder(),
					OrgExceptions.trimToNull(request.description()), actor);
			attachments.save(existing);
		}
		files.activate(file);
		return response(existing);
	}

	@Transactional(readOnly = true)
	public List<AttachmentResponse> list(String targetTableName, String targetKey) {
		String table = requiredTable(targetTableName);
		permissions.check(TARGETS.get(table), "READ");
		return attachments.findByTargetTableNameAndTargetKeyAndUseYn(table, targetKey.trim(), "Y").stream()
				.sorted(Comparator.comparing(FileAttachment::getSortOrder, Comparator.nullsLast(Integer::compareTo))
						.thenComparing(FileAttachment::getAttachmentPk))
				.map(this::response)
				.toList();
	}

	@Transactional(readOnly = true)
	public Download download(long attachmentPk) {
		FileAttachment attachment = require(attachmentPk);
		if (!attachment.inUse()) {
			throw new ApiException(404, "FILE.NOT_FOUND", "파일을 찾을 수 없습니다.", 2);
		}
		permissions.check(TARGETS.get(attachment.getTargetTableName()), "READ");
		return files.streamStored(attachment.getFile().getFilePk());
	}

	@Transactional
	public void delete(long attachmentPk) {
		FileAttachment attachment = require(attachmentPk);
		if (!attachment.inUse()) {
			throw new ApiException(404, "FILE.NOT_FOUND", "파일을 찾을 수 없습니다.", 2);
		}
		permissions.check(TARGETS.get(attachment.getTargetTableName()), "UPDATE");
		attachment.deactivate(currentUser.pkOrNull());
	}

	private FileAttachment require(long attachmentPk) {
		return attachments.findById(attachmentPk)
				.orElseThrow(() -> new ApiException(404, "FILE.NOT_FOUND", "파일을 찾을 수 없습니다.", 2));
	}

	private String requiredTable(String targetTableName) {
		String table = targetTableName == null ? "" : targetTableName.trim().toLowerCase();
		if (!TARGETS.containsKey(table)) {
			throw new ApiException(400, "FILE.TARGET_NOT_ALLOWED", "첨부할 수 없는 대상입니다.", 2);
		}
		return table;
	}

	private void requireTarget(String table, String key) {
		long id;
		try {
			id = Long.parseLong(key);
		}
		catch (NumberFormatException ex) {
			throw OrgExceptions.of(404, NOT_FOUND.get(table), "대상을 찾을 수 없습니다.");
		}
		String column = switch (table) {
			case "person" -> "person_pk";
			case "company" -> "company_pk";
			case "dept" -> "dept_pk";
			default -> "team_pk";
		};
		Number count = (Number) entityManager.createNativeQuery(
				"SELECT count(*) FROM " + table + " WHERE " + column + " = ? AND use_yn = 'Y'")
				.setParameter(1, id)
				.getSingleResult();
		if (count.longValue() == 0) {
			throw OrgExceptions.of(404, NOT_FOUND.get(table), "대상을 찾을 수 없습니다.");
		}
	}

	private AttachmentResponse response(FileAttachment attachment) {
		StoredFile file = attachment.getFile();
		return new AttachmentResponse(attachment.getAttachmentPk(), file.getFilePk(), file.getOriginalFileName(), file.getFileSize(),
				file.getMimeType(), file.getFileExtension(), file.getChecksumSha256(), file.getFileStatusCode(),
				attachment.getAttachmentCategoryCode(), attachment.getSortOrder(), attachment.getDescription(), attachment.getUseYn());
	}

}
