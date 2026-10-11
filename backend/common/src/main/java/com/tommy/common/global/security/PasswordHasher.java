package com.tommy.common.global.security;

public interface PasswordHasher {

	String hash(String rawPassword);

	boolean matches(String rawPassword, String passwordHash);

}
