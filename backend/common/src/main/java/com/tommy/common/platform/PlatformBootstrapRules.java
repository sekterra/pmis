package com.tommy.common.platform;

public final class PlatformBootstrapRules {

	private PlatformBootstrapRules() {
	}

	public static boolean empty(long rowCount) {
		return rowCount == 0L;
	}

}
