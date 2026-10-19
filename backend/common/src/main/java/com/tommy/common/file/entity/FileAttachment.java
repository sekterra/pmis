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
@Table(name = "file_attachment")
public class FileAttachment {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "file_attachment_seq_gen")
	@SequenceGenerator(name = "file_attachment_seq_gen", sequenceName = "file_attachment_seq", allocationSize = 1)
	private Long attachmentPk;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "file_pk", nullable = false)
	private StoredFile file;

	@Column(nullable = false, length = 100)
	private String targetTableName;

	@Column(nullable = false, length = 100)
	private String targetKey;

	@Column(length = 30)
	private String attachmentCategoryCode;

	private Integer sortOrder;

	@Column(length = 500)
	private String description;

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(nullable = false, length = 1)
	private String useYn;

	private LocalDateTime createDatetime;

	private Long createUserPk;

	private LocalDateTime updateDatetime;

	private Long updateUserPk;

	public static FileAttachment create(StoredFile file, String targetTableName, String targetKey, String category, Integer sortOrder,
			String description, Long userPk) {
		FileAttachment attachment = new FileAttachment();
		attachment.file = file;
		attachment.targetTableName = targetTableName;
		attachment.targetKey = targetKey;
		attachment.attachmentCategoryCode = category;
		attachment.sortOrder = sortOrder;
		attachment.description = description;
		attachment.useYn = "Y";
		attachment.createDatetime = LocalDateTime.now();
		attachment.createUserPk = userPk;
		attachment.updateDatetime = attachment.createDatetime;
		attachment.updateUserPk = userPk;
		return attachment;
	}

	public void revive(String category, Integer sortOrder, String description, Long userPk) {
		this.attachmentCategoryCode = category;
		this.sortOrder = sortOrder;
		this.description = description;
		this.useYn = "Y";
		touch(userPk);
	}

	public void deactivate(Long userPk) {
		this.useYn = "N";
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
