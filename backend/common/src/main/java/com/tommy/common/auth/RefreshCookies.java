package com.tommy.common.auth;

import java.time.Duration;

import org.springframework.http.ResponseCookie;

import jakarta.servlet.http.HttpServletResponse;

public final class RefreshCookies {

	public static final String NAME = "mdm_refresh";

	public static final String PATH = "/api/mdm/v1/auth";

	public static final String PLATFORM_NAME = "mdm_platform_refresh";

	public static final String PLATFORM_PATH = "/api/platform/v1/auth";

	private RefreshCookies() {
	}

	public static void set(HttpServletResponse response, String value, Duration maxAge, boolean secure) {
		set(response, NAME, PATH, value, maxAge, secure);
	}

	public static void clear(HttpServletResponse response, boolean secure) {
		clear(response, NAME, PATH, secure);
	}

	public static void set(HttpServletResponse response, String name, String path, String value, Duration maxAge, boolean secure) {
		response.addHeader("Set-Cookie", header(name, path, value, maxAge, secure));
	}

	public static void clear(HttpServletResponse response, String name, String path, boolean secure) {
		response.addHeader("Set-Cookie", header(name, path, "", Duration.ZERO, secure));
	}

	public static String header(String value, Duration maxAge, boolean secure) {
		return header(NAME, PATH, value, maxAge, secure);
	}

	public static String header(String name, String path, String value, Duration maxAge, boolean secure) {
		return ResponseCookie.from(name, value == null ? "" : value)
				.path(path)
				.httpOnly(true)
				.secure(secure)
				.sameSite("Lax")
				.maxAge(maxAge)
				.build()
				.toString();
	}

}
