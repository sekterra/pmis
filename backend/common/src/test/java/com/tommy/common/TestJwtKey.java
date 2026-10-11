package com.tommy.common;

import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.test.context.DynamicPropertyRegistry;

import com.tommy.common.global.security.JwtSigningKeys;

public final class TestJwtKey {

	private static final Path FILE = write();

	private TestJwtKey() {
	}

	public static void register(DynamicPropertyRegistry registry) {
		registry.add("mdm.security.private-key-path", FILE::toString);
	}

	private static Path write() {
		try {
			JwtSigningKeys keys = JwtSigningKeys.generate();
			Path file = Files.createTempFile("mdm-test-jwt", ".pem");
			file.toFile().deleteOnExit();
			Files.writeString(file, JwtSigningKeys.pem(keys.privateKey().getEncoded(), "PRIVATE KEY"));
			return file;
		}
		catch (Exception ex) {
			throw new IllegalStateException("테스트 JWT 키를 만들지 못했습니다.", ex);
		}
	}

}
