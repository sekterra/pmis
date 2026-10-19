package com.tommy.common.org.support;

public final class PrimaryAssignmentPolicy {

	private PrimaryAssignmentPolicy() {
	}

	public static boolean becomesPrimary(boolean requested, boolean current, long otherCurrentCount) {
		if (!current) {
			return requested;
		}
		if (otherCurrentCount == 0) {
			return true;
		}
		return requested;
	}

	public static boolean demotesExisting(boolean primary, boolean current, long otherCurrentCount) {
		return current && primary && otherCurrentCount > 0;
	}

}
