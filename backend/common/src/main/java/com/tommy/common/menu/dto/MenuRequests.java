package com.tommy.common.menu.dto;

import java.util.List;

import com.tommy.common.i18n.dto.MessageTextRequest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class MenuRequests {

	private MenuRequests() {
	}

	public record MenuCreateRequest(
			@NotBlank @Size(max = 50) String solutionCode,
			@NotBlank @Size(max = 100) String menuCode,
			@NotBlank @Size(max = 200) String menuName,
			@NotBlank @Size(max = 30) String menuTypeCode,
			@Size(max = 100) String parentMenuCode,
			@Size(max = 300) String routePath,
			@Size(max = 100) String iconName,
			Integer sortOrder,
			@Size(max = 1) String visibleYn,
			@Size(max = 1000) String description,
			@Valid List<MessageTextRequest> translations) {
	}

	public record MenuUpdateRequest(
			@Size(max = 50) String solutionCode,
			@Size(max = 100) String menuCode,
			@Size(max = 30) String menuTypeCode,
			@NotBlank @Size(max = 200) String menuName,
			@Size(max = 100) String parentMenuCode,
			@Size(max = 300) String routePath,
			@Size(max = 100) String iconName,
			Integer sortOrder,
			@Size(max = 1) String visibleYn,
			@Size(max = 1000) String description,
			@Valid List<MessageTextRequest> translations) {
	}

	public record MenuSortItem(@NotBlank @Size(max = 100) String menuCode, Integer sortOrder) {
	}

}
