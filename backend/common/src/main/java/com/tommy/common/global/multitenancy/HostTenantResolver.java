package com.tommy.common.global.multitenancy;

import java.util.Optional;

import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;

@Component
public class HostTenantResolver implements TenantResolver {

	private final TenantDirectory directory;

	public HostTenantResolver(TenantDirectory directory) {
		this.directory = directory;
	}

	@Override
	public Optional<String> resolveTenantCode(HttpServletRequest request) {
		String host = request.getServerName();
		if (host == null || host.isBlank()) {
			return Optional.empty();
		}
		return directory.findByDomain(host).map(TenantSnapshot::tenantCode);
	}

}
