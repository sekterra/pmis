package com.tommy.common.i18n;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.tommy.common.global.exception.ApiException;

public final class SupportLanguageRules {

	private SupportLanguageRules() {
	}

	public static void requireDefaultLanguageInUse(String defaultYn, String useYn) {
		if ("Y".equals(defaultYn) && !"Y".equals(useYn)) {
			throw new IllegalArgumentException("기본 언어는 사용 중이어야 합니다.");
		}
	}

	public static void requireNoCycle(String code, String fallback, Map<String, String> fallbackByCode) {
		Map<String, String> graph = new HashMap<>(fallbackByCode);
		if (fallback == null || fallback.isBlank()) {
			graph.remove(code);
		}
		else {
			graph.put(code, fallback);
		}
		Set<String> seen = new HashSet<>();
		String current = code;
		while (current != null && !current.isBlank()) {
			if (!seen.add(current)) {
				throw new ApiException(400, "I18N.FALLBACK_CYCLE", "대체 언어가 순환합니다.", 2);
			}
			current = graph.get(current);
		}
	}

	public static boolean blocksDeactivate(boolean defaultLanguage, boolean usedAsFallback) {
		return defaultLanguage || usedAsFallback;
	}

	public static List<String> replacedDefaults(String target, boolean makeDefault, List<String> currentDefaults) {
		if (!makeDefault) {
			if (currentDefaults.size() == 1 && currentDefaults.contains(target)) {
				throw new ApiException(400, "I18N.DEFAULT_REQUIRED", "기본 언어는 하나만 있어야 합니다.", 2);
			}
			return List.of();
		}
		return currentDefaults.stream().filter(code -> !code.equals(target)).toList();
	}

}
