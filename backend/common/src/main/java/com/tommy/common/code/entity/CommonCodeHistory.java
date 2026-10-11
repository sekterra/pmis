package com.tommy.common.code.entity;

import java.time.LocalDateTime;

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
@Table(name = "common_code_history")
public class CommonCodeHistory {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "common_code_history_seq_gen")
	@SequenceGenerator(name = "common_code_history_seq_gen", sequenceName = "common_code_history_seq", allocationSize = 1)
	private Long historyPk;

	@Column(nullable = false)
	private Long codePk;

	@Column(nullable = false, length = 100)
	private String codeGroupCode;

	@Column(nullable = false, length = 100)
	private String codeValue;

	@Column(nullable = false, length = 1)
	private String changeTypeCode;

	@Column(columnDefinition = "text")
	private String beforeData;

	@Column(columnDefinition = "text")
	private String afterData;

	@Column(length = 500)
	private String changeReason;

	@Column(nullable = false)
	private LocalDateTime changeDatetime;

	private Long changeUserPk;

	public static CommonCodeHistory record(Long codePk, String codeGroupCode, String codeValue, String changeTypeCode,
			String beforeData, String afterData, String changeReason) {
		CommonCodeHistory history = new CommonCodeHistory();
		history.codePk = codePk;
		history.codeGroupCode = codeGroupCode;
		history.codeValue = codeValue;
		history.changeTypeCode = changeTypeCode;
		history.beforeData = beforeData;
		history.afterData = afterData;
		history.changeReason = changeReason;
		history.changeDatetime = LocalDateTime.now();
		return history;
	}

}
