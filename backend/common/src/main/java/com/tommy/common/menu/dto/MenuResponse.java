package com.tommy.common.menu.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.tommy.common.i18n.dto.MessageTextResponse;

public record MenuResponse(
		Long menuPk,
		Long parentMenuPk,
		String parentMenuCode,
		String solutionCode,
		String menuCode,
		String menuName,
		String menuTypeCode,
		String routePath,
		String iconName,
		Integer levelNo,
		String menuPath,
		Integer sortOrder,
		String visibleYn,
		String useYn,
		String description,
		LocalDateTime createDatetime,
		Long createUserPk,
		LocalDateTime updateDatetime,
		Long updateUserPk,
		List<MessageTextResponse> translations,
		List<PermissionSummary> permissions) {

	public record PermissionSummary(String permissionCode, String actionCode, String useYn) {
	}

}
