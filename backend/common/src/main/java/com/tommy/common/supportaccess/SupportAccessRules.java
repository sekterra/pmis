package com.tommy.common.supportaccess;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.springframework.security.oauth2.jwt.Jwt;

import com.tommy.common.global.exception.ApiException;
import com.tommy.common.global.response.FieldErrorItem;

public final class SupportAccessRules {

	public static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

	private static final DateTimeFormatter REASON_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

	private static final Pattern USER_CREATE = Pattern.compile("^/api/mdm/v1/users$");

	private static final Pattern PASSWORD_RESET = Pattern.compile("^/api/mdm/v1/users/\\d+/password/reset$");

	private static final Pattern ROLE_REPLACE = Pattern.compile("^/api/mdm/v1/users/\\d+/roles$");

	private static final Pattern ROLE_PERMISSIONS = Pattern.compile("^/api/mdm/v1/roles/\\d+/permissions$");

	private static final Pattern PERMISSION_WRITE = Pattern.compile("^/api/mdm/v1/permissions(/.*)?$");

	private static final Pattern MENU_WRITE = Pattern.compile("^/api/mdm/v1/menus(/.*)?$");

	private SupportAccessRules() {
	}

	public static Reason reason(String raw, String loginId, ZonedDateTime now) {
		String trimmed = raw == null ? "" : raw.trim();
		if (trimmed.isEmpty()) {
			return new Reason(REASON_TIME.format(now.withZoneSameInstant(SEOUL)) + " " + loginId, true);
		}
		if (trimmed.length() > 200) {
			throw new ApiException(400, "COMMON.VALIDATION_FAILED", "요청 값이 올바르지 않습니다.", 2,
					List.of(new FieldErrorItem("reason", "사유는 200자 이하입니다.")));
		}
		return new Reason(trimmed, false);
	}

	public static String displayStatus(String stored, Instant expire, Instant now) {
		if ("ACTIVE".equals(stored) && expire != null && expire.isBefore(now)) {
			return "EXPIRED";
		}
		return stored;
	}

	public static boolean sensitive(String method, String path) {
		String verb = method == null ? "" : method.toUpperCase(Locale.ROOT);
		if (path == null) {
			return false;
		}
		if ("POST".equals(verb) && (USER_CREATE.matcher(path).matches() || PASSWORD_RESET.matcher(path).matches())) {
			return true;
		}
		if ("PUT".equals(verb) && (ROLE_REPLACE.matcher(path).matches() || ROLE_PERMISSIONS.matcher(path).matches())) {
			return true;
		}
		if (("POST".equals(verb) || "PUT".equals(verb) || "DELETE".equals(verb))
				&& (PERMISSION_WRITE.matcher(path).matches() || MENU_WRITE.matcher(path).matches())) {
			return true;
		}
		return false;
	}

	public static boolean writeMethod(String method) {
		String verb = method == null ? "" : method.toUpperCase(Locale.ROOT);
		return "POST".equals(verb) || "PUT".equals(verb) || "PATCH".equals(verb) || "DELETE".equals(verb);
	}

	public static Long claimLong(Jwt jwt, String name) {
		if (jwt == null || jwt.getClaims() == null || !jwt.getClaims().containsKey(name)) {
			return null;
		}
		Object value = jwt.getClaim(name);
		if (value instanceof Number number) {
			return number.longValue();
		}
		if (value == null) {
			return null;
		}
		return Long.parseLong(value.toString());
	}

	public record Reason(String text, boolean defaulted) {
	}

}
