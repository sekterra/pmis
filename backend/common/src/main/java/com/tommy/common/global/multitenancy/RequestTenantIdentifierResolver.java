package com.tommy.common.global.multitenancy;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;

import com.tommy.common.global.exception.ApiException;

public class RequestTenantIdentifierResolver implements CurrentTenantIdentifierResolver<String> {

	@Override
	public String resolveCurrentTenantIdentifier() {
		if (!TenantContext.inRequest()) {
			return SchemaNames.PUBLIC_SCHEMA;
		}
		String schemaName = TenantContext.schema();
		if (schemaName == null || schemaName.isBlank()) {
			throw new ApiException(400, "TENANT.UNRESOLVED", "테넌트를 식별할 수 없습니다.", 2);
		}
		SchemaNames.requireSearchPath(schemaName);
		return schemaName;
	}

	@Override
	public boolean validateExistingCurrentSessions() {
		return true;
	}

	@Override
	public boolean isRoot(String tenantId) {
		return SchemaNames.PUBLIC_SCHEMA.equals(tenantId);
	}

}
