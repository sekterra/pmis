package com.tommy.common.menu;

import java.util.regex.Pattern;

public final class MenuRules {

	public static final int MAX_LEVEL = 4;

	public static final Pattern SOLUTION = Pattern.compile("^[A-Z][A-Z0-9_]{1,49}$");

	public static final Pattern MENU_CODE = Pattern.compile("^[A-Z][A-Z0-9_]{0,99}$");

	public static final Pattern ICON = Pattern.compile("^[A-Za-z][A-Za-z0-9]*$");

	private MenuRules() {
	}

	public static boolean solution(String value) {
		return value != null && SOLUTION.matcher(value).matches();
	}

	public static boolean menuCode(String value) {
		return value != null && MENU_CODE.matcher(value).matches();
	}

	public static boolean icon(String value) {
		return value == null || ICON.matcher(value).matches();
	}

	public static boolean route(String value) {
		if (value == null || !value.startsWith("/") || value.length() > 300) {
			return false;
		}
		return !value.contains("//") && !value.contains("..") && !value.contains("\\") && !value.contains("://")
				&& value.chars().noneMatch(Character::isWhitespace);
	}

	public static String path(String parentPath, String menuCode) {
		if (parentPath == null || parentPath.isBlank()) {
			return "/" + menuCode;
		}
		return parentPath + "/" + menuCode;
	}

	public static boolean cycle(String currentPath, String parentPath) {
		if (currentPath == null || parentPath == null) {
			return false;
		}
		return parentPath.equals(currentPath) || parentPath.startsWith(currentPath + "/");
	}

	public static boolean tooDeep(int levelNo) {
		return levelNo > MAX_LEVEL;
	}

}
