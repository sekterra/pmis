package com.tommy.common.global.security;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import com.tommy.common.global.exception.ApiException;
import com.tommy.common.global.multitenancy.TenantContext;
import com.tommy.common.global.time.TenantClock;

import jakarta.persistence.EntityManager;

@Component
public class DbPermissionChecker implements PermissionChecker {

	private static final long TTL_MILLIS = 60_000L;

	private static final List<String> ACTION_ORDER = List.of("READ", "CREATE", "UPDATE", "DELETE", "EXPORT");

	private final EntityManager entityManager;

	private final TenantClock clock;

	private final MdmSecurityProperties properties;

	private final ConcurrentHashMap<String, Cached> cache = new ConcurrentHashMap<>();

	public DbPermissionChecker(EntityManager entityManager, TenantClock clock, MdmSecurityProperties properties) {
		this.entityManager = entityManager;
		this.clock = clock;
		this.properties = properties;
	}

	@Override
	public void check(String menuCode, String actionCode) {
		if (properties.isPermitAll()) {
			return;
		}
		long userPk = currentUserPk();
		String schema = TenantContext.schema() == null ? "" : TenantContext.schema();
		Set<String> codes = codes(schema, userPk);
		String required = PermissionChecker.permissionCode(menuCode, actionCode);
		boolean allowed = codes.contains(required)
				&& ("READ".equals(actionCode) || codes.contains(PermissionChecker.permissionCode(menuCode, "READ")));
		if (!allowed) {
			throw new ApiException(403, "AUTH.FORBIDDEN", "이 기능을 사용할 권한이 없습니다.", 2);
		}
	}

	public Map<String, List<String>> actionsByMenu() {
		long userPk = currentUserPk();
		String schema = TenantContext.schema() == null ? "" : TenantContext.schema();
		Map<String, Set<String>> grouped = new HashMap<>();
		for (String code : codes(schema, userPk)) {
			int split = code.lastIndexOf(':');
			if (split <= 0 || split == code.length() - 1) {
				continue;
			}
			grouped.computeIfAbsent(code.substring(0, split), key -> new HashSet<>()).add(code.substring(split + 1));
		}
		Map<String, List<String>> result = new LinkedHashMap<>();
		for (Map.Entry<String, Set<String>> entry : grouped.entrySet()) {
			if (!entry.getValue().contains("READ")) {
				continue;
			}
			List<String> actions = new ArrayList<>(entry.getValue());
			actions.sort(Comparator.comparingInt((String action) -> {
				int index = ACTION_ORDER.indexOf(action);
				return index < 0 ? ACTION_ORDER.size() : index;
			}).thenComparing(action -> action));
			result.put(entry.getKey(), List.copyOf(actions));
		}
		return Map.copyOf(result);
	}

	public void evict(String schema, long userPk) {
		cache.remove((schema == null ? "" : schema) + "\n" + userPk);
	}

	private Set<String> codes(String schema, long userPk) {
		String key = schema + "\n" + userPk;
		long now = System.currentTimeMillis();
		Cached current = cache.get(key);
		if (current != null && current.expiresAt() > now) {
			return current.codes();
		}
		Set<String> loaded = load(userPk);
		cache.put(key, new Cached(loaded, now + TTL_MILLIS));
		return loaded;
	}

	private Set<String> load(long userPk) {
		LocalDate today = clock.today();
		@SuppressWarnings("unchecked")
		List<Object[]> rows = entityManager.createNativeQuery("""
				SELECT m.menu_code, p.action_code
				FROM user_role ur
				JOIN role r ON r.role_pk = ur.role_pk AND r.use_yn = 'Y'
				JOIN role_permission rp ON rp.role_pk = r.role_pk AND rp.use_yn = 'Y'
				JOIN permission p ON p.permission_pk = rp.permission_pk AND p.use_yn = 'Y'
				JOIN menu m ON m.menu_pk = p.menu_pk AND m.use_yn = 'Y'
				WHERE ur.user_pk = ?
				  AND ur.use_yn = 'Y'
				  AND (ur.valid_from_date IS NULL OR ur.valid_from_date <= ?)
				  AND (ur.valid_to_date IS NULL OR ur.valid_to_date >= ?)
				""")
				.setParameter(1, userPk)
				.setParameter(2, today)
				.setParameter(3, today)
				.getResultList();
		Set<String> codes = new HashSet<>();
		for (Object[] row : rows) {
			codes.add(String.valueOf(row[0]).trim() + ":" + String.valueOf(row[1]).trim());
		}
		return Set.copyOf(codes);
	}

	private static long currentUserPk() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)
				|| !JwtTokenService.TENANT_TYPE.equals(jwt.getClaimAsString("typ"))) {
			throw new ApiException(401, "AUTH.UNAUTHORIZED", "인증이 필요합니다.", 2);
		}
		try {
			return Long.parseLong(jwt.getSubject());
		}
		catch (NumberFormatException ex) {
			throw new ApiException(401, "AUTH.TOKEN_REJECTED", "토큰이 유효하지 않습니다.", 2);
		}
	}

	private record Cached(Set<String> codes, long expiresAt) {
	}

}
