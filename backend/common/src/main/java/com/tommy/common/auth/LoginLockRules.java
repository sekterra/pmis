package com.tommy.common.auth;

import java.time.Duration;
import java.time.Instant;

public final class LoginLockRules {

	public enum Action {
		SUCCESS, FAIL_LOCKED, FAIL_INCREMENT, FAIL_RESTART
	}

	private LoginLockRules() {
	}

	public static boolean expired(Instant lockedAt, int lockMinutes, Instant now) {
		if (lockMinutes <= 0 || lockedAt == null || now == null) {
			return false;
		}
		return !now.isBefore(lockedAt.plus(Duration.ofMinutes(lockMinutes)));
	}

	public static Action decide(boolean locked, boolean lockExpired, boolean passwordOk) {
		if (locked && !lockExpired) {
			return Action.FAIL_LOCKED;
		}
		if (locked) {
			return passwordOk ? Action.SUCCESS : Action.FAIL_RESTART;
		}
		return passwordOk ? Action.SUCCESS : Action.FAIL_INCREMENT;
	}

	public static boolean locks(Action action, int failCount, int maxFail) {
		if (action == Action.FAIL_RESTART) {
			return maxFail <= 1;
		}
		if (action == Action.FAIL_INCREMENT) {
			return failCount + 1 >= maxFail;
		}
		return false;
	}

}
