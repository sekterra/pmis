package com.tommy.common.org.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

public record DeptResponse(
		Long deptPk,
		Long companyPk,
		String companyCode,
		Long parentDeptPk,
		String parentDeptCode,
		String deptCode,
		String deptName,
		String deptTypeCode,
		Integer levelNo,
		String deptPath,
		Integer sortOrder,
		LocalDate validFromDate,
		LocalDate validToDate,
		String description,
		String useYn,
		LocalDateTime createDatetime,
		Long createUserPk,
		LocalDateTime updateDatetime,
		Long updateUserPk,
		@JsonInclude(JsonInclude.Include.NON_NULL) List<TextTranslation> translations) {
}
