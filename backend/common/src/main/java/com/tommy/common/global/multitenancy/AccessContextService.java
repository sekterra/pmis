package com.tommy.common.global.multitenancy;

import java.util.Arrays;
import java.util.Optional;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tommy.common.global.exception.ApiException;

@Service
public class AccessContextService {

	private final PlatformHostProperties platformHosts;

	private final Optional<HeaderTenantResolver> headerTenantResolver;

	private final TenantDirectory directory;

	private final Environment environment;

	public AccessContextService(PlatformHostProperties platformHosts, Optional<HeaderTenantResolver> headerTenantResolver,
			TenantDirectory directory, Environment environment) {
		this.platformHosts = platformHosts;
		this.headerTenantResolver = headerTenantResolver;
		this.directory = directory;
		this.environment = environment;
	}

	public ContextView resolve(jakarta.servlet.http.HttpServletRequest request) {
		boolean dev = Arrays.stream(environment.getActiveProfiles()).anyMatch("dev"::equals);
		Optional<String> header = dev ? headerTenantResolver.flatMap(resolver -> resolver.resolveTenantCode(request)) : Optional.empty();
		if (header.isPresent()) {
			TenantSnapshot snapshot = directory.findByTenantCode(header.get()).orElse(null);
			if (snapshot == null) {
				throw new ApiException(404, "TENANT.NOT_FOUND", "테넌트를 찾을 수 없습니다.", 2);
			}
			return tenant(snapshot);
		}
		String host = PlatformHostProperties.requestHost(request);
		if (platformHosts.matches(host)) {
			return new ContextView("PLATFORM", null, null);
		}
		TenantSnapshot byHost = directory.findByDomain(host).orElse(null);
		if (byHost == null) {
			throw new ApiException(400, "TENANT.UNRESOLVED", "테넌트를 식별할 수 없습니다.", 2);
		}
		return tenant(byHost);
	}

	private ContextView tenant(TenantSnapshot snapshot) {
		TenantAccess.requireActiveSchema(snapshot);
		String name = directory.findTenantName(snapshot.tenantCode()).orElse(null);
		return new ContextView("TENANT", snapshot.tenantCode(), name);
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record ContextView(String kind, String tenantCode, String tenantName) {
	}

}
