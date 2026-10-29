package com.tommy.common.org.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record DeptHistoryResponse(
		Long historyPk,
		Long deptPk,
		String changeTypeCode,
		LocalDate effectiveDate,
		String beforeDeptName,
		String afterDeptName,
		Long beforeParentDeptPk,
		String beforeParentDeptName,
		Long afterParentDeptPk,
		String afterParentDeptName,
		String changeReason,
		LocalDateTime changeDatetime,
		Long changeUserPk) {
}
