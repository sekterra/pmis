package com.tommy.common.access.dto;

import java.util.List;

import com.tommy.common.org.dto.TextTranslation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RoleRequest(
		@NotBlank @Pattern(regexp = "^[A-Z][A-Z0-9_]{1,49}$") String roleCode,
		@NotBlank @Size(max = 200) String roleName,
		Integer sortOrder,
		@Size(max = 1000) String description,
		@Valid List<TextTranslation> translations) {
}
