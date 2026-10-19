package com.tommy.common.file.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "file_storage")
public class FileStorage {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "file_storage_seq_gen")
	@SequenceGenerator(name = "file_storage_seq_gen", sequenceName = "file_storage_seq", allocationSize = 1)
	private Long storagePk;

	@Column(nullable = false, length = 50, updatable = false)
	private String storageCode;

	@Column(nullable = false, length = 200)
	private String storageName;

	@Column(nullable = false, length = 30)
	private String storageTypeCode;

	@Column(nullable = false, length = 500)
	private String basePath;

	@Column(columnDefinition = "text")
	private String connectionConfig;

	@Column(length = 200)
	private String secretRef;

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(nullable = false, length = 1)
	private String defaultYn;

	private Integer maxFileSizeMb;

	@Column(length = 500)
	private String allowedExtensions;

	@Column(length = 1000)
	private String description;

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(nullable = false, length = 1)
	private String useYn;

	private LocalDateTime createDatetime;

	private Long createUserPk;

	private LocalDateTime updateDatetime;

	private Long updateUserPk;

	public static FileStorage create(String storageCode, String storageName, String storageTypeCode, String basePath, Long userPk) {
		FileStorage storage = new FileStorage();
		storage.storageCode = storageCode;
		storage.storageName = storageName;
		storage.storageTypeCode = storageTypeCode;
		storage.basePath = basePath;
		storage.defaultYn = "N";
		storage.useYn = "Y";
		storage.createDatetime = LocalDateTime.now();
		storage.createUserPk = userPk;
		storage.updateDatetime = storage.createDatetime;
		storage.updateUserPk = userPk;
		return storage;
	}

	public void change(String storageName, String storageTypeCode, String basePath, String connectionConfig, String secretRef,
			Integer maxFileSizeMb, String allowedExtensions, String description, Long userPk) {
		this.storageName = storageName;
		this.storageTypeCode = storageTypeCode;
		this.basePath = basePath;
		this.connectionConfig = connectionConfig;
		this.secretRef = secretRef;
		this.maxFileSizeMb = maxFileSizeMb;
		this.allowedExtensions = allowedExtensions;
		this.description = description;
		touch(userPk);
	}

	public void markDefault(boolean value, Long userPk) {
		this.defaultYn = value ? "Y" : "N";
		touch(userPk);
	}

	public void deactivate(Long userPk) {
		this.useYn = "N";
		touch(userPk);
	}

	public void activate(Long userPk) {
		this.useYn = "Y";
		touch(userPk);
	}

	public boolean inUse() {
		return "Y".equals(useYn);
	}

	public boolean defaultStorage() {
		return "Y".equals(defaultYn);
	}

	private void touch(Long userPk) {
		this.updateDatetime = LocalDateTime.now();
		this.updateUserPk = userPk;
	}

}
