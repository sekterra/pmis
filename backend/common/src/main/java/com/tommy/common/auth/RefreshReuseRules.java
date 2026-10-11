package com.tommy.common.auth;

import java.time.Duration;
import java.time.Instant;

public final class RefreshReuseRules {

	public enum Decision {
		ROTATE, IN_PROGRESS, REUSE
	}

	private RefreshReuseRules() {
	}

	public static Decision decide(String revokeReason, Instant revokedAt, Instant now, int graceSeconds) {
		if (revokeReason == null || revokeReason.isBlank()) {
			return Decision.ROTATE;
		}
		if ("ROTATED".equals(revokeReason) && revokedAt != null && graceSeconds > 0 && now != null
				&& !now.isAfter(revokedAt.plus(Duration.ofSeconds(graceSeconds)))) {
			return Decision.IN_PROGRESS;
		}
		return Decision.REUSE;
	}

}
