package com.tommy.common.devadmin;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Pattern;

import com.tommy.common.global.multitenancy.SchemaNames;

public final class DevSetAdminRules {

	public static final int BCRYPT_MAX_BYTES = 72;

	private static final Pattern LOGIN = Pattern.compile("^[a-z0-9._@-]{3,100}$");

	private DevSetAdminRules() {
	}

	public static void requireDevProfile(boolean devProfile) {
		if (!devProfile) {
			throw new IllegalArgumentException("개발 프로필이 아닙니다.");
		}
	}

	public static Command parse(String target, String tenantCode, String login, String password) {
		if (blank(target) || blank(login) || password == null || password.isBlank()) {
			throw new IllegalArgumentException("환경변수가 없습니다.");
		}
		if (!"platform".equals(target) && !"tenant".equals(target)) {
			throw new IllegalArgumentException("MDM_DEV_ADMIN_TARGET 이 올바르지 않습니다.");
		}
		if ("tenant".equals(target) && blank(tenantCode)) {
			throw new IllegalArgumentException("MDM_DEV_ADMIN_TENANT_CODE 가 없습니다.");
		}
		String loginId = login.trim().toLowerCase(Locale.ROOT);
		if (!LOGIN.matcher(loginId).matches()) {
			throw new IllegalArgumentException("로그인 아이디 형식이 올바르지 않습니다.");
		}
		if (password.getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAX_BYTES) {
			throw new IllegalArgumentException("비밀번호가 BCrypt 한도를 넘습니다.");
		}
		String code = null;
		if ("tenant".equals(target)) {
			code = tenantCode.trim();
			try {
				SchemaNames.requireTenantCode(code);
			}
			catch (IllegalArgumentException ex) {
				throw new IllegalArgumentException("테넌트 코드 형식이 아닙니다.");
			}
		}
		return new Command(target, code, loginId, password);
	}

	public static String line(String target, String tenantCode, String loginId, boolean created) {
		String action = created ? "created" : "updated";
		if ("tenant".equals(target)) {
			return "tenant " + tenantCode + " " + action + " " + loginId;
		}
		return "platform " + action + " " + loginId;
	}

	private static boolean blank(String value) {
		return value == null || value.isBlank();
	}

	public record Command(String target, String tenantCode, String loginId, String password) {

		@Override
		public String toString() {
			return "Command[target=" + target + ", tenantCode=" + tenantCode + ", loginId=" + loginId + "]";
		}
	}

}
