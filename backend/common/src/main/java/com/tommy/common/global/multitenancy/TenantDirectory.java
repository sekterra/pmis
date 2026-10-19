package com.tommy.common.global.multitenancy;

import java.util.Optional;

public interface TenantDirectory {

	Optional<TenantSnapshot> findByTenantCode(String tenantCode);

	Optional<TenantSnapshot> findByDomain(String domainName);

	default Optional<String> findTenantName(String tenantCode) {
		return Optional.empty();
	}

	void evict(String tenantCode);

}
