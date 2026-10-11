package com.tommy.common.auth;

import jakarta.servlet.http.HttpServletRequest;

public final class ClientAddresses {

	private ClientAddresses() {
	}

	public static String ip(HttpServletRequest request, boolean trustForwarded) {
		String value = request.getRemoteAddr();
		if (trustForwarded) {
			String forwarded = request.getHeader("X-Forwarded-For");
			if (forwarded != null && !forwarded.isBlank()) {
				value = forwarded.split(",")[0].trim();
			}
		}
		if (value == null) {
			return null;
		}
		return value.length() > 45 ? value.substring(0, 45) : value;
	}

	public static String userAgent(HttpServletRequest request) {
		String agent = request.getHeader("User-Agent");
		if (agent == null) {
			return null;
		}
		return agent.length() > 500 ? agent.substring(0, 500) : agent;
	}

}
