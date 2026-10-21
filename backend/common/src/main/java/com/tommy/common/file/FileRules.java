package com.tommy.common.file;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import com.tommy.common.global.exception.ApiException;

import tools.jackson.databind.ObjectMapper;

public final class FileRules {

	public static final Set<String> DANGEROUS = Set.of("exe", "bat", "cmd", "com", "sh", "js", "jar", "msi", "dll", "scr", "ps1", "vbs");

	public static final Set<String> IMAGES = Set.of("png", "jpg", "jpeg", "gif", "webp");

	public static final Set<String> ZIP_FAMILY = Set.of("zip", "docx", "xlsx", "pptx", "odt", "ods", "odp");

	public static final Pattern SECRET_REF = Pattern.compile("^[A-Za-z0-9_.-]{1,200}$");

	public static final Pattern CODE = Pattern.compile("^[A-Z0-9_-]{1,50}$");

	public static final Pattern EXTENSION = Pattern.compile("^[a-z0-9]{1,20}$");

	private static final Set<String> SECRET_WORDS = Set.of("password", "secret", "token", "key");

	private FileRules() {
	}

	public static String sanitizeName(String raw) {
		String name = raw == null ? "" : raw.replace('\\', '/');
		int slash = name.lastIndexOf('/');
		if (slash >= 0) {
			name = name.substring(slash + 1);
		}
		StringBuilder builder = new StringBuilder();
		for (int index = 0; index < name.length(); index++) {
			char character = name.charAt(index);
			if (character >= 0x20 && character != 0x7f) {
				builder.append(character);
			}
		}
		name = builder.toString().trim();
		if (name.isEmpty() || ".".equals(name) || "..".equals(name)) {
			name = "file";
		}
		if (name.length() <= 300) {
			return name;
		}
		int dot = name.lastIndexOf('.');
		String extension = dot > 0 && name.length() - dot <= 21 ? name.substring(dot) : "";
		int keep = Math.max(1, 300 - extension.length());
		String head = name.substring(0, Math.min(keep, dot > 0 ? dot : name.length()));
		if (head.length() + extension.length() > 300) {
			head = head.substring(0, 300 - extension.length());
		}
		return head + extension;
	}

	public static String extensionOf(String name) {
		int dot = name.lastIndexOf('.');
		if (dot <= 0 || dot == name.length() - 1) {
			return null;
		}
		String extension = name.substring(dot + 1).toLowerCase(Locale.ROOT);
		return EXTENSION.matcher(extension).matches() ? extension : null;
	}

	public static void assertExtensionAllowed(String name, String allowedCsv) {
		String lower = name.toLowerCase(Locale.ROOT);
		String[] parts = lower.split("\\.", -1);
		if (parts.length >= 3) {
			for (int index = 1; index < parts.length - 1; index++) {
				if (DANGEROUS.contains(parts[index])) {
					rejectExtension();
				}
			}
		}
		String extension = extensionOf(name);
		if (extension != null && DANGEROUS.contains(extension)) {
			rejectExtension();
		}
		if (extension == null) {
			rejectExtension();
		}
		Set<String> allowed = parseExtensions(allowedCsv);
		if (!allowed.isEmpty() && !allowed.contains(extension)) {
			rejectExtension();
		}
	}

	public static String normalizeExtensions(String raw) {
		if (raw == null || raw.isBlank()) {
			return null;
		}
		StringBuilder builder = new StringBuilder();
		for (String part : raw.split(",")) {
			String extension = part.trim().toLowerCase(Locale.ROOT);
			if (!EXTENSION.matcher(extension).matches()) {
				throw new ApiException(400, "COMMON.VALIDATION_FAILED", "확장자 목록 형식이 올바르지 않습니다.", 2);
			}
			if (builder.length() > 0) {
				builder.append(',');
			}
			builder.append(extension);
		}
		String value = builder.toString();
		if (value.length() > 500) {
			throw new ApiException(400, "COMMON.VALIDATION_FAILED", "확장자 목록이 너무 깁니다.", 2);
		}
		return value;
	}

	public static Set<String> parseExtensions(String csv) {
		if (csv == null || csv.isBlank()) {
			return Set.of();
		}
		java.util.LinkedHashSet<String> values = new java.util.LinkedHashSet<>();
		for (String part : csv.split(",")) {
			String extension = part.trim().toLowerCase(Locale.ROOT);
			if (!extension.isEmpty()) {
				values.add(extension);
			}
		}
		return Set.copyOf(values);
	}

	public static String sniff(byte[] data) {
		if (data == null) {
			return null;
		}
		if (startsWith(data, new byte[] { (byte) 0x89, 0x50, 0x4e, 0x47 })) {
			return "image/png";
		}
		if (startsWith(data, new byte[] { (byte) 0xff, (byte) 0xd8, (byte) 0xff })) {
			return "image/jpeg";
		}
		if (startsWith(data, new byte[] { 'G', 'I', 'F', '8' })) {
			return "image/gif";
		}
		if (data.length >= 12 && startsWith(data, new byte[] { 'R', 'I', 'F', 'F' })
				&& data[8] == 'W' && data[9] == 'E' && data[10] == 'B' && data[11] == 'P') {
			return "image/webp";
		}
		if (startsWith(data, new byte[] { '%', 'P', 'D', 'F' })) {
			return "application/pdf";
		}
		if (data.length >= 4 && data[0] == 'P' && data[1] == 'K' && (data[2] == 3 || data[2] == 5 || data[2] == 7)) {
			return "application/zip";
		}
		return null;
	}

	public static void assertMime(String extension, byte[] data) {
		String sniffed = sniff(data);
		if (requiresSignature(extension)) {
			if (sniffed == null || !compatible(extension, sniffed)) {
				rejectMime();
			}
			return;
		}
		if (sniffed != null && !compatible(extension, sniffed)) {
			rejectMime();
		}
		if (sniffed == null && containsNul(data)) {
			rejectMime();
		}
	}

	public static String mimeType(String extension, byte[] data) {
		assertMime(extension, data);
		String sniffed = sniff(data);
		if (sniffed != null) {
			return sniffed;
		}
		if (extension == null) {
			return "application/octet-stream";
		}
		return switch (extension) {
			case "png" -> "image/png";
			case "jpg", "jpeg" -> "image/jpeg";
			case "gif" -> "image/gif";
			case "webp" -> "image/webp";
			case "pdf" -> "application/pdf";
			case "txt" -> "text/plain";
			case "csv" -> "text/csv";
			default -> ZIP_FAMILY.contains(extension) ? "application/zip" : "application/octet-stream";
		};
	}

	public static boolean imageExtension(String extension) {
		return extension != null && IMAGES.contains(extension);
	}

	public static String storageKey(LocalDateTime now) {
		return String.format("%04d/%02d/%s", now.getYear(), now.getMonthValue(), UUID.randomUUID());
	}

	public static void assertBasePath(String basePath) {
		if (basePath == null || basePath.isBlank() || basePath.indexOf('\0') >= 0 || basePath.contains("..")) {
			throw new ApiException(400, "FILE.INVALID_PATH", "저장 경로가 올바르지 않습니다.", 2);
		}
	}

	public static Path resolveBase(String localRoot, String basePath) {
		assertBasePath(basePath);
		Path base = Path.of(basePath);
		if (base.isAbsolute()) {
			return base.normalize();
		}
		if (localRoot == null || localRoot.isBlank()) {
			throw new ApiException(409, "FILE.NO_DEFAULT_STORAGE", "기본 저장소가 없습니다.", 2);
		}
		Path root = Path.of(localRoot).toAbsolutePath().normalize();
		Path resolved = root.resolve(base).normalize();
		if (!resolved.startsWith(root)) {
			throw new ApiException(400, "FILE.INVALID_PATH", "저장 경로가 올바르지 않습니다.", 2);
		}
		return resolved;
	}

	public static Path resolveFile(Path base, String tenantCode, String storageKey) {
		if (tenantCode == null || tenantCode.isBlank() || tenantCode.contains("..") || tenantCode.indexOf('/') >= 0
				|| tenantCode.indexOf('\\') >= 0) {
			throw new ApiException(400, "FILE.INVALID_PATH", "저장 경로가 올바르지 않습니다.", 2);
		}
		if (storageKey == null || storageKey.isBlank() || storageKey.contains("..") || storageKey.startsWith("/")
				|| storageKey.startsWith("\\")) {
			throw new ApiException(400, "FILE.INVALID_PATH", "저장 경로가 올바르지 않습니다.", 2);
		}
		Path tenant = base.resolve(tenantCode).normalize();
		Path target = tenant.resolve(storageKey).normalize();
		if (!target.startsWith(tenant)) {
			throw new ApiException(400, "FILE.INVALID_PATH", "저장 경로가 올바르지 않습니다.", 2);
		}
		return target;
	}

	public static void assertConfig(String json, ObjectMapper mapper) {
		if (json == null || json.isBlank()) {
			return;
		}
		Object parsed;
		try {
			parsed = mapper.readValue(json, Object.class);
		}
		catch (RuntimeException ex) {
			throw new ApiException(400, "COMMON.VALIDATION_FAILED", "연결 설정은 JSON 객체여야 합니다.", 2);
		}
		if (!(parsed instanceof Map<?, ?> map)) {
			throw new ApiException(400, "COMMON.VALIDATION_FAILED", "연결 설정은 JSON 객체여야 합니다.", 2);
		}
		for (Object name : map.keySet()) {
			if (sensitive(String.valueOf(name))) {
				throw new ApiException(400, "FILE.SECRET_IN_CONFIG", "연결 설정에 비밀 값을 넣을 수 없습니다.", 2);
			}
		}
	}

	public static boolean sensitive(String name) {
		String lower = name == null ? "" : name.toLowerCase(Locale.ROOT);
		for (String word : SECRET_WORDS) {
			if (lower.contains(word)) {
				return true;
			}
		}
		return false;
	}

	public static void assertSecretRef(String secretRef) {
		if (secretRef == null || secretRef.isBlank()) {
			return;
		}
		if (!SECRET_REF.matcher(secretRef).matches()) {
			throw new ApiException(400, "COMMON.VALIDATION_FAILED", "비밀 참조 형식이 올바르지 않습니다.", 2);
		}
	}

	public static void assertCode(String code, String field) {
		if (code == null || !CODE.matcher(code).matches()) {
			throw new ApiException(400, "COMMON.VALIDATION_FAILED", "코드 형식이 올바르지 않습니다.", 2);
		}
	}

	public static String disposition(String filename) {
		String ascii = filename.chars()
				.map(character -> character >= 0x20 && character < 0x7f && character != '"' && character != '\\' && character != ';'
						? character : '_')
				.collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
				.toString();
		if (ascii.isBlank()) {
			ascii = "file";
		}
		String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
		return "attachment; filename=\"" + ascii + "\"; filename*=UTF-8''" + encoded;
	}

	public static void assertSize(long size, Integer storageMaxMb, int appMaxMb) {
		long storageLimit = storageMaxMb == null ? Long.MAX_VALUE : storageMaxMb.longValue() * 1024L * 1024L;
		long appLimit = appMaxMb * 1024L * 1024L;
		if (size > Math.min(storageLimit, appLimit)) {
			throw new ApiException(413, "FILE.TOO_LARGE", "파일이 너무 큽니다.", 2);
		}
	}

	private static boolean requiresSignature(String extension) {
		return extension != null && (IMAGES.contains(extension) || "pdf".equals(extension) || ZIP_FAMILY.contains(extension));
	}

	private static boolean containsNul(byte[] data) {
		if (data == null) {
			return false;
		}
		int limit = Math.min(data.length, 8192);
		for (int index = 0; index < limit; index++) {
			if (data[index] == 0) {
				return true;
			}
		}
		return false;
	}

	private static void rejectMime() {
		throw new ApiException(400, "FILE.MIME_MISMATCH", "파일 내용과 확장자가 일치하지 않습니다.", 2);
	}

	private static boolean compatible(String extension, String sniffed) {
		if (extension == null) {
			return false;
		}
		return switch (sniffed) {
			case "image/png" -> "png".equals(extension);
			case "image/jpeg" -> "jpg".equals(extension) || "jpeg".equals(extension);
			case "image/gif" -> "gif".equals(extension);
			case "image/webp" -> "webp".equals(extension);
			case "application/pdf" -> "pdf".equals(extension);
			case "application/zip" -> ZIP_FAMILY.contains(extension);
			default -> false;
		};
	}

	private static boolean startsWith(byte[] data, byte[] prefix) {
		if (data.length < prefix.length) {
			return false;
		}
		for (int index = 0; index < prefix.length; index++) {
			if (data[index] != prefix[index]) {
				return false;
			}
		}
		return true;
	}

	private static void rejectExtension() {
		throw new ApiException(400, "FILE.EXTENSION_NOT_ALLOWED", "허용되지 않는 확장자입니다.", 2);
	}

}
