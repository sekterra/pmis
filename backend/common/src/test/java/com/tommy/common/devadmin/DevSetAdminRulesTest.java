package com.tommy.common.devadmin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.SecureRandom;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.tommy.common.devadmin.DevSetAdminRules.Command;

class DevSetAdminRulesTest {

	@Test
	void rejectsMissingProfileAndBlankValues() {
		assertThrows(IllegalArgumentException.class, () -> DevSetAdminRules.requireDevProfile(false));
		DevSetAdminRules.requireDevProfile(true);
		String login = login();
		String password = letters(8);
		assertThrows(IllegalArgumentException.class, () -> DevSetAdminRules.parse(null, null, login, password));
		Command omitted = DevSetAdminRules.parse("tenant", "   ", login, password);
		assertNull(omitted.tenantCode());
		assertFalse(omitted.toString().contains(password));
		assertThrows(IllegalArgumentException.class, () -> DevSetAdminRules.parse("tenant", "it-ab", "  ", password));
		assertThrows(IllegalArgumentException.class, () -> DevSetAdminRules.parse("platform", null, login, "   "));
	}

	@Test
	void checksLoginShapeAndBcryptLimitWithRandomValues() {
		String password = letters(12);
		assertThrows(IllegalArgumentException.class, () -> DevSetAdminRules.parse("other", null, login(), password));
		assertThrows(IllegalArgumentException.class, () -> DevSetAdminRules.parse("platform", null, "ab", password));
		assertThrows(IllegalArgumentException.class, () -> DevSetAdminRules.parse("platform", null, "bad id", password));
		Command command = DevSetAdminRules.parse("platform", null, "  AbC.User_1  ", letters(DevSetAdminRules.BCRYPT_MAX_BYTES));
		assertEquals("abc.user_1", command.loginId());
		assertEquals("platform", command.target());
		assertFalse(command.toString().contains(command.password()));
		String tooLong = letters(DevSetAdminRules.BCRYPT_MAX_BYTES + 1);
		assertThrows(IllegalArgumentException.class, () -> DevSetAdminRules.parse("platform", null, login(), tooLong));
		String tenantPassword = letters(6);
		Command tenant = DevSetAdminRules.parse("tenant", "it-ab", "Login@X", tenantPassword);
		assertEquals("it-ab", tenant.tenantCode());
		assertEquals("login@x", tenant.loginId());
		String line = DevSetAdminRules.line(tenant.target(), tenant.tenantCode(), tenant.loginId(), true);
		assertEquals("tenant it-ab created login@x", line);
		assertFalse(line.contains(tenantPassword));
		assertTrue(DevSetAdminRules.line("platform", null, tenant.loginId(), false).startsWith("platform updated "));
	}

	@Test
	void resolvesABlankTenantCodeOnlyWhenOneActiveTenantExists() {
		assertEquals("it-one", DevSetAdminRules.resolveOmittedTenant(List.of("it-one")));
		IllegalStateException none = assertThrows(IllegalStateException.class, () -> DevSetAdminRules.resolveOmittedTenant(List.of()));
		assertEquals("ACTIVE 테넌트가 없습니다.", none.getMessage());
		IllegalStateException many = assertThrows(IllegalStateException.class,
				() -> DevSetAdminRules.resolveOmittedTenant(List.of("it-b", "it-a")));
		assertTrue(many.getMessage().contains("it-b"));
		assertTrue(many.getMessage().contains("it-a"));
		assertTrue(many.getMessage().startsWith("ACTIVE 테넌트가 2개입니다:"));
	}

	private static String login() {
		return "it" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
	}

	private static String letters(int length) {
		SecureRandom random = new SecureRandom();
		char[] chars = new char[length];
		for (int index = 0; index < length; index++) {
			chars[index] = (char) ('g' + random.nextInt(20));
		}
		return new String(chars);
	}

}
