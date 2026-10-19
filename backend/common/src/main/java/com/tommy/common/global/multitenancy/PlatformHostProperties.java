package com.tommy.common.global.multitenancy;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;

import jakarta.servlet.http.HttpServletRequest;

@ConfigurationProperties(prefix = "mdm.platform")
public class PlatformHostProperties {

	private String hosts = "";

	public void setHosts(String hosts) {
		this.hosts = hosts == null ? "" : hosts;
	}

	public String getHosts() {
		return hosts;
	}

	public boolean restricted() {
		return !allowed().isEmpty();
	}

	public boolean matches(String rawHost) {
		String host = normalize(rawHost);
		return !host.isEmpty() && allowed().contains(host);
	}

	public static String requestHost(HttpServletRequest request) {
		String header = request.getHeader("Host");
		if (header != null && !header.isBlank()) {
			return normalize(header);
		}
		return normalize(request.getServerName());
	}

	public static String normalize(String raw) {
		if (raw == null) {
			return "";
		}
		String value = raw.trim().toLowerCase(Locale.ROOT);
		if (value.startsWith("[")) {
			int end = value.indexOf(']');
			return end >= 0 ? value.substring(0, end + 1) : "";
		}
		int colon = value.lastIndexOf(':');
		if (colon > 0 && value.indexOf(':') == colon && colon < value.length() - 1 && value.substring(colon + 1).chars().allMatch(Character::isDigit)) {
			value = value.substring(0, colon);
		}
		return value;
	}

	private Set<String> allowed() {
		Set<String> values = new LinkedHashSet<>();
		for (String part : Arrays.asList(hosts.split(","))) {
			String host = normalize(part);
			if (!host.isEmpty()) {
				values.add(host);
			}
		}
		return values;
	}

}
