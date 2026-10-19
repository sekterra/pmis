package com.tommy.common.access.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserCreateRequest(
		@NotBlank @Size(max = 100) String loginId,
		@Size(max = 30) String accountTypeCode,
		Long personPk,
		@Size(max = 20) String supportLanguageCode,
		@Size(max = 50) String timezoneId,
		@Size(max = 1000) String description,
		List<String> roleCodes) {
}
