package com.tommy.common.global.multitenancy;

public final class TenantContext {

	private static final ThreadLocal<String> SCHEMA = new ThreadLocal<>();
	private static final ThreadLocal<Boolean> REQUEST = new ThreadLocal<>();

	private TenantContext() {
	}

	public static void beginRequest() {
		REQUEST.set(Boolean.TRUE);
	}

	public static void setSchema(String schemaName) {
		SCHEMA.set(schemaName);
	}

	public static String schema() {
		return SCHEMA.get();
	}

	public static boolean inRequest() {
		return Boolean.TRUE.equals(REQUEST.get());
	}

	public static void clear() {
		SCHEMA.remove();
		REQUEST.remove();
	}

}
