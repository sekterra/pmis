package com.tommy.common.global.security;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.stereotype.Service;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

@Service
public class JwtTokenService implements JwtDecoder {

	public static final String TENANT_AUDIENCE = "mdm-api";

	public static final String PLATFORM_AUDIENCE = "mdm-platform";

	public static final String TENANT_TYPE = "tenant-user";

	public static final String PLATFORM_TYPE = "platform";

	private final JwtSigningKeys keys;

	private final String issuer;

	public JwtTokenService(MdmSecurityProperties properties, org.springframework.core.env.Environment environment) {
		boolean dev = java.util.Arrays.stream(environment.getActiveProfiles()).anyMatch("dev"::equals);
		this.keys = JwtSigningKeys.load(properties.getPrivateKeyPath(), properties.getPreviousPublicKeyPath(), dev);
		this.issuer = properties.getIssuer();
	}

	public JwtSigningKeys keys() {
		return keys;
	}

	public String issue(String subject, String audience, String type, String tenant, boolean passwordChangeRequired, Duration ttl) {
		return issue(subject, audience, type, tenant, passwordChangeRequired, ttl, java.util.Map.of());
	}

	public String issue(String subject, String audience, String type, String tenant, boolean passwordChangeRequired, Duration ttl,
			java.util.Map<String, Object> extra) {
		Instant now = Instant.now();
		Instant exp = now.plus(ttl);
		try {
			JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder()
					.issuer(issuer)
					.subject(subject)
					.audience(audience)
					.jwtID(UUID.randomUUID().toString())
					.issueTime(Date.from(now))
					.expirationTime(Date.from(exp))
					.claim("typ", type)
					.claim("pwd_change_required", passwordChangeRequired);
			if (tenant != null) {
				claims.claim("tenant", tenant);
			}
			if (extra != null) {
				extra.forEach((name, value) -> {
					if (value != null) {
						claims.claim(name, value);
					}
				});
			}
			SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(keys.kid()).build(), claims.build());
			jwt.sign(new RSASSASigner(keys.privateKey()));
			return jwt.serialize();
		}
		catch (Exception ex) {
			throw new IllegalStateException("JWT 를 발급하지 못했습니다.", ex);
		}
	}

	@Override
	public Jwt decode(String token) {
		try {
			SignedJWT signed = SignedJWT.parse(token);
			String kid = signed.getHeader().getKeyID();
			var publicKey = keys.select(kid);
			if (publicKey == null || !signed.verify(new RSASSAVerifier(publicKey))) {
				throw new BadJwtException("rejected");
			}
			JWTClaimsSet claims = signed.getJWTClaimsSet();
			Date expiration = claims.getExpirationTime();
			if (expiration == null || !expiration.after(new Date())) {
				throw new JwtValidationException("expired", List.of(new OAuth2Error("invalid_token", "expired", null)));
			}
			if (!issuer.equals(claims.getIssuer())) {
				throw new BadJwtException("rejected");
			}
			Instant issuedAt = claims.getIssueTime() == null ? Instant.now() : claims.getIssueTime().toInstant();
			return Jwt.withTokenValue(token)
					.header("alg", "RS256")
					.header("kid", kid)
					.subject(claims.getSubject())
					.issuer(claims.getIssuer())
					.audience(claims.getAudience())
					.issuedAt(issuedAt)
					.expiresAt(expiration.toInstant())
					.claims(map -> claims.getClaims().forEach((name, value) -> {
						if (!java.util.Set.of("iss", "sub", "aud", "exp", "iat", "nbf").contains(name)) {
							map.put(name, value);
						}
					}))
					.build();
		}
		catch (BadJwtException ex) {
			throw ex;
		}
		catch (Exception ex) {
			throw new BadJwtException("malformed", ex);
		}
	}

}
