package com.tommy.common.tenant.provision;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.tommy.common.global.exception.ApiException;
import com.tommy.common.global.multitenancy.TenantDirectory;

class AdminProvisioningRulesTest {

	@Test
	void temporaryPasswordMeetsCharacterRules() {
		for (int attempt = 0; attempt < 30; attempt++) {
			String value = TemporaryPassword.generate().reveal();
			assertEquals(16, value.length());
			assertTrue(value.chars().anyMatch(Character::isUpperCase));
			assertTrue(value.chars().anyMatch(Character::isLowerCase));
			assertTrue(value.chars().anyMatch(Character::isDigit));
			assertTrue(value.chars().anyMatch(character -> "!@#$%^&*-_+=?".indexOf(character) >= 0));
			assertFalse(value.chars().anyMatch(character -> "0O1lI".indexOf(character) >= 0));
		}
	}

	@Test
	void invalidPolicyDaysFailAndExistingAccountSkipsPassword() {
		assertEquals(7, SecurityPolicyValues.positiveDays("7"));
		assertEquals(7, SecurityPolicyValues.positiveDays("07"));
		for (String raw : new String[] { null, "", "0", "-1", "1.5", "days" }) {
			ApiException exception = assertThrows(ApiException.class, () -> SecurityPolicyValues.positiveDays(raw));
			assertEquals("TENANT.SECURITY_POLICY_MISSING", exception.errorCode());
		}
		TemporaryPassword generated = TemporaryPassword.generate();
		TenantAdminWriter.CreationPlan present = TenantAdminWriter.plan(4L, generated);
		assertTrue(present.alreadyPresent());
		assertTrue(present.temporaryPassword() == null);
		TenantAdminWriter.CreationPlan created = TenantAdminWriter.plan(null, generated);
		assertFalse(created.alreadyPresent());
		assertTrue(created.temporaryPassword() == generated);
		ReissuedPassword reissued = new ReissuedPassword("it-ab", generated);
		assertFalse(reissued.toString().contains(generated.reveal()));
	}

	@Test
	void policyFailureMarksProvisioningFailed() {
		PgTenantAdvisoryLock lock = mock(PgTenantAdvisoryLock.class);
		doAnswer(invocation -> {
			invocation.getArgument(1, Runnable.class).run();
			return null;
		}).when(lock).runWithTenantLock(anyString(), any());
		ProvisioningJournal journal = mock(ProvisioningJournal.class);
		when(journal.findTenant(anyString())).thenReturn(Optional.empty());
		TenantDirectory directory = mock(TenantDirectory.class);
		ProvisioningStep failPolicy = new ProvisioningStep() {
			@Override
			public ProvisioningStepCode code() {
				return ProvisioningStepCode.ADMIN_CREATED;
			}

			@Override
			public void execute(ProvisioningWork work) {
				throw new ApiException(500, "TENANT.SECURITY_POLICY_MISSING", "보안 정책 임시 비밀번호 유효 일수가 없습니다.", 2);
			}
		};
		TenantProvisioningService service = new TenantProvisioningService(lock, journal, directory, List.of(
				noop(ProvisioningStepCode.REQUESTED),
				noop(ProvisioningStepCode.SCHEMA_CREATED),
				noop(ProvisioningStepCode.MIGRATED),
				noop(ProvisioningStepCode.SEEDED),
				failPolicy));
		ApiException exception = assertThrows(ApiException.class, () -> service.provision(
				new ProvisionTenantCommand("it-ab", "이름", null, "ko-KR", "관리자", "admin@example.com", null)));
		assertEquals("TENANT.SECURITY_POLICY_MISSING", exception.errorCode());
		verify(journal).markFailed(any(), anyString());
	}

	private static ProvisioningStep noop(ProvisioningStepCode code) {
		return new ProvisioningStep() {
			@Override
			public ProvisioningStepCode code() {
				return code;
			}

			@Override
			public void execute(ProvisioningWork work) {
				if (work.tenantPk() == 0) {
					work.setTenantPk(7);
				}
			}
		};
	}

}
