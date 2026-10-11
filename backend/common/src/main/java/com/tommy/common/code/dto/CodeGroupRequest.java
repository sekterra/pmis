package com.tommy.common.code.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CodeGroupRequest(
		@NotBlank @Size(max = 100) @Pattern(regexp = "^[A-Z][A-Z0-9_]*$") String codeGroupCode,
		@Size(max = 50) String solutionCode,
		@NotBlank @Size(max = 200) String codeGroupName,
		@Size(max = 1000) String description,
		@NotNull Boolean hierarchy,
		Integer codeValueLength,
		String extAttrSchema,
		Integer sortOrder,
		@Valid List<TranslationRequest> translations) {
}
