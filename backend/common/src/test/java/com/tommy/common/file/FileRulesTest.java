package com.tommy.common.file;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.tommy.common.global.exception.ApiException;

import tools.jackson.databind.ObjectMapper;

class FileRulesTest {

	private final ObjectMapper mapper = new ObjectMapper();

	@Test
	void rejectsDangerousAndDoubleExtensions() {
		assertThrows(ApiException.class, () -> FileRules.assertExtensionAllowed("run.exe", "png,exe"));
		assertThrows(ApiException.class, () -> FileRules.assertExtensionAllowed("a.exe.png", "png"));
		FileRules.assertExtensionAllowed("a.php.png", "png");
		assertEquals("png", FileRules.extensionOf("a.php.png"));
	}

	@Test
	void checksMagicBytesAndSanitizesNames() {
		byte[] png = new byte[] { (byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a };
		FileRules.assertMime("png", png);
		assertEquals("image/png", FileRules.mimeType("png", png));
		assertEquals("FILE.MIME_MISMATCH", assertThrows(ApiException.class, () -> FileRules.assertMime("txt", png)).errorCode());
		assertEquals("FILE.MIME_MISMATCH", assertThrows(ApiException.class, () -> FileRules.assertMime("png", "hello".getBytes())).errorCode());
		assertEquals("FILE.MIME_MISMATCH", assertThrows(ApiException.class, () -> FileRules.mimeType("pdf", png)).errorCode());
		assertEquals("FILE.MIME_MISMATCH", assertThrows(ApiException.class, () -> FileRules.assertMime("docx", "hello".getBytes())).errorCode());
		byte[] zip = new byte[] { 'P', 'K', 3, 4 };
		FileRules.assertMime("docx", zip);
		assertEquals("application/zip", FileRules.mimeType("xlsx", zip));
		FileRules.assertMime("txt", "hello".getBytes());
		FileRules.assertMime("csv", "a,b".getBytes());
		assertEquals("text/plain", FileRules.mimeType("txt", "hello".getBytes()));
		assertEquals("FILE.MIME_MISMATCH",
				assertThrows(ApiException.class, () -> FileRules.assertMime("txt", new byte[] { 'a', 0, 'b' })).errorCode());
		byte[] lateNul = new byte[8193];
		java.util.Arrays.fill(lateNul, (byte) 'a');
		lateNul[8192] = 0;
		FileRules.assertMime("txt", lateNul);
		lateNul[8191] = 0;
		assertEquals("FILE.MIME_MISMATCH", assertThrows(ApiException.class, () -> FileRules.assertMime("csv", lateNul)).errorCode());
		assertEquals("note.txt", FileRules.sanitizeName("..\\dir\\note.txt"));
		assertFalse(FileRules.sanitizeName("a\u0001b.txt").contains("\u0001"));
		String longName = "가".repeat(400) + ".txt";
		assertTrue(FileRules.sanitizeName(longName).length() <= 300);
		assertTrue(FileRules.sanitizeName(longName).endsWith(".txt"));
		assertFalse(FileRules.storageKey(java.time.LocalDateTime.of(2026, 10, 19, 1, 2)).contains("note"));
	}

	@Test
	void rejectsPathEscapeAndSecretConfig() {
		assertEquals("FILE.INVALID_PATH", assertThrows(ApiException.class, () -> FileRules.assertBasePath("../secret")).errorCode());
		assertEquals("FILE.SECRET_IN_CONFIG",
				assertThrows(ApiException.class, () -> FileRules.assertConfig("{\"apiPassword\":\"x\"}", mapper)).errorCode());
		FileRules.assertConfig("{\"region\":\"local\"}", mapper);
		assertEquals("COMMON.VALIDATION_FAILED",
				assertThrows(ApiException.class, () -> FileRules.assertConfig("[1]", mapper)).errorCode());
		String header = FileRules.disposition("보고서 \"a\".png");
		assertTrue(header.startsWith("attachment;"));
		assertTrue(header.contains("filename*=UTF-8''"));
		assertFalse(header.contains("\n"));
	}

	@Test
	void storesAtomicallyAndRejectsEscape(@TempDir Path root) throws Exception {
		LocalFileStore store = new LocalFileStore();
		Path target = root.resolve("tenant").resolve("2026").resolve("10").resolve("id");
		store.write(target, new byte[] { 1, 2, 3 });
		assertTrue(store.exists(target));
		assertFalse(Files.exists(target.resolveSibling("id.uploading")));
		Path escaped = FileRules.resolveFile(root, "tenant", "2026/10/id");
		assertTrue(escaped.startsWith(root.resolve("tenant")));
		assertEquals("FILE.INVALID_PATH", assertThrows(ApiException.class, () -> FileRules.resolveFile(root, "tenant", "../other")).errorCode());
		store.write(target, new byte[] { 9 });
		Path blocked = root.resolve("blocked");
		Files.createDirectories(blocked);
		Files.write(blocked.resolve("child"), new byte[] { 1 });
		assertFalse(store.delete(blocked));
		assertTrue(store.delete(target));
	}

}
