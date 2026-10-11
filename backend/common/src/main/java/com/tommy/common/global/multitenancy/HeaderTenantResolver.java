package com.tommy.common.global.multitenancy;

import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;

public class HeaderTenantResolver implements TenantResolver {

	public static final String HEADER = "X-Tenant-Code";

	@Override
	public Optional<String> resolveTenantCode(HttpServletRequest request) {
		String value = request.getHeader(HEADER);
		if (value == null || value.isBlank()) {
			return Optional.empty();
		}
		return Optional.of(value.trim());
	}

}
