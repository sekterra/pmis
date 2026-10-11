package com.tommy.common.global.multitenancy;

import java.util.Optional;

public interface TenantDirectory {

	Optional<TenantSnapshot> findByTenantCode(String tenantCode);

	Optional<TenantSnapshot> findByDomain(String domainName);

	void evict(String tenantCode);

}
