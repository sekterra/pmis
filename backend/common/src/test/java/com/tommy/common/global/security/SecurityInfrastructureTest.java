package com.tommy.common.global.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtValidationException;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import jakarta.servlet.FilterChain;
import tools.jackson.databind.json.JsonMapper;

class SecurityInfrastructureTest {

	private final ApplicationContextRunner runner = new ApplicationContextRunner().withUserConfiguration(GuardConfig.class);

	@Test
	void refusesPermitAllAndInsecureCookieOutsideDev() {
		runner.withPropertyValues("spring.profiles.active=local", "mdm.security.permit-all=true")
				.run(context -> assertThat(context).hasFailed());
		runner.withPropertyValues("spring.profiles.active=test", "mdm.security.permit-all=true")
				.run(context -> assertThat(context).hasFailed());
		runner.withPropertyValues("spring.profiles.active=local", "mdm.security.cookie.secure=false")
				.run(context -> assertThat(context).hasFailed());
		runner.withPropertyValues("spring.profiles.active=dev", "mdm.security.permit-all=true")
				.run(context -> assertThat(context).hasNotFailed());
		assertThrows(IllegalStateException.class, () -> JwtSigningKeys.load("", "", false));
	}

	@Test
	void issuesAndVerifiesJwtAndPublishesOnlyPublicJwks() throws Exception {
		JwtSigningKeys previous = JwtSigningKeys.generate();
		Path previousPublic = Files.createTempFile("mdm-prev", ".pem");
		previousPublic.toFile().deleteOnExit();
		Files.writeString(previousPublic, JwtSigningKeys.pem(previous.publicKey().getEncoded(), "PUBLIC KEY"));
		MdmSecurityProperties properties = new MdmSecurityProperties();
		properties.setIssuer("http://localhost");
		properties.setPreviousPublicKeyPath(previousPublic.toString());
		Environment environment = mock(Environment.class);
		when(environment.getActiveProfiles()).thenReturn(new String[] { "dev" });
		JwtTokenService tokens = new JwtTokenService(properties, environment);
		assertEquals(tokens.keys().kid(), JwtSigningKeys.thumbprint(tokens.keys().publicKey()));
		String access = tokens.issue("7", JwtTokenService.TENANT_AUDIENCE, JwtTokenService.TENANT_TYPE, "acme", true,
				Duration.ofMinutes(5));
		Jwt jwt = tokens.decode(access);
		assertEquals("7", jwt.getSubject());
		assertEquals("acme", jwt.getClaimAsString("tenant"));
		assertEquals(Boolean.TRUE, jwt.getClaim("pwd_change_required"));
		assertThrows(JwtValidationException.class, () -> tokens.decode(tokens.issue("7", JwtTokenService.TENANT_AUDIENCE,
				JwtTokenService.TENANT_TYPE, "acme", false, Duration.ofSeconds(-30))));
		assertThrows(BadJwtException.class, () -> tokens.decode(access.substring(0, access.length() - 2) + "aa"));
		JwtTokenService other = new JwtTokenService(new MdmSecurityProperties(), environment);
		assertThrows(BadJwtException.class, () -> tokens.decode(other.issue("1", JwtTokenService.TENANT_AUDIENCE,
				JwtTokenService.TENANT_TYPE, "acme", false, Duration.ofMinutes(5))));
		String previousToken = sign(previous, properties.getIssuer());
		assertEquals(previous.kid(), tokens.keys().previousKid());
		assertEquals(previous.publicKey().getModulus(), tokens.keys().previousPublicKey().getModulus());
		Jwt accepted = tokens.decode(previousToken);
		assertEquals("9", accepted.getSubject());
		String jwks = tokens.keys().publicJwkJson();
		assertFalse(jwks.contains("\"d\""));
		assertFalse(jwks.contains("\"p\""));
		assertFalse(jwks.contains("\"q\""));
		assertFalse(jwks.contains("\"dp\""));
		assertFalse(jwks.contains("\"dq\""));
		assertFalse(jwks.contains("\"qi\""));
		assertTrue(jwks.contains(tokens.keys().kid()));
		assertTrue(jwks.contains(tokens.keys().previousKid()));
	}

	@Test
	void anonymousBundleRequiresEveryNamespaceToBePublic() throws Exception {
		MdmSecurityProperties properties = new MdmSecurityProperties();
		PublicNamespaceFilter filter = new PublicNamespaceFilter(properties, JsonMapper.builder().build());
		FilterChain chain = mock(FilterChain.class);
		MockHttpServletRequest denied = new MockHttpServletRequest("GET", "/api/mdm/v1/i18n/bundles/menu");
		MockHttpServletResponse response = new MockHttpServletResponse();
		filter.doFilter(denied, response, chain);
		assertEquals(401, response.getStatus());
		assertTrue(response.getContentAsString().contains("AUTH.UNAUTHORIZED"));
		verify(chain, never()).doFilter(denied, response);
		MockHttpServletRequest allowed = new MockHttpServletRequest("GET", "/api/mdm/v1/i18n/bundles");
		allowed.setParameter("namespaces", "common,error,auth");
		filter.doFilter(allowed, new MockHttpServletResponse(), chain);
		verify(chain).doFilter(org.mockito.ArgumentMatchers.eq(allowed), org.mockito.ArgumentMatchers.any());
	}

	private static String sign(JwtSigningKeys keys, String issuer) throws Exception {
		JWTClaimsSet claims = new JWTClaimsSet.Builder().issuer(issuer).subject("9").audience(JwtTokenService.TENANT_AUDIENCE)
				.issueTime(Date.from(Instant.now())).expirationTime(Date.from(Instant.now().plusSeconds(60)))
				.claim("typ", JwtTokenService.TENANT_TYPE).claim("tenant", "acme").claim("pwd_change_required", false).build();
		SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(keys.kid()).build(), claims);
		jwt.sign(new RSASSASigner(keys.privateKey()));
		return jwt.serialize();
	}

	@Configuration
	@EnableConfigurationProperties(MdmSecurityProperties.class)
	static class GuardConfig {

		@Bean
		MdmSecurityGuard mdmSecurityGuard(MdmSecurityProperties properties, Environment environment) {
			return new MdmSecurityGuard(properties, environment);
		}

	}

}
