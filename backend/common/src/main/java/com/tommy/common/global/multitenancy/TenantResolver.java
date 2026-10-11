package com.tommy.common.global.multitenancy;

import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;

public interface TenantResolver {

	Optional<String> resolveTenantCode(HttpServletRequest request);

}
