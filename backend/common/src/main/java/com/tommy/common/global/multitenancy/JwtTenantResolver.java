package com.tommy.common.global.multitenancy;

import java.util.Optional;

import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;

@Component
public class JwtTenantResolver implements TenantResolver {

	private final TenantClaimReader claimReader;

	public JwtTenantResolver(TenantClaimReader claimReader) {
		this.claimReader = claimReader;
	}

	@Override
	public Optional<String> resolveTenantCode(HttpServletRequest request) {
		return claimReader.readTenantCode(request);
	}

}
