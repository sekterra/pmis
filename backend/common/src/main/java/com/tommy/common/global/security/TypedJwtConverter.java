package com.tommy.common.global.security;

import java.util.List;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

final class TypedJwtConverter implements Converter<Jwt, AbstractAuthenticationToken> {

	private final JwtAuthenticationConverter delegate = new JwtAuthenticationConverter();

	private final String audience;

	private final String type;

	TypedJwtConverter(String audience, String type) {
		this.audience = audience;
		this.type = type;
		this.delegate.setJwtGrantedAuthoritiesConverter(jwt -> List.of());
	}

	@Override
	public AbstractAuthenticationToken convert(Jwt jwt) {
		List<String> audiences = jwt.getAudience();
		if (audiences == null || !audiences.contains(audience) || !type.equals(jwt.getClaimAsString("typ"))) {
			throw new OAuth2AuthenticationException(new OAuth2Error("invalid_token", "rejected", null));
		}
		if (JwtTokenService.PLATFORM_TYPE.equals(type) && jwt.getClaim("tenant") != null) {
			throw new OAuth2AuthenticationException(new OAuth2Error("invalid_token", "rejected", null));
		}
		return delegate.convert(jwt);
	}

}
