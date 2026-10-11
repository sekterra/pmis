package com.tommy.common.global.security;

import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.tommy.common.global.exception.ApiException;

import jakarta.persistence.EntityManager;

@Component
public class AccountStatusCache {

	private static final long TTL_MILLIS = 60_000L;

	private final EntityManager entityManager;

	private final ConcurrentHashMap<String, Entry> cache = new ConcurrentHashMap<>();

	public AccountStatusCache(EntityManager entityManager) {
		this.entityManager = entityManager;
	}

	public void requireActive(String schema, long userPk) {
		if (!active(schema, userPk)) {
			throw new ApiException(401, "AUTH.ACCOUNT_DISABLED", "사용할 수 없는 계정입니다.", 2);
		}
	}

	public void evict(String schema, long userPk) {
		if (schema != null) {
			cache.remove(schema + "\n" + userPk);
		}
	}

	private boolean active(String schema, long userPk) {
		String key = (schema == null ? "" : schema) + "\n" + userPk;
		long now = System.currentTimeMillis();
		Entry current = cache.get(key);
		if (current != null && current.expiresAt() > now) {
			return current.active();
		}
		boolean value = load(userPk);
		cache.put(key, new Entry(value, now + TTL_MILLIS));
		return value;
	}

	private boolean load(long userPk) {
		try {
			Object[] row = (Object[]) entityManager.createNativeQuery(
					"SELECT user_status_code, use_yn FROM user_account WHERE user_pk = ?")
					.setParameter(1, userPk)
					.getSingleResult();
			return row != null && "ACTIVE".equals(row[0]) && "Y".equals(String.valueOf(row[1]).trim());
		}
		catch (RuntimeException ex) {
			return false;
		}
	}

	private record Entry(boolean active, long expiresAt) {
	}

}
