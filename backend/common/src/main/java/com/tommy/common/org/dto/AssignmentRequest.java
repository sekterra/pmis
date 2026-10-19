package com.tommy.common.org.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AssignmentRequest(
		@NotBlank @Size(max = 50) String companyCode,
		@Size(max = 50) String deptCode,
		@Size(max = 50) String employeeNo,
		@NotBlank @Size(max = 30) String employmentTypeCode,
		@Size(max = 30) String positionCode,
		@Size(max = 30) String jobTitleCode,
		@NotBlank @Size(max = 30) String assignmentStatusCode,
		@Pattern(regexp = "Y|N") String primaryYn,
		@Pattern(regexp = "Y|N") String deptHeadYn,
		@NotNull LocalDate startDate,
		LocalDate endDate,
		@Size(max = 1000) String description) {
}
