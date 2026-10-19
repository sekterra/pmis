package com.tommy.common.team.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tommy.common.org.dto.TextTranslation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class TeamDtos {

	private TeamDtos() {
	}

	public record TeamRequest(
			@NotBlank @Size(max = 50) String teamCode,
			@NotBlank @Size(max = 200) String teamName,
			@NotBlank @Size(max = 30) String teamTypeCode,
			LocalDate startDate,
			LocalDate endDate,
			Integer sortOrder,
			@Size(max = 1000) String description,
			@Valid List<TextTranslation> translations) {
	}

	public record TeamUpdateRequest(
			@NotBlank @Size(max = 200) String teamName,
			@NotBlank @Size(max = 30) String teamTypeCode,
			LocalDate startDate,
			LocalDate endDate,
			Integer sortOrder,
			@Size(max = 1000) String description,
			@Valid List<TextTranslation> translations) {
	}

	public record TeamResponse(
			Long teamPk,
			String teamCode,
			String teamName,
			String teamTypeCode,
			LocalDate startDate,
			LocalDate endDate,
			Integer sortOrder,
			String description,
			String useYn,
			long memberCount,
			LocalDateTime createDatetime,
			Long createUserPk,
			LocalDateTime updateDatetime,
			Long updateUserPk,
			@JsonInclude(JsonInclude.Include.NON_NULL) List<TextTranslation> translations) {
	}

	public record MemberRequest(
			@NotNull Long personPk,
			@NotBlank @Size(max = 30) String teamRoleCode,
			@NotNull LocalDate startDate,
			LocalDate endDate,
			@Size(max = 500) String description) {
	}

	public record MemberUpdateRequest(
			@NotBlank @Size(max = 30) String teamRoleCode,
			@NotNull LocalDate startDate,
			LocalDate endDate,
			@Size(max = 500) String description) {
	}

	public record MemberEndRequest(LocalDate endDate) {
	}

	public record MemberResponse(
			Long teamMemberPk,
			Long teamPk,
			Long personPk,
			String personName,
			String teamRoleCode,
			LocalDate startDate,
			LocalDate endDate,
			String description,
			String useYn,
			boolean current) {
	}

}
