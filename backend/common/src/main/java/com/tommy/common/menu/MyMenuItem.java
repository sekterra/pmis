package com.tommy.common.menu;

import java.util.List;

public record MyMenuItem(String menuCode, String parentMenuCode, String menuName, String menuTypeCode, String routePath,
		String iconName, Integer levelNo, Integer sortOrder, List<String> actions) {
}
