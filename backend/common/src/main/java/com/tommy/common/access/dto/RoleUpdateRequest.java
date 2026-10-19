package com.tommy.common.access.dto;

import java.util.List;

import com.tommy.common.org.dto.TextTranslation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RoleUpdateRequest(
		@NotBlank @Size(max = 200) String roleName,
		Integer sortOrder,
		@Size(max = 1000) String description,
		@Valid List<TextTranslation> translations) {
}
