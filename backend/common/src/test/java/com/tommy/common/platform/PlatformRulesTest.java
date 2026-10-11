package com.tommy.common.platform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.tommy.common.auth.PasswordPolicy;
import com.tommy.common.global.exception.ApiException;
import com.tommy.common.tenant.provision.TemporaryPassword;

class PlatformRulesTest {

	@Test
	void checksPlatformPasswordAndMasksTemporaryPassword() {
		assertFalse(PasswordPolicy.violations("Short1!", "ops@example.com", 12, 3).isEmpty());
		assertTrue(PasswordPolicy.violations("N3w-Policy-Pass1", "ops@example.com", 12, 3).isEmpty());
		TemporaryPassword password = TemporaryPassword.generate(20);
		assertEquals(20, password.reveal().length());
		assertTrue(PasswordPolicy.violations(password.reveal(), "ops@example.com", 12, 4).isEmpty());
		assertEquals("TemporaryPassword[masked]", password.toString());
		assertFalse(password.toString().contains(password.reveal()));
	}

	@Test
	void createsBootstrapAdminOnlyWhenEmpty() {
		assertTrue(PlatformBootstrapRules.empty(0));
		assertFalse(PlatformBootstrapRules.empty(1));
	}

	@Test
	void allowsOnlyActiveAndSuspendedTransitions() {
		List<String> statuses = List.of("PROVISIONING", "ACTIVE", "SUSPENDED", "TERMINATED", "FAILED");
		for (String from : statuses) {
			for (String to : statuses) {
				boolean allowed = ("ACTIVE".equals(from) && "SUSPENDED".equals(to)) || ("SUSPENDED".equals(from) && "ACTIVE".equals(to));
				assertEquals(allowed, TenantStatusTransitions.allowed(from, to), from + "->" + to);
			}
		}
	}

	@Test
	void rejectsBadDomainNames() {
		assertEquals("CUSTOM", DomainNames.typeCode(DomainNames.require("it-host.example.test")));
		assertEquals("SUBDOMAIN", DomainNames.typeCode(DomainNames.require("acme")));
		assertThrows(ApiException.class, () -> DomainNames.require("IT.Example"));
		assertThrows(ApiException.class, () -> DomainNames.require("-bad.example"));
		assertThrows(ApiException.class, () -> DomainNames.require("a".repeat(254)));
	}

	@Test
	void printsTemporaryPasswordOnce() {
		TemporaryPassword password = TemporaryPassword.generate(20);
		PrintStream original = System.out;
		ByteArrayOutputStream captured = new ByteArrayOutputStream();
		System.setOut(new PrintStream(captured));
		try {
			System.out.println(password.reveal());
		}
		finally {
			System.setOut(original);
		}
		String printed = captured.toString().trim();
		assertEquals(password.reveal(), printed);
		assertFalse(printed.contains("\n"));
	}

}
