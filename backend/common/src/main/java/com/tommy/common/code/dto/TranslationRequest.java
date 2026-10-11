package com.tommy.common.code.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TranslationRequest(
		@NotBlank @Size(max = 20) String supportLanguageCode,
		@NotBlank @Size(max = 200) String name,
		@Size(max = 1000) String description) {
}
