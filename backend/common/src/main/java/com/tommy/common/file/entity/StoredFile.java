package com.tommy.common.file.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "file")
public class StoredFile {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "file_seq_gen")
	@SequenceGenerator(name = "file_seq_gen", sequenceName = "file_seq", allocationSize = 1)
	private Long filePk;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "storage_pk", nullable = false)
	private FileStorage storage;

	@Column(nullable = false, length = 1000)
	private String storageKey;

	@Column(nullable = false, length = 300)
	private String originalFileName;

	@Column(length = 20)
	private String fileExtension;

	@Column(length = 150)
	private String mimeType;

	@Column(nullable = false)
	private Long fileSize;

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(length = 64)
	private String checksumSha256;

	@Column(nullable = false, length = 30)
	private String fileStatusCode;

	private Long uploadUserPk;

	@Column(nullable = false)
	private LocalDateTime uploadDatetime;

	private LocalDateTime deleteScheduledDatetime;

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(nullable = false, length = 1)
	private String useYn;

	private LocalDateTime createDatetime;

	private Long createUserPk;

	private LocalDateTime updateDatetime;

	private Long updateUserPk;

	public static StoredFile upload(FileStorage storage, String storageKey, String originalFileName, String fileExtension,
			String mimeType, long fileSize, String checksumSha256, Long userPk, LocalDateTime now) {
		StoredFile file = new StoredFile();
		file.storage = storage;
		file.storageKey = storageKey;
		file.originalFileName = originalFileName;
		file.fileExtension = fileExtension;
		file.mimeType = mimeType;
		file.fileSize = fileSize;
		file.checksumSha256 = checksumSha256;
		file.fileStatusCode = "TEMP";
		file.uploadUserPk = userPk;
		file.uploadDatetime = now;
		file.useYn = "Y";
		file.createDatetime = now;
		file.createUserPk = userPk;
		file.updateDatetime = now;
		file.updateUserPk = userPk;
		return file;
	}

	public void activate(Long userPk) {
		this.fileStatusCode = "ACTIVE";
		this.useYn = "Y";
		this.deleteScheduledDatetime = null;
		touch(userPk);
	}

	public void markDeleted(LocalDateTime scheduled, Long userPk) {
		this.useYn = "N";
		this.fileStatusCode = "DELETED";
		this.deleteScheduledDatetime = scheduled;
		touch(userPk);
	}

	public void markPurged(Long userPk) {
		this.deleteScheduledDatetime = null;
		touch(userPk);
	}

	public void restore(String status, Long userPk) {
		this.useYn = "Y";
		this.fileStatusCode = status;
		this.deleteScheduledDatetime = null;
		touch(userPk);
	}

	public boolean inUse() {
		return "Y".equals(useYn);
	}

	private void touch(Long userPk) {
		this.updateDatetime = LocalDateTime.now();
		this.updateUserPk = userPk;
	}

}
