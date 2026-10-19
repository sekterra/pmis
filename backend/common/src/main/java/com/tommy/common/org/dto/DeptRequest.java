package com.tommy.common.org.dto;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DeptRequest(
		@NotBlank @Size(max = 50) String companyCode,
		@Size(max = 50) String parentDeptCode,
		@NotBlank @Size(max = 50) String deptCode,
		@NotBlank @Size(max = 200) String deptName,
		@NotBlank @Size(max = 30) String deptTypeCode,
		Integer sortOrder,
		LocalDate validFromDate,
		LocalDate validToDate,
		@Size(max = 1000) String description,
		@Valid List<TextTranslation> translations) {
}
