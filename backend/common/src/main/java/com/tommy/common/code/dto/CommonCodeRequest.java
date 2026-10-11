package com.tommy.common.code.dto;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommonCodeRequest(
		@NotBlank @Size(max = 100) String groupCode,
		@NotBlank @Size(max = 100) String codeValue,
		@NotBlank @Size(max = 200) String codeName,
		@Size(max = 100) String parentCodeValue,
		Integer sortOrder,
		@Size(max = 200) String extAttr1,
		@Size(max = 200) String extAttr2,
		@Size(max = 200) String extAttr3,
		@Size(max = 200) String extAttr4,
		@Size(max = 200) String extAttr5,
		LocalDate validFromDate,
		LocalDate validToDate,
		@Size(max = 500) String changeReason,
		@Valid List<TranslationRequest> translations) {
}
