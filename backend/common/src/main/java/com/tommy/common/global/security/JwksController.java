package com.tommy.common.global.security;

import java.time.Duration;
import java.util.Map;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@RestController
public class JwksController {

	private final JwtTokenService tokens;

	private final ObjectMapper objectMapper;

	public JwksController(JwtTokenService tokens, ObjectMapper objectMapper) {
		this.tokens = tokens;
		this.objectMapper = objectMapper;
	}

	@GetMapping("/api/mdm/v1/auth/jwks")
	public ResponseEntity<Map<String, Object>> jwks() {
		Map<String, Object> body = objectMapper.readValue(tokens.keys().publicJwkJson(), new TypeReference<>() {
		});
		return ResponseEntity.ok().cacheControl(CacheControl.maxAge(Duration.ofSeconds(300)).cachePublic()).body(body);
	}

}
