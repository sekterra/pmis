package com.tommy.common.global.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class BcryptPasswordHasher implements PasswordHasher {

	private final BCryptPasswordEncoder encoder;

	public BcryptPasswordHasher(@Value("${mdm.security.bcrypt-strength:10}") int strength) {
		if (strength < 4 || strength > 31) {
			throw new IllegalArgumentException("BCrypt 강도는 4 이상 31 이하여야 합니다.");
		}
		this.encoder = new BCryptPasswordEncoder(strength);
	}

	@Override
	public String hash(String rawPassword) {
		return encoder.encode(rawPassword);
	}

	@Override
	public boolean matches(String rawPassword, String passwordHash) {
		return encoder.matches(rawPassword, passwordHash);
	}

}
