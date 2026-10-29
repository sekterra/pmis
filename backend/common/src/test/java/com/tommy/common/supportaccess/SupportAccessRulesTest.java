package com.tommy.common.supportaccess;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.ZonedDateTime;

import org.junit.jupiter.api.Test;

import com.tommy.common.auth.RefreshTokenValues;
import com.tommy.common.global.exception.ApiException;

class SupportAccessRulesTest {

	@Test
	void fillsADefaultReasonAndRejectsALongOne() {
		ZonedDateTime now = ZonedDateTime.of(2026, 10, 29, 1, 5, 0, 0, java.time.ZoneOffset.UTC);
		SupportAccessRules.Reason blank = SupportAccessRules.reason("  ", "ops@example.com", now);
		assertTrue(blank.defaulted());
		assertEquals("2026-10-29 10:05 ops@example.com", blank.text());
		SupportAccessRules.Reason given = SupportAccessRules.reason(" 점검 ", "ops@example.com", now);
		assertFalse(given.defaulted());
		assertEquals("점검", given.text());
		ApiException tooLong = assertThrows(ApiException.class,
				() -> SupportAccessRules.reason("가".repeat(201), "ops@example.com", now));
		assertEquals("COMMON.VALIDATION_FAILED", tooLong.errorCode());
	}

	@Test
	void hashesTicketsAndShowsExpiredActiveRowsAsExpired() {
		String ticket = RefreshTokenValues.raw();
		assertEquals(64, RefreshTokenValues.hash(ticket).length());
		assertFalse(RefreshTokenValues.hash(ticket).equals(ticket));
		Instant now = Instant.parse("2026-10-29T01:00:00Z");
		assertEquals("EXPIRED", SupportAccessRules.displayStatus("ACTIVE", now.minusSeconds(1), now));
		assertEquals("ACTIVE", SupportAccessRules.displayStatus("ACTIVE", now.plusSeconds(30), now));
		assertEquals("ENDED", SupportAccessRules.displayStatus("ENDED", now.minusSeconds(1), now));
	}

	@Test
	void marksPasswordRolePermissionAndMenuWritesSensitive() {
		assertTrue(SupportAccessRules.sensitive("POST", "/api/mdm/v1/users"));
		assertTrue(SupportAccessRules.sensitive("post", "/api/mdm/v1/users/15/password/reset"));
		assertTrue(SupportAccessRules.sensitive("PUT", "/api/mdm/v1/users/15/roles"));
		assertTrue(SupportAccessRules.sensitive("PUT", "/api/mdm/v1/roles/4/permissions"));
		assertTrue(SupportAccessRules.sensitive("DELETE", "/api/mdm/v1/permissions/9"));
		assertTrue(SupportAccessRules.sensitive("POST", "/api/mdm/v1/menus"));
		assertFalse(SupportAccessRules.sensitive("GET", "/api/mdm/v1/users"));
		assertFalse(SupportAccessRules.sensitive("POST", "/api/mdm/v1/auth/support-access/end"));
		assertFalse(SupportAccessRules.sensitive("PUT", "/api/mdm/v1/users/15/status"));
	}

}
