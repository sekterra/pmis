package com.tommy.common.global.multitenancy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.core.env.Environment;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.tommy.common.global.exception.ApiException;
import com.tommy.common.global.exception.ApiExceptionHandler;

import jakarta.servlet.http.HttpServletRequest;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AccessContextSliceTest {

	@Mock
	private TenantDirectory directory;

	@Mock
	private Environment environment;

	@Test
	void choosesHeaderThenPlatformHostThenTenantHost() {
		when(environment.getActiveProfiles()).thenReturn(new String[] { "dev" });
		PlatformHostProperties hosts = new PlatformHostProperties();
		hosts.setHosts(" LocalHost:8080, 127.0.0.1 ");
		AccessContextService service = new AccessContextService(hosts, Optional.of(new HeaderTenantResolver()), directory, environment);
		when(directory.findByTenantCode("acme")).thenReturn(Optional.of(new TenantSnapshot("acme", "t_acme", "ACTIVE", true)));
		when(directory.findTenantName("acme")).thenReturn(Optional.of("에이씨엠이"));
		HttpServletRequest header = request("LOCALHOST:9", "acme");
		AccessContextService.ContextView tenant = service.resolve(header);
		assertEquals("TENANT", tenant.kind());
		assertEquals("acme", tenant.tenantCode());
		assertEquals("에이씨엠이", tenant.tenantName());
		assertFalse(tenant.toString().contains("t_acme"));

		AccessContextService.ContextView platform = service.resolve(request("localhost:8080", null));
		assertEquals("PLATFORM", platform.kind());
		assertEquals(null, platform.tenantCode());

		when(directory.findByDomain("tenant.example")).thenReturn(Optional.of(new TenantSnapshot("beta", "t_beta", "ACTIVE", true)));
		when(directory.findTenantName("beta")).thenReturn(Optional.of("베타"));
		AccessContextService.ContextView byHost = service.resolve(request("TENANT.EXAMPLE:443", null));
		assertEquals("beta", byHost.tenantCode());
		verify(directory).findByDomain("tenant.example");
	}

	@Test
	void rejectsUnknownHostAndMissingHeaderTenant() {
		when(environment.getActiveProfiles()).thenReturn(new String[] { "dev" });
		PlatformHostProperties hosts = new PlatformHostProperties();
		hosts.setHosts("localhost");
		AccessContextService service = new AccessContextService(hosts, Optional.of(new HeaderTenantResolver()), directory, environment);
		when(directory.findByDomain("missing.example")).thenReturn(Optional.empty());
		ApiException unknown = assertThrows(ApiException.class, () -> service.resolve(request("missing.example", null)));
		assertEquals("TENANT.UNRESOLVED", unknown.errorCode());
		when(directory.findByTenantCode("gone")).thenReturn(Optional.empty());
		ApiException missing = assertThrows(ApiException.class, () -> service.resolve(request("localhost", "gone")));
		assertEquals("TENANT.NOT_FOUND", missing.errorCode());
		when(directory.findByDomain("closed.example")).thenReturn(Optional.of(new TenantSnapshot("closed", "t_closed", "SUSPENDED", true)));
		ApiException inactive = assertThrows(ApiException.class, () -> service.resolve(request("closed.example", null)));
		assertEquals("TENANT.INACTIVE", inactive.errorCode());
	}

	@Test
	void contextIsPublicAndDoesNotStoreTheResponse() throws Exception {
		when(environment.getActiveProfiles()).thenReturn(new String[] { "prod" });
		PlatformHostProperties hosts = new PlatformHostProperties();
		hosts.setHosts("console.example");
		AccessContextService service = new AccessContextService(hosts, Optional.empty(), directory, environment);
		MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new AccessContextController(service))
				.setControllerAdvice(new ApiExceptionHandler())
				.build();
		mockMvc.perform(get("/api/mdm/v1/context").header("Host", "console.example"))
				.andExpect(status().isOk())
				.andExpect(header().string("Cache-Control", "no-store"))
				.andExpect(jsonPath("$.data.kind").value("PLATFORM"))
				.andExpect(jsonPath("$.data.tenantCode").doesNotExist())
				.andExpect(jsonPath("$.data.schemaName").doesNotExist());
	}

	private static HttpServletRequest request(String host, String tenant) {
		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getHeader("Host")).thenReturn(host);
		when(request.getHeader(HeaderTenantResolver.HEADER)).thenReturn(tenant);
		return request;
	}

}
