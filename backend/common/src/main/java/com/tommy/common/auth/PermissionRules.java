package com.tommy.common.auth;

public final class PermissionRules {

	private PermissionRules() {
	}

	public static void requireReadWhenGrantingOther(boolean sameMenuReadGranted, String actionCode) {
		if (actionCode != null && !"READ".equals(actionCode) && !sameMenuReadGranted) {
			throw new IllegalArgumentException("READ 이외 권한을 부여하려면 같은 메뉴의 READ 권한이 필요합니다.");
		}
	}

}
