package com.tommy.common.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

public final class RefreshTokenValues {

	private static final SecureRandom RANDOM = new SecureRandom();

	private RefreshTokenValues() {
	}

	public static String raw() {
		byte[] bytes = new byte[32];
		RANDOM.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	public static String hash(String raw) {
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		}
		catch (Exception ex) {
			throw new IllegalStateException("Refresh 해시를 만들지 못했습니다.", ex);
		}
	}

}
