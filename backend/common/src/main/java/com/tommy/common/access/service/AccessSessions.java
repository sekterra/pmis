package com.tommy.common.access.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Component;

import com.tommy.common.code.CodeRules;
import com.tommy.common.global.exception.ApiException;
import com.tommy.common.global.multitenancy.TenantContext;
import com.tommy.common.global.security.AccountStatusCache;
import com.tommy.common.global.security.DbPermissionChecker;
import com.tommy.common.global.security.PasswordHasher;
import com.tommy.common.tenant.provision.TemporaryPassword;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;

@Component
public class AccessSessions {

	private final EntityManager entityManager;

	private final PasswordHasher passwords;

	private final DbPermissionChecker permissions;

	private final AccountStatusCache accounts;

	public AccessSessions(EntityManager entityManager, PasswordHasher passwords, DbPermissionChecker permissions,
			AccountStatusCache accounts) {
		this.entityManager = entityManager;
		this.passwords = passwords;
		this.permissions = permissions;
		this.accounts = accounts;
	}

	public TemporaryPassword issue() {
		return TemporaryPassword.generate();
	}

	public String hash(String raw) {
		return passwords.hash(raw);
	}

	public LocalDateTime expireAt() {
		Object value;
		try {
			value = entityManager.createNativeQuery("""
					SELECT c.ext_attr1
					FROM common_code c
					JOIN code_group g ON g.code_group_pk = c.code_group_pk
					WHERE g.code_group_code = ? AND c.code_value = ? AND g.use_yn = 'Y' AND c.use_yn = 'Y'
					""").setParameter(1, "SECURITY_POLICY")
					.setParameter(2, "TEMP_PASSWORD_VALID_DAYS")
					.getSingleResult();
		}
		catch (NoResultException ex) {
			throw new ApiException(500, "TENANT.SECURITY_POLICY_MISSING", "보안 정책이 없습니다.", 2);
		}
		String raw = value == null ? null : value.toString();
		if (!CodeRules.validPolicy("TEMP_PASSWORD_VALID_DAYS", raw)) {
			throw new ApiException(500, "TENANT.SECURITY_POLICY_MISSING", "보안 정책이 없습니다.", 2);
		}
		return LocalDateTime.now().plusDays(Integer.parseInt(raw.trim()));
	}

	public void revokeRefresh(long userPk) {
		entityManager.createNativeQuery("""
				UPDATE user_refresh_token
				SET revoked_datetime = CURRENT_TIMESTAMP, revoke_reason_code = 'ADMIN'
				WHERE user_pk = ? AND revoked_datetime IS NULL
				""").setParameter(1, userPk).executeUpdate();
	}

	public void evictUser(long userPk) {
		String schema = TenantContext.schema();
		permissions.evict(schema, userPk);
		accounts.evict(schema, userPk);
	}

	public void evictUsers(List<Long> userPks) {
		String schema = TenantContext.schema();
		permissions.evictUsers(schema, userPks);
		if (userPks != null) {
			for (Long userPk : userPks) {
				if (userPk != null) {
					accounts.evict(schema, userPk);
				}
			}
		}
	}

	public void evictAll() {
		permissions.evictAll(TenantContext.schema());
	}

}
