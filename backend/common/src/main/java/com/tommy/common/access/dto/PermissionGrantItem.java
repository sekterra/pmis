package com.tommy.common.access.dto;

public record PermissionGrantItem(
		Long permissionPk,
		String permissionCode,
		String menuCode,
		String menuName,
		String actionCode,
		String permissionName,
		boolean granted) {
}
