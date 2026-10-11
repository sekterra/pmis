package com.tommy.common.i18n;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.tommy.common.global.exception.ApiException;

class BundleTextsTest {

	@Test
	void resolvesLanguageFallbackDefaultAndStopsCycles() {
		Map<String, String> texts = Map.of("ko-KR", "한글", "en-US", "English");
		Map<String, String> fallbacks = Map.of("en-US", "ko-KR", "fr-FR", "en-US", "a", "b", "b", "a");
		assertEquals("English", BundleTexts.resolve("en-US", texts, fallbacks, "ko-KR", "기본"));
		assertEquals("한글", BundleTexts.resolve("fr-FR", Map.of("ko-KR", "한글"), fallbacks, "ko-KR", "기본"));
		assertEquals("기본", BundleTexts.resolve("de-DE", Map.of(), fallbacks, "ko-KR", "기본"));
		assertEquals("기본", BundleTexts.resolve("a", Map.of(), fallbacks, "ko-KR", "기본"));
	}

	@Test
	void rejectsNamespaceCountsOutsideTheLimit() {
		assertThrows(ApiException.class, () -> BundleTexts.namespaceCodes(" "));
		assertThrows(ApiException.class, () -> BundleTexts.namespaceCodes(String.join(",", java.util.Collections.nCopies(21, "a"))));
		assertEquals(2, BundleTexts.namespaceCodes(" error , menu ").size());
	}

}
