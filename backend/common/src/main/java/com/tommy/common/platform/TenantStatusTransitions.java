package com.tommy.common.platform;

public final class TenantStatusTransitions {

	private TenantStatusTransitions() {
	}

	public static boolean allowed(String from, String to) {
		return ("ACTIVE".equals(from) && "SUSPENDED".equals(to)) || ("SUSPENDED".equals(from) && "ACTIVE".equals(to));
	}

}
