package com.tommy.common.global.multitenancy;

public final class DatabaseNameGuard {

	public static final String EXPECTED = SchemaNames.PUBLIC_SCHEMA;

	private DatabaseNameGuard() {
	}

	public static void require(String actualDatabaseName) {
		if (!EXPECTED.equals(actualDatabaseName)) {
			throw new IllegalStateException("접속 DB가 pmis 가 아닙니다.");
		}
	}

}
