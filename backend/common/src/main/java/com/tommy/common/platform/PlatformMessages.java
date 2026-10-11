package com.tommy.common.platform;

import java.util.Locale;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

public final class PlatformMessages {

	private static final Map<String, String> ENGLISH = Map.ofEntries(
			Map.entry("AUTH.INVALID_CREDENTIALS", "Sign-in failed."),
			Map.entry("AUTH.CSRF", "The request header is missing."),
			Map.entry("AUTH.TOKEN_EXPIRED", "The token has expired."),
			Map.entry("AUTH.TOKEN_REUSED", "The token was reused."),
			Map.entry("AUTH.REFRESH_IN_PROGRESS", "Token refresh is already in progress."),
			Map.entry("AUTH.ACCOUNT_DISABLED", "The account cannot be used."),
			Map.entry("AUTH.UNAUTHORIZED", "Authentication is required."),
			Map.entry("AUTH.PASSWORD_CURRENT", "The current password is wrong."),
			Map.entry("AUTH.PASSWORD_SAME", "The new password must differ from the current password."),
			Map.entry("AUTH.PASSWORD_POLICY", "The password does not meet the policy."),
			Map.entry("AUTH.PASSWORD_CHANGE_REQUIRED", "The password must be changed."),
			Map.entry("TENANT.INVALID_STATUS_TRANSITION", "That status change is not allowed."),
			Map.entry("TENANT.LAST_DOMAIN", "The last active domain cannot be removed."),
			Map.entry("TENANT.NOT_FOUND", "The tenant was not found."),
			Map.entry("TENANT.DUPLICATE", "The tenant code or domain already exists."),
			Map.entry("TENANT.ALREADY_ACTIVE", "The tenant is already active."),
			Map.entry("TENANT.INACTIVE", "The tenant cannot be used."),
			Map.entry("COMMON.VALIDATION_FAILED", "The request is invalid."),
			Map.entry("COMMON.INVALID_SORT", "The sort field is invalid."),
			Map.entry("COMMON.INVALID_PAGE", "The page value is invalid."));

	private PlatformMessages() {
	}

	public static String text(HttpServletRequest request, String code, String korean) {
		String accept = request == null ? null : request.getHeader("Accept-Language");
		if (accept != null && accept.toLowerCase(Locale.ROOT).startsWith("en")) {
			return ENGLISH.getOrDefault(code, korean);
		}
		return korean;
	}

}
