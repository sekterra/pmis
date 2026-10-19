package com.tommy.common.file.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class FileDtos {

	private FileDtos() {
	}

	public record StorageRequest(
			@NotBlank @Size(max = 50) String storageCode,
			@NotBlank @Size(max = 200) String storageName,
			@NotBlank @Size(max = 30) String storageTypeCode,
			@NotBlank @Size(max = 500) String basePath,
			String connectionConfig,
			@Size(max = 200) String secretRef,
			String defaultYn,
			Integer maxFileSizeMb,
			@Size(max = 500) String allowedExtensions,
			@Size(max = 1000) String description) {
	}

	public record StorageUpdateRequest(
			@NotBlank @Size(max = 200) String storageName,
			@NotBlank @Size(max = 30) String storageTypeCode,
			@NotBlank @Size(max = 500) String basePath,
			String connectionConfig,
			@Size(max = 200) String secretRef,
			String defaultYn,
			Integer maxFileSizeMb,
			@Size(max = 500) String allowedExtensions,
			@Size(max = 1000) String description) {
	}

	public record StorageResponse(
			Long storagePk,
			String storageCode,
			String storageName,
			String storageTypeCode,
			String basePath,
			@JsonInclude(JsonInclude.Include.NON_NULL) String connectionConfig,
			String secretRef,
			String defaultYn,
			Integer maxFileSizeMb,
			String allowedExtensions,
			String description,
			String useYn,
			LocalDateTime createDatetime,
			Long createUserPk,
			LocalDateTime updateDatetime,
			Long updateUserPk) {
	}

	public record FileResponse(
			Long filePk,
			String originalFileName,
			Long fileSize,
			String mimeType,
			String fileExtension,
			String checksumSha256,
			String fileStatusCode,
			Long uploadUserPk,
			LocalDateTime uploadDatetime,
			String useYn,
			@JsonInclude(JsonInclude.Include.NON_NULL) List<AttachmentResponse> attachments) {
	}

	public record AttachmentRequest(
			@NotNull Long fileId,
			@NotBlank @Size(max = 100) String targetTableName,
			@NotBlank @Size(max = 100) String targetKey,
			@Size(max = 30) String attachmentCategoryCode,
			Integer sortOrder,
			@Size(max = 500) String description) {
	}

	public record AttachmentResponse(
			Long attachmentPk,
			Long filePk,
			String originalFileName,
			Long fileSize,
			String mimeType,
			String fileExtension,
			String checksumSha256,
			String fileStatusCode,
			String attachmentCategoryCode,
			Integer sortOrder,
			String description,
			String useYn) {
	}

	public record CleanupResponse(int expiredTempCount, int purgedCount, int diskFailureCount) {
	}

	public record ProfileRequest(@NotNull Long fileId) {
	}

	public record Download(byte[] bytes, String mimeType, String disposition) {
	}

}
