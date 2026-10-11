package com.tommy.common.global.multitenancy;

public final class SchemaNames {

	public static final String PUBLIC_SCHEMA = "pmis";

	private static final java.util.regex.Pattern TENANT = java.util.regex.Pattern.compile("^t_[a-z0-9_]{1,61}$");

	private static final java.util.regex.Pattern TENANT_CODE = java.util.regex.Pattern.compile("^[a-z0-9]+(-[a-z0-9]+)*$");

	private SchemaNames() {
	}

	public static void requireTenantSchema(String schemaName) {
		if (!isTenantSchema(schemaName)) {
			throw new IllegalArgumentException("테넌트 스키마명이 아닙니다.");
		}
	}

	public static void requireSearchPath(String schemaName) {
		if (!PUBLIC_SCHEMA.equals(schemaName) && !isTenantSchema(schemaName)) {
			throw new IllegalArgumentException("search_path 로 쓸 수 없는 스키마명입니다.");
		}
	}

	public static void requireTenantCode(String tenantCode) {
		if (tenantCode == null || tenantCode.length() > 50 || !TENANT_CODE.matcher(tenantCode).matches()) {
			throw new IllegalArgumentException("테넌트 코드 형식이 아닙니다.");
		}
	}

	public static String schemaNameForTenantCode(String tenantCode) {
		requireTenantCode(tenantCode);
		String schemaName = "t_" + tenantCode.replace('-', '_');
		requireTenantSchema(schemaName);
		return schemaName;
	}

	public static boolean isTenantSchema(String schemaName) {
		return schemaName != null
				&& TENANT.matcher(schemaName).matches()
				&& !schemaName.startsWith("pg_")
				&& !"public".equals(schemaName)
				&& !PUBLIC_SCHEMA.equals(schemaName)
				&& !"information_schema".equals(schemaName);
	}

}
