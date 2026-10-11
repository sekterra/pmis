package com.tommy.common.global.security;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nimbusds.jose.jwk.RSAKey;

public record JwtSigningKeys(RSAPrivateKey privateKey, RSAPublicKey publicKey, String kid, RSAPublicKey previousPublicKey,
		String previousKid) {

	private static final Logger log = LoggerFactory.getLogger(JwtSigningKeys.class);

	public static JwtSigningKeys load(String privateKeyPath, String previousPublicKeyPath, boolean ephemeralAllowed) {
		if (privateKeyPath == null || privateKeyPath.isBlank()) {
			if (!ephemeralAllowed) {
				throw new IllegalStateException("JWT 개인키 경로가 없습니다.");
			}
			JwtSigningKeys generated = generate();
			log.warn("임시 JWT 서명 키를 메모리에 만들었습니다. kid={}", generated.kid());
			return withPrevious(generated, previousPublicKeyPath);
		}
		RSAPrivateKey privateKey = PemKeys.readPrivateKey(privateKeyPath);
		RSAPublicKey publicKey = PemKeys.publicFromPrivate(privateKey);
		String kid = thumbprint(publicKey);
		return withPrevious(new JwtSigningKeys(privateKey, publicKey, kid, null, null), previousPublicKeyPath);
	}

	private static JwtSigningKeys withPrevious(JwtSigningKeys current, String previousPublicKeyPath) {
		if (previousPublicKeyPath == null || previousPublicKeyPath.isBlank()) {
			return current;
		}
		RSAPublicKey previous = PemKeys.readPublicKey(previousPublicKeyPath);
		return new JwtSigningKeys(current.privateKey(), current.publicKey(), current.kid(), previous, thumbprint(previous));
	}

	public static JwtSigningKeys generate() {
		try {
			KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
			generator.initialize(2048);
			KeyPair pair = generator.generateKeyPair();
			RSAPublicKey publicKey = (RSAPublicKey) pair.getPublic();
			return new JwtSigningKeys((RSAPrivateKey) pair.getPrivate(), publicKey, thumbprint(publicKey), null, null);
		}
		catch (Exception ex) {
			throw new IllegalStateException("임시 JWT 키를 만들지 못했습니다.", ex);
		}
	}

	public static String thumbprint(RSAPublicKey publicKey) {
		try {
			return new RSAKey.Builder(publicKey).build().computeThumbprint().toString();
		}
		catch (Exception ex) {
			throw new IllegalStateException("JWT kid 를 계산하지 못했습니다.", ex);
		}
	}

	public RSAPublicKey select(String keyId) {
		if (kid.equals(keyId)) {
			return publicKey;
		}
		if (previousKid != null && previousKid.equals(keyId)) {
			return previousPublicKey;
		}
		return null;
	}

	public String publicJwkJson() {
		try {
			StringBuilder json = new StringBuilder();
			json.append("{\"keys\":[");
			json.append(jwk(publicKey, kid));
			if (previousPublicKey != null) {
				json.append(',').append(jwk(previousPublicKey, previousKid));
			}
			json.append("]}");
			return json.toString();
		}
		catch (Exception ex) {
			throw new IllegalStateException("JWKS 를 만들지 못했습니다.", ex);
		}
	}

	private static String jwk(RSAPublicKey key, String keyId) throws Exception {
		RSAKey jwk = new RSAKey.Builder(key).keyID(keyId).build().toPublicJWK();
		return jwk.toJSONString();
	}

	public static String pem(byte[] der, String label) {
		String body = Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(der);
		return "-----BEGIN " + label + "-----\n" + body + "\n-----END " + label + "-----\n";
	}

}
