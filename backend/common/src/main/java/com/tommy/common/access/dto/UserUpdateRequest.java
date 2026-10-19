package com.tommy.common.access.dto;

import jakarta.validation.constraints.Size;

public record UserUpdateRequest(
		Long personPk,
		Boolean clearPerson,
		@Size(max = 20) String supportLanguageCode,
		@Size(max = 50) String timezoneId,
		@Size(max = 1000) String description) {
}
