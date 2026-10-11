package com.tommy.common.i18n.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record NationRequest(
		@NotBlank @Pattern(regexp = "^[A-Z]{2}$") String nationCode,
		@Size(max = 3) String isoAlpha3Code,
		@Size(max = 3) String isoNumericCode,
		@NotBlank @Size(max = 200) String nationName,
		@NotBlank @Size(max = 20) String defaultLanguageCode,
		@Size(max = 10) String phoneCode,
		@Size(max = 3) String currencyCode,
		@Size(max = 50) String timezoneId,
		Integer sortOrder) {
}
