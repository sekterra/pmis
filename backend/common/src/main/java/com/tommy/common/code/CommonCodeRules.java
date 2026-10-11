package com.tommy.common.code;

public final class CommonCodeRules {

	private CommonCodeRules() {
	}

	public static void requireSameCodeGroup(Long codeGroupPk, Long parentCodeGroupPk) {
		if (parentCodeGroupPk != null && !parentCodeGroupPk.equals(codeGroupPk)) {
			throw new IllegalArgumentException("부모 코드는 같은 코드그룹이어야 합니다.");
		}
	}

}
