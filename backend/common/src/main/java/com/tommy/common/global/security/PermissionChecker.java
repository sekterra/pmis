package com.tommy.common.global.security;

public interface PermissionChecker {

	void check(String menuCode, String actionCode);

	static String permissionCode(String menuCode, String actionCode) {
		return menuCode + ":" + actionCode;
	}

}
