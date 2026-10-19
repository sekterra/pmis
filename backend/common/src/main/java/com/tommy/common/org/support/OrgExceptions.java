package com.tommy.common.org.support;

import java.time.LocalDate;
import java.util.List;

import com.tommy.common.global.exception.ApiException;
import com.tommy.common.global.response.FieldErrorItem;

public final class OrgExceptions {

	private OrgExceptions() {
	}

	public static ApiException of(int status, String code, String message) {
		return new ApiException(status, code, message, 2);
	}

	public static ApiException field(String code, String message, String field) {
		return new ApiException(400, code, message, 2, List.of(new FieldErrorItem(field, message)));
	}

	public static void requireDateOrder(LocalDate start, LocalDate end, String code, String message) {
		if (start != null && end != null && end.isBefore(start)) {
			throw of(400, code, message);
		}
	}

	public static String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	public static String yn(String value) {
		return "Y".equals(value) ? "Y" : "N";
	}

}
