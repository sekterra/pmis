package com.tommy.common.global.security;

import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import tools.jackson.databind.ObjectMapper;

@Configuration
@ConditionalOnWebApplication
public class ApiSecurityConfig implements WebMvcConfigurer {

	private final PermissionInterceptor permissionInterceptor;

	private final MdmSecurityProperties properties;

	private final JwtTokenService jwtTokenService;

	private final ApiAuthenticationEntryPoint entryPoint;

	private final ObjectMapper objectMapper;

	public ApiSecurityConfig(PermissionInterceptor permissionInterceptor, MdmSecurityProperties properties,
			JwtTokenService jwtTokenService, ApiAuthenticationEntryPoint entryPoint, ObjectMapper objectMapper) {
		this.permissionInterceptor = permissionInterceptor;
		this.properties = properties;
		this.jwtTokenService = jwtTokenService;
		this.entryPoint = entryPoint;
		this.objectMapper = objectMapper;
	}

	@Bean
	@Order(1)
	SecurityFilterChain platformSecurityFilterChain(HttpSecurity http) throws Exception {
		base(http);
		http.securityMatcher("/api/platform/**")
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(HttpMethod.POST, "/api/platform/v1/auth/login", "/api/platform/v1/auth/refresh").permitAll()
						.anyRequest().authenticated())
				.oauth2ResourceServer(oauth -> oauth.authenticationEntryPoint(entryPoint)
						.jwt(jwt -> jwt.decoder(jwtTokenService)
						.jwtAuthenticationConverter(new TypedJwtConverter(JwtTokenService.PLATFORM_AUDIENCE, JwtTokenService.PLATFORM_TYPE))));
		return http.build();
	}

	@Bean
	@Order(2)
	SecurityFilterChain mdmSecurityFilterChain(HttpSecurity http) throws Exception {
		base(http);
		http.securityMatcher("/api/mdm/**")
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(HttpMethod.POST, "/api/mdm/v1/auth/login", "/api/mdm/v1/auth/refresh",
								"/api/mdm/v1/auth/support-access/exchange").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/mdm/v1/auth/jwks", "/api/mdm/v1/i18n/languages", "/api/mdm/v1/context").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/mdm/v1/i18n/bundles", "/api/mdm/v1/i18n/bundles/**").permitAll()
						.anyRequest().authenticated())
				.oauth2ResourceServer(oauth -> oauth.authenticationEntryPoint(entryPoint)
						.jwt(jwt -> jwt.decoder(jwtTokenService)
						.jwtAuthenticationConverter(new TypedJwtConverter(JwtTokenService.TENANT_AUDIENCE, JwtTokenService.TENANT_TYPE))))
				.addFilterAfter(new PublicNamespaceFilter(properties, objectMapper), BearerTokenAuthenticationFilter.class);
		return http.build();
	}

	@Bean
	@Order(3)
	SecurityFilterChain fallbackSecurityFilterChain(HttpSecurity http) throws Exception {
		base(http);
		http.authorizeHttpRequests(auth -> auth.requestMatchers("/actuator/health").permitAll().anyRequest().authenticated());
		return http.build();
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(permissionInterceptor);
	}

	private void base(HttpSecurity http) throws Exception {
		http.csrf(AbstractHttpConfigurer::disable)
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.exceptionHandling(handling -> handling.authenticationEntryPoint(entryPoint));
		List<String> origins = properties.getCors().getAllowedOrigins().stream().filter(origin -> origin != null && !origin.isBlank()).toList();
		if (!origins.isEmpty()) {
			http.cors(cors -> cors.configurationSource(corsSource(origins)));
		}
	}

	private static CorsConfigurationSource corsSource(List<String> origins) {
		return request -> {
			CorsConfiguration configuration = new CorsConfiguration();
			configuration.setAllowedOrigins(origins);
			configuration.setAllowCredentials(true);
			configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
			configuration.setAllowedHeaders(List.of("*"));
			return configuration;
		};
	}

}
