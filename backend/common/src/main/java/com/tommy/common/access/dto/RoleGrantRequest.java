package com.tommy.common.access.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RoleGrantRequest(
		@NotBlank @Size(max = 50) String roleCode,
		LocalDate validFromDate,
		LocalDate validToDate) {
}
