package com.tommy.common.global.multitenancy;

import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;

public interface TenantClaimReader {

	Optional<String> readTenantCode(HttpServletRequest request);

}
