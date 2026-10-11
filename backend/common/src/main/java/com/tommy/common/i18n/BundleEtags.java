package com.tommy.common.i18n;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.TreeMap;

import com.tommy.common.i18n.dto.BundleView;

public final class BundleEtags {

	private BundleEtags() {
	}

	public static String weak(BundleView view) {
		StringBuilder body = new StringBuilder();
		body.append(view.namespaceCode()).append('\n').append(view.language()).append('\n');
		for (Map.Entry<String, String> entry : new TreeMap<>(view.messages()).entrySet()) {
			body.append(entry.getKey()).append('=').append(entry.getValue()).append('\n');
		}
		return "W/\"" + sha256(body.toString()) + "\"";
	}

	public static boolean matches(String ifNoneMatch, String etag) {
		if (ifNoneMatch == null || ifNoneMatch.isBlank()) {
			return false;
		}
		for (String part : ifNoneMatch.split(",")) {
			if (etag.equals(part.trim())) {
				return true;
			}
		}
		return false;
	}

	private static String sha256(String value) {
		try {
			byte[] hash = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
			StringBuilder hex = new StringBuilder(hash.length * 2);
			for (byte item : hash) {
				hex.append(String.format("%02x", item));
			}
			return hex.toString();
		}
		catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException(ex);
		}
	}

}
