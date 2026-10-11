package com.tommy.common.global.multitenancy;

import com.tommy.common.global.exception.ApiException;

public final class TenantAccess {

	private TenantAccess() {
	}

	public static String requireActiveSchema(TenantSnapshot snapshot) {
		if (snapshot == null) {
			throw new ApiException(404, "TENANT.NOT_FOUND", "테넌트를 찾을 수 없습니다.", 2);
		}
		SchemaNames.requireTenantSchema(snapshot.schemaName());
		if (!snapshot.active()) {
			throw new ApiException(403, "TENANT.INACTIVE", "테넌트를 사용할 수 없습니다.", 2);
		}
		return snapshot.schemaName();
	}

}
