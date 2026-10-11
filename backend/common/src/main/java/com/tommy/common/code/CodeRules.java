package com.tommy.common.code;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

import com.tommy.common.global.exception.ApiException;

public final class CodeRules {

	private CodeRules() {
	}

	public static void requireDateOrder(LocalDate from, LocalDate to) {
		if (from != null && to != null && from.isAfter(to)) {
			throw new ApiException(400, "CODE.INVALID_DATE", "시작일은 종료일보다 늦을 수 없습니다.", 2);
		}
	}

	public static void requireValueLength(Integer maxLength, String codeValue) {
		if (maxLength != null && codeValue != null && codeValue.length() > maxLength) {
			throw new ApiException(400, "CODE.VALUE_LENGTH", "코드 값이 허용 길이를 넘습니다.", 2);
		}
	}

	public static void requireParentAllowed(boolean hierarchical, String parentCodeValue) {
		if (!hierarchical && parentCodeValue != null && !parentCodeValue.isBlank()) {
			throw new ApiException(400, "CODE.PARENT_NOT_ALLOWED", "계층형이 아닌 그룹에는 상위 코드를 둘 수 없습니다.", 2);
		}
	}

	public static void requireNoCycle(Long selfPk, Long parentPk, Function<Long, Long> parentOf) {
		if (parentPk == null) {
			return;
		}
		Set<Long> seen = new HashSet<>();
		Long current = parentPk;
		while (current != null) {
			if (current.equals(selfPk) || !seen.add(current)) {
				throw new ApiException(400, "CODE.CYCLE", "코드 계층이 순환합니다.", 2);
			}
			current = parentOf.apply(current);
		}
	}

	public static void requirePositivePolicy(String extAttr1) {
		requirePolicyValue("TEMP_PASSWORD_VALID_DAYS", extAttr1);
	}

	public static void requirePolicyValue(String codeValue, String extAttr1) {
		if (!validPolicy(codeValue, extAttr1)) {
			throw new ApiException(400, "CODE.INVALID_POLICY_VALUE", "보안 정책 값은 양의 정수여야 합니다.", 2);
		}
	}

	public static boolean validPolicy(String codeValue, String raw) {
		if (raw == null || !raw.trim().matches("[0-9]+")) {
			return false;
		}
		long value;
		try {
			value = Long.parseLong(raw.trim());
		}
		catch (NumberFormatException ex) {
			return false;
		}
		if (value > Integer.MAX_VALUE) {
			return false;
		}
		return switch (codeValue == null ? "" : codeValue) {
			case "LOCK_MINUTES" -> value >= 0;
			case "PASSWORD_MIN_CLASSES" -> value >= 1 && value <= 4;
			case "PASSWORD_MIN_LENGTH" -> value >= 8;
			default -> value >= 1;
		};
	}

	public static boolean positiveDays(String raw) {
		if (raw == null || !raw.trim().matches("[0-9]+")) {
			return false;
		}
		try {
			long days = Long.parseLong(raw.trim());
			return days >= 1 && days <= Integer.MAX_VALUE;
		}
		catch (NumberFormatException ex) {
			return false;
		}
	}

}
