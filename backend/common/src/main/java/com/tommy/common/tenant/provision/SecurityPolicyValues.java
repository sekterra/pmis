package com.tommy.common.tenant.provision;

import com.tommy.common.global.exception.ApiException;

final class SecurityPolicyValues {

	static final String GROUP = "SECURITY_POLICY";

	static final String TEMP_PASSWORD_VALID_DAYS = "TEMP_PASSWORD_VALID_DAYS";

	private SecurityPolicyValues() {
	}

	static int positiveDays(String raw) {
		if (raw == null || !raw.trim().matches("[0-9]+")) {
			throw missing();
		}
		long days;
		try {
			days = Long.parseLong(raw.trim());
		}
		catch (NumberFormatException ex) {
			throw missing();
		}
		if (days < 1 || days > Integer.MAX_VALUE) {
			throw missing();
		}
		return (int) days;
	}

	private static ApiException missing() {
		return new ApiException(500, "TENANT.SECURITY_POLICY_MISSING", "보안 정책 임시 비밀번호 유효 일수가 없습니다.", 2);
	}

}
