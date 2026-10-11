package com.tommy.common.tenant.provision;

public record ProvisionTenantCommand(
		String tenantCode,
		String tenantName,
		String schemaName,
		String defaultLanguageCode,
		String adminName,
		String adminEmail,
		Long executedByUserPk) {
}
