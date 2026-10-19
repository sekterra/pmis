package com.tommy.common.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.tommy.common.global.security.AccountStatusCache;
import com.tommy.common.global.security.JwtTokenService;
import com.tommy.common.global.security.MdmSecurityProperties;
import com.tommy.common.global.security.PasswordHasher;
import com.tommy.common.global.time.TenantClock;
import com.tommy.common.tenant.provision.TemporaryPassword;

import jakarta.persistence.EntityManager;

class AuthRulesTest {

	@Test
	void checksPasswordPolicy() {
		assertEquals(List.of(), PasswordPolicy.violations("Abcdefg1!x", "other", 10, 3));
		assertFalse(PasswordPolicy.violations("short1!", "other", 10, 3).isEmpty());
		assertFalse(PasswordPolicy.violations("alllowercase1", "other", 10, 3).isEmpty());
		assertFalse(PasswordPolicy.violations("Admin12345!", "admin", 10, 3).isEmpty());
		assertTrue(PasswordPolicy.violations(TemporaryPassword.generate().reveal(), "admin@example.com", 10, 4).isEmpty());
	}

	@Test
	void restartsLockCountAfterExpiry() {
		Instant lockedAt = Instant.parse("2026-10-10T00:00:00Z");
		assertFalse(LoginLockRules.expired(null, 30, lockedAt.plus(Duration.ofDays(1))));
		assertFalse(LoginLockRules.expired(lockedAt, 30, lockedAt.plus(Duration.ofMinutes(29))));
		assertTrue(LoginLockRules.expired(lockedAt, 30, lockedAt.plus(Duration.ofMinutes(30))));
		assertFalse(LoginLockRules.expired(lockedAt, 0, lockedAt.plus(Duration.ofDays(1))));
		assertEquals(LoginLockRules.Action.FAIL_LOCKED, LoginLockRules.decide(true, false, true));
		assertEquals(LoginLockRules.Action.SUCCESS, LoginLockRules.decide(true, true, true));
		assertEquals(LoginLockRules.Action.FAIL_RESTART, LoginLockRules.decide(true, true, false));
		assertEquals(LoginLockRules.Action.FAIL_INCREMENT, LoginLockRules.decide(false, false, false));
		assertTrue(LoginLockRules.locks(LoginLockRules.Action.FAIL_INCREMENT, 4, 5));
		assertFalse(LoginLockRules.locks(LoginLockRules.Action.FAIL_RESTART, 4, 5));
	}

	@Test
	void judgesRefreshGrace() {
		Instant revoked = Instant.parse("2026-10-10T00:00:00Z");
		assertEquals(RefreshReuseRules.Decision.IN_PROGRESS,
				RefreshReuseRules.decide("ROTATED", revoked, revoked.plusSeconds(10), 10));
		assertEquals(RefreshReuseRules.Decision.REUSE,
				RefreshReuseRules.decide("ROTATED", revoked, revoked.plusSeconds(11), 10));
		assertEquals(RefreshReuseRules.Decision.REUSE,
				RefreshReuseRules.decide("LOGOUT", revoked, revoked.plusSeconds(1), 10));
		assertEquals(RefreshReuseRules.Decision.REUSE,
				RefreshReuseRules.decide("PASSWORD_CHANGED", revoked, revoked.plusSeconds(1), 10));
	}

	@Test
	void writesRefreshCookieAttributes() {
		String header = RefreshCookies.header("token", Duration.ofDays(14), true);
		assertTrue(header.startsWith("mdm_refresh=token"));
		assertTrue(header.contains("Path=/api/mdm/v1/auth"));
		assertTrue(header.contains("HttpOnly"));
		assertTrue(header.contains("Secure"));
		assertTrue(header.contains("SameSite=Lax"));
		assertFalse(RefreshCookies.header("token", Duration.ofDays(1), false).contains("Secure"));
		assertTrue(RefreshCookies.header("", Duration.ZERO, true).contains("Max-Age=0"));
	}

	@Test
	void comparesDummyHashWhenAccountIsMissing() {
		PasswordHasher hasher = mock(PasswordHasher.class);
		when(hasher.hash(org.mockito.ArgumentMatchers.any())).thenReturn("dummy-hash");
		TenantAuthService service = new TenantAuthService(mock(EntityManager.class), hasher, mock(JwtTokenService.class),
				new MdmSecurityProperties(), mock(AccountStatusCache.class), mock(TenantClock.class));
		service.rejectUnknown("secret");
		verify(hasher).matches("secret", "dummy-hash");
	}

}
