package com.tommy.common.platform;

import java.util.Locale;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tommy.common.global.exception.ApiException;
import com.tommy.common.global.multitenancy.SchemaNames;
import com.tommy.common.global.security.MdmSecurityProperties;
import com.tommy.common.global.security.PasswordHasher;
import com.tommy.common.tenant.provision.TemporaryPassword;

@Service
public class PlatformAdminBootstrapService {

	private final JdbcTemplate jdbcTemplate;

	private final PasswordHasher passwordHasher;

	private final MdmSecurityProperties properties;

	public PlatformAdminBootstrapService(JdbcTemplate jdbcTemplate, PasswordHasher passwordHasher, MdmSecurityProperties properties) {
		this.jdbcTemplate = jdbcTemplate;
		this.passwordHasher = passwordHasher;
		this.properties = properties;
	}

	@Transactional
	public TemporaryPassword createIfEmpty(String loginId, String adminName, String email) {
		String database = jdbcTemplate.queryForObject("SELECT current_database()", String.class);
		if (!"pmis".equals(database)) {
			throw new IllegalStateException("데이터베이스 이름이 pmis 가 아닙니다.");
		}
		Long count = jdbcTemplate.queryForObject("SELECT count(*) FROM pmis.platform_admin", Long.class);
		if (!PlatformBootstrapRules.empty(count == null ? -1 : count)) {
			throw new IllegalStateException("플랫폼 운영자 계정이 이미 있습니다.");
		}
		String id = loginId == null ? "" : loginId.trim().toLowerCase(Locale.ROOT);
		try {
			if (id.isBlank() || id.length() > 100) {
				throw new IllegalArgumentException("로그인 아이디가 없습니다.");
			}
		}
		catch (IllegalArgumentException ex) {
			throw new IllegalStateException(ex.getMessage(), ex);
		}
		String name = blank(adminName) ? id : adminName.trim();
		String mail = blank(email) ? id : email.trim();
		TemporaryPassword password = TemporaryPassword.generate(20);
		int days = properties.getPlatform().getTempPasswordValidDays();
		jdbcTemplate.update("""
				INSERT INTO pmis.platform_admin (
				    login_id, admin_name, email, password_hash, must_change_password_yn, admin_status_code,
				    password_expire_datetime, use_yn)
				VALUES (?, ?, ?, ?, 'Y', 'ACTIVE', CURRENT_TIMESTAMP + (? * INTERVAL '1 day'), 'Y')
				""", id, name, mail, passwordHasher.hash(password.reveal()), days);
		return password;
	}

	private static boolean blank(String value) {
		return value == null || value.isBlank();
	}

}
