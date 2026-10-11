package com.tommy.common.i18n.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MessageTextRequest(
		@NotBlank @Size(max = 20) String supportLanguageCode,
		@NotBlank String messageText,
		@Size(max = 30) String translationStatusCode,
		Boolean machineTranslated) {
}
