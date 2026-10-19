package com.tommy.common.menu.dto;

import java.util.List;

public record MenuTreeItem(
		Long menuPk,
		String menuCode,
		String menuName,
		String menuTypeCode,
		Integer depth,
		Integer sortOrder,
		int childCount,
		long permissionCount,
		List<MenuTreeItem> children) {
}
