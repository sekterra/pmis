package com.tommy.common.menu;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SeedMenuCodes {

	private static final Pattern CODE = Pattern.compile("'([A-Z][A-Z0-9_]*)'");

	private static final Set<String> CODES = load();

	private SeedMenuCodes() {
	}

	public static boolean protects(String menuCode, String solutionCode) {
		if (CODES.isEmpty()) {
			return "MDM".equals(solutionCode);
		}
		return CODES.contains(menuCode);
	}

	static Set<String> codes() {
		return CODES;
	}

	private static Set<String> load() {
		try (InputStream input = SeedMenuCodes.class.getResourceAsStream("/db/seed/tenant/S05_role_menu_permission.sql")) {
			if (input == null) {
				return Set.of();
			}
			String sql = new String(input.readAllBytes(), StandardCharsets.UTF_8);
			int start = sql.indexOf("INSERT INTO menu");
			int end = sql.indexOf("INSERT INTO permission");
			if (start < 0 || end < start) {
				return Set.of();
			}
			Matcher matcher = CODE.matcher(sql.substring(start, end));
			Set<String> codes = new LinkedHashSet<>();
			while (matcher.find()) {
				String code = matcher.group(1);
				if (!"MDM".equals(code) && !"GROUP".equals(code) && !"PAGE".equals(code) && !"Y".equals(code)) {
					codes.add(code);
				}
			}
			return Set.copyOf(codes);
		}
		catch (IOException ex) {
			return Set.of();
		}
	}

}
