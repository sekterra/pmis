package com.tommy.common.tenant.provision;

import java.security.SecureRandom;

public final class TemporaryPassword {

	private static final char[] UPPER = "ABCDEFGHJKLMNPQRSTUVWXYZ".toCharArray();

	private static final char[] LOWER = "abcdefghijkmnopqrstuvwxyz".toCharArray();

	private static final char[] DIGITS = "23456789".toCharArray();

	private static final char[] SYMBOLS = "!@#$%^&*-_+=?".toCharArray();

	private static final char[] ALL = (new String(UPPER) + new String(LOWER) + new String(DIGITS) + new String(SYMBOLS))
			.toCharArray();

	private static final SecureRandom RANDOM = new SecureRandom();

	private final String value;

	private TemporaryPassword(String value) {
		this.value = value;
	}

	public static TemporaryPassword generate() {
		return generate(16);
	}

	public static TemporaryPassword generate(int length) {
		if (length < 4) {
			throw new IllegalArgumentException("임시 비밀번호 길이는 4 이상이어야 합니다.");
		}
		char[] chars = new char[length];
		chars[0] = pick(UPPER);
		chars[1] = pick(LOWER);
		chars[2] = pick(DIGITS);
		chars[3] = pick(SYMBOLS);
		for (int index = 4; index < chars.length; index++) {
			chars[index] = pick(ALL);
		}
		for (int index = chars.length - 1; index > 0; index--) {
			int swap = RANDOM.nextInt(index + 1);
			char current = chars[index];
			chars[index] = chars[swap];
			chars[swap] = current;
		}
		return new TemporaryPassword(new String(chars));
	}

	private static char pick(char[] alphabet) {
		return alphabet[RANDOM.nextInt(alphabet.length)];
	}

	public String reveal() {
		return value;
	}

	@Override
	public String toString() {
		return "TemporaryPassword[masked]";
	}

}
