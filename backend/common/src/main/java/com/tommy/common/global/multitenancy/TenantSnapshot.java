package com.tommy.common.global.multitenancy;

public record TenantSnapshot(String tenantCode, String schemaName, String statusCode, boolean inUse) {

	public boolean active() {
		return inUse && "ACTIVE".equals(statusCode);
	}

}
