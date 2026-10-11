package com.tommy.common.platform;

import java.util.Locale;

import com.tommy.common.global.exception.ApiException;

public final class DomainNames {

	private static final String HOST = "^[a-z0-9]([a-z0-9-]{0,61}[a-z0-9])?(\\.[a-z0-9]([a-z0-9-]{0,61}[a-z0-9])?)*$";

	private DomainNames() {
	}

	public static String require(String host) {
		String value = host == null ? "" : host.trim();
		if (value.length() < 1 || value.length() > 253 || !value.equals(value.toLowerCase(Locale.ROOT)) || !value.matches(HOST)) {
			throw new ApiException(400, "COMMON.VALIDATION_FAILED", "도메인 형식이 올바르지 않습니다.", 2);
		}
		return value;
	}

	public static String typeCode(String host) {
		return host.contains(".") ? "CUSTOM" : "SUBDOMAIN";
	}

}
