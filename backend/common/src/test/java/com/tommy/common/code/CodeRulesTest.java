package com.tommy.common.code;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.tommy.common.global.exception.ApiException;

class CodeRulesTest {

	@Test
	void rejectsHierarchyLengthDateAndCycle() {
		ApiException parent = assertThrows(ApiException.class, () -> CodeRules.requireParentAllowed(false, "ROOT"));
		assertEquals("CODE.PARENT_NOT_ALLOWED", parent.errorCode());
		assertDoesNotThrow(() -> CodeRules.requireParentAllowed(true, "ROOT"));
		ApiException length = assertThrows(ApiException.class, () -> CodeRules.requireValueLength(2, "ABC"));
		assertEquals("CODE.VALUE_LENGTH", length.errorCode());
		ApiException date = assertThrows(ApiException.class,
				() -> CodeRules.requireDateOrder(LocalDate.of(2026, 10, 11), LocalDate.of(2026, 10, 10)));
		assertEquals("CODE.INVALID_DATE", date.errorCode());
		Map<Long, Long> parents = Map.of(2L, 1L, 3L, 2L);
		ApiException cycle = assertThrows(ApiException.class,
				() -> CodeRules.requireNoCycle(1L, 3L, parents::get));
		assertEquals("CODE.CYCLE", cycle.errorCode());
		assertDoesNotThrow(() -> CodeRules.requireNoCycle(4L, 3L, parents::get));
		ApiException policy = assertThrows(ApiException.class, () -> CodeRules.requirePositivePolicy("0"));
		assertEquals("CODE.INVALID_POLICY_VALUE", policy.errorCode());
		assertDoesNotThrow(() -> CodeRules.requirePolicyValue("LOCK_MINUTES", "0"));
		assertThrows(ApiException.class, () -> CodeRules.requirePolicyValue("PASSWORD_MIN_CLASSES", "5"));
		assertThrows(ApiException.class, () -> CodeRules.requirePolicyValue("PASSWORD_MIN_LENGTH", "7"));
		assertThrows(ApiException.class, () -> CodeRules.requirePolicyValue("PASSWORD_HISTORY_COUNT", "0"));
	}

}
