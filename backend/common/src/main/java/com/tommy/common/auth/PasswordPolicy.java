package com.tommy.common.auth;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class PasswordPolicy {

	private PasswordPolicy() {
	}

	public static boolean sameAsCurrent(boolean matchesCurrent) {
		return matchesCurrent;
	}

	public static List<String> violations(String password, String loginId, int minLength, int minClasses) {
		List<String> reasons = new ArrayList<>();
		String value = password == null ? "" : password;
		if (value.length() < minLength) {
			reasons.add("길이는 " + minLength + "자 이상이어야 합니다.");
		}
		int classes = 0;
		if (value.chars().anyMatch(Character::isUpperCase)) {
			classes++;
		}
		if (value.chars().anyMatch(Character::isLowerCase)) {
			classes++;
		}
		if (value.chars().anyMatch(Character::isDigit)) {
			classes++;
		}
		if (value.chars().anyMatch(ch -> !Character.isLetterOrDigit(ch) && !Character.isWhitespace(ch))) {
			classes++;
		}
		if (classes < minClasses) {
			reasons.add("문자 종류는 " + minClasses + "가지 이상이어야 합니다.");
		}
		if (loginId != null && !loginId.isBlank() && value.toLowerCase(Locale.ROOT).contains(loginId.toLowerCase(Locale.ROOT))) {
			reasons.add("로그인 아이디를 포함할 수 없습니다.");
		}
		return List.copyOf(reasons);
	}

}
