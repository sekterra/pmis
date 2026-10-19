package com.tommy.common.org.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record AssignmentResponse(
		Long assignmentPk,
		Long personPk,
		Long companyPk,
		String companyCode,
		String companyName,
		Long deptPk,
		String deptCode,
		String deptName,
		String employeeNo,
		String employmentTypeCode,
		String positionCode,
		String jobTitleCode,
		String assignmentStatusCode,
		String primaryYn,
		String deptHeadYn,
		LocalDate startDate,
		LocalDate endDate,
		String description,
		String useYn,
		LocalDateTime createDatetime,
		Long createUserPk,
		LocalDateTime updateDatetime,
		Long updateUserPk) {
}
