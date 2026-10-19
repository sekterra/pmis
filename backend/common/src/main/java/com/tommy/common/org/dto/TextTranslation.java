package com.tommy.common.org.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TextTranslation(
		@NotBlank @Size(max = 20) String supportLanguageCode,
		@NotBlank @Size(max = 2000) String text) {
}
