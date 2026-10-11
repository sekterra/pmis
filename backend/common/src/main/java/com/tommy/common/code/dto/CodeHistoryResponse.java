package com.tommy.common.code.dto;

import java.time.LocalDateTime;

public record CodeHistoryResponse(
		Long historyPk,
		Long codePk,
		String codeGroupCode,
		String codeValue,
		String changeTypeCode,
		String beforeData,
		String afterData,
		String changeReason,
		LocalDateTime changeDatetime,
		Long changeUserPk) {
}
