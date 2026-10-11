package com.tommy.common.i18n.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SupportLanguageRequest(
		@NotBlank @Size(max = 20) @Pattern(regexp = "^[A-Za-z]{2,3}(-[A-Za-z0-9]{2,8})*$") String supportLanguageCode,
		@NotBlank @Size(max = 100) String languageName,
		@Size(max = 100) String nativeName,
		@Size(max = 10) String isoLanguageCode,
		@Size(max = 20) String fallbackLanguageCode,
		@NotBlank @Pattern(regexp = "LTR|RTL") String textDirectionCode,
		@Size(max = 50) String dateFormat,
		@Size(max = 50) String datetimeFormat,
		@Size(max = 50) String numberFormat,
		@NotNull Boolean defaultLanguage,
		Integer sortOrder) {
}
