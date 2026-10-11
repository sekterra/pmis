package com.tommy.common.global.security;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

final class PemKeys {

	private PemKeys() {
	}

	static RSAPrivateKey readPrivateKey(String path) {
		try {
			byte[] der = der(Files.readString(Path.of(path)));
			return (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(der));
		}
		catch (Exception ex) {
			throw new IllegalStateException("JWT 개인키를 읽지 못했습니다.", ex);
		}
	}

	static RSAPublicKey readPublicKey(String path) {
		try {
			byte[] der = der(Files.readString(Path.of(path)));
			return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(der));
		}
		catch (Exception ex) {
			throw new IllegalStateException("JWT 공개키를 읽지 못했습니다.", ex);
		}
	}

	static RSAPublicKey publicFromPrivate(RSAPrivateKey privateKey) {
		try {
			RSAPrivateCrtKey crt = (RSAPrivateCrtKey) privateKey;
			return (RSAPublicKey) KeyFactory.getInstance("RSA")
					.generatePublic(new RSAPublicKeySpec(crt.getModulus(), crt.getPublicExponent()));
		}
		catch (Exception ex) {
			throw new IllegalStateException("JWT 공개키를 유도하지 못했습니다.", ex);
		}
	}

	private static byte[] der(String pem) {
		String body = pem.replace("-----BEGIN PRIVATE KEY-----", "")
				.replace("-----END PRIVATE KEY-----", "")
				.replace("-----BEGIN PUBLIC KEY-----", "")
				.replace("-----END PUBLIC KEY-----", "")
				.replaceAll("\\s", "");
		return Base64.getDecoder().decode(body);
	}

}
