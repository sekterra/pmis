package com.tommy.common.access.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tommy.common.org.dto.TextTranslation;

public record RoleResponse(
		Long rolePk,
		String roleCode,
		String roleName,
		Integer sortOrder,
		String description,
		String useYn,
		Long userCount,
		LocalDateTime createDatetime,
		Long createUserPk,
		LocalDateTime updateDatetime,
		Long updateUserPk,
		@JsonInclude(JsonInclude.Include.NON_NULL) List<TextTranslation> translations) {
}
