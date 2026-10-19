package com.tommy.common.org.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

public record PersonResponse(
		Long personPk,
		String personName,
		String email,
		String mobilePhoneNo,
		String officePhoneNo,
		Long profileFilePk,
		boolean hasProfileImage,
		String description,
		String useYn,
		LocalDateTime createDatetime,
		Long createUserPk,
		LocalDateTime updateDatetime,
		Long updateUserPk,
		@JsonInclude(JsonInclude.Include.NON_NULL) List<TextTranslation> translations,
		PrimaryAssignmentSummary primaryAssignment,
		@JsonInclude(JsonInclude.Include.NON_NULL) List<AssignmentResponse> assignments) {
}
