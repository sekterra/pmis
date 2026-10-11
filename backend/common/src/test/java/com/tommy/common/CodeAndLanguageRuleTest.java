package com.tommy.common;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.tommy.common.auth.PermissionRules;
import com.tommy.common.code.CommonCodeRules;
import com.tommy.common.i18n.SupportLanguageRules;

class CodeAndLanguageRuleTest {

	@Test
	void parentCodeMustBelongToSameGroup() {
		assertDoesNotThrow(() -> CommonCodeRules.requireSameCodeGroup(1L, null));
		assertDoesNotThrow(() -> CommonCodeRules.requireSameCodeGroup(1L, 1L));
		assertThrows(IllegalArgumentException.class, () -> CommonCodeRules.requireSameCodeGroup(1L, 2L));
	}

	@Test
	void otherPermissionRequiresReadOnSameMenu() {
		assertDoesNotThrow(() -> PermissionRules.requireReadWhenGrantingOther(true, "CREATE"));
		assertDoesNotThrow(() -> PermissionRules.requireReadWhenGrantingOther(false, "READ"));
		assertThrows(IllegalArgumentException.class, () -> PermissionRules.requireReadWhenGrantingOther(false, "CREATE"));
	}

	@Test
	void defaultLanguageMustBeInUse() {
		assertDoesNotThrow(() -> SupportLanguageRules.requireDefaultLanguageInUse("N", "N"));
		assertDoesNotThrow(() -> SupportLanguageRules.requireDefaultLanguageInUse("Y", "Y"));
		assertThrows(IllegalArgumentException.class, () -> SupportLanguageRules.requireDefaultLanguageInUse("Y", "N"));
	}

}
