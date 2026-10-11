package com.tommy.common.i18n;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.tommy.common.global.exception.ApiException;

public final class BundleTexts {

	public static final int MAX_NAMESPACES = 20;

	private BundleTexts() {
	}

	public static String resolve(String requested, Map<String, String> textsByLanguage, Map<String, String> fallbacks,
			String tenantDefault, String defaultMessage) {
		Set<String> seen = new HashSet<>();
		String current = requested;
		while (current != null && !current.isBlank() && seen.add(current)) {
			String text = textsByLanguage.get(current);
			if (text != null && !text.isBlank()) {
				return text;
			}
			current = fallbacks.get(current);
		}
		if (tenantDefault != null && !tenantDefault.isBlank() && !seen.contains(tenantDefault)) {
			String text = textsByLanguage.get(tenantDefault);
			if (text != null && !text.isBlank()) {
				return text;
			}
		}
		return defaultMessage;
	}

	public static List<String> namespaceCodes(String raw) {
		if (raw == null || raw.isBlank()) {
			throw invalid("네임스페이스가 필요합니다.");
		}
		List<String> codes = new ArrayList<>();
		for (String part : raw.split(",")) {
			if (!part.isBlank()) {
				codes.add(part.trim());
			}
		}
		if (codes.isEmpty() || codes.size() > MAX_NAMESPACES) {
			throw invalid("네임스페이스는 1개 이상 20개까지 조회할 수 있습니다.");
		}
		return codes;
	}

	private static ApiException invalid(String message) {
		return new ApiException(400, "COMMON.VALIDATION_FAILED", message, 2);
	}

}
