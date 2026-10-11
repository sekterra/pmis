package com.tommy.common.platform;

import java.util.concurrent.ConcurrentHashMap;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class PlatformAccountStatus {

	private static final long TTL_MILLIS = 60_000L;

	private final JdbcTemplate jdbcTemplate;

	private final ConcurrentHashMap<Long, Entry> cache = new ConcurrentHashMap<>();

	public PlatformAccountStatus(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public boolean active(long adminPk) {
		long now = System.currentTimeMillis();
		Entry current = cache.get(adminPk);
		if (current != null && current.expiresAt() > now) {
			return current.active();
		}
		boolean value = load(adminPk);
		cache.put(adminPk, new Entry(value, now + TTL_MILLIS));
		return value;
	}

	public void evict(long adminPk) {
		cache.remove(adminPk);
	}

	private boolean load(long adminPk) {
		try {
			return Boolean.TRUE.equals(jdbcTemplate.query("""
					SELECT admin_status_code, use_yn
					FROM pmis.platform_admin
					WHERE platform_admin_pk = ?
					""", (resultSet, row) -> "ACTIVE".equals(resultSet.getString("admin_status_code"))
					&& "Y".equals(trim(resultSet.getString("use_yn"))), adminPk).stream().findFirst().orElse(false));
		}
		catch (RuntimeException ex) {
			return false;
		}
	}

	private static String trim(String value) {
		return value == null ? "" : value.trim();
	}

	private record Entry(boolean active, long expiresAt) {
	}

}
