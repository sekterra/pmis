package com.tommy.common.i18n;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.tommy.common.global.exception.ApiException;

class SupportLanguageRulesTest {

	@Test
	void rejectsFallbackCycleAndProtectsTheOnlyDefault() {
		assertThrows(ApiException.class, () -> SupportLanguageRules.requireNoCycle("ko-KR", "en-US", Map.of("en-US", "ko-KR")));
		SupportLanguageRules.requireNoCycle("ja-JP", "en-US", Map.of("en-US", "ko-KR"));
		assertTrue(SupportLanguageRules.blocksDeactivate(true, false));
		assertTrue(SupportLanguageRules.blocksDeactivate(false, true));
		assertThrows(ApiException.class, () -> SupportLanguageRules.replacedDefaults("ko-KR", false, List.of("ko-KR")));
		assertEquals(List.of("ko-KR"), SupportLanguageRules.replacedDefaults("ja-JP", true, List.of("ko-KR")));
	}

}
