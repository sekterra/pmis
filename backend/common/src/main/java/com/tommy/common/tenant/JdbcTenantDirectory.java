package com.tommy.common.tenant;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.tommy.common.global.multitenancy.TenantDirectory;
import com.tommy.common.global.multitenancy.TenantSnapshot;

@Component
public class JdbcTenantDirectory implements TenantDirectory {

	private static final Duration TTL = Duration.ofSeconds(30);

	private final JdbcTemplate jdbcTemplate;
	private final ConcurrentHashMap<String, Cached> byCode = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<String, Cached> byDomain = new ConcurrentHashMap<>();

	public JdbcTenantDirectory(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public Optional<TenantSnapshot> findByTenantCode(String tenantCode) {
		if (tenantCode == null || tenantCode.isBlank()) {
			return Optional.empty();
		}
		Cached cached = byCode.get(tenantCode);
		if (cached != null && cached.fresh()) {
			return Optional.of(cached.snapshot());
		}
		Optional<TenantSnapshot> loaded = jdbcTemplate.query("""
				SELECT tenant_code, schema_name, tenant_status_code, use_yn
				FROM pmis.tenant
				WHERE tenant_code = ?
				""", (resultSet, row) -> new TenantSnapshot(
				resultSet.getString("tenant_code"),
				resultSet.getString("schema_name"),
				resultSet.getString("tenant_status_code"),
				"Y".equals(resultSet.getString("use_yn"))), tenantCode).stream().findFirst();
		loaded.ifPresent(snapshot -> byCode.put(tenantCode, new Cached(snapshot)));
		return loaded;
	}

	@Override
	public Optional<TenantSnapshot> findByDomain(String domainName) {
		if (domainName == null || domainName.isBlank()) {
			return Optional.empty();
		}
		Cached cached = byDomain.get(domainName);
		if (cached != null && cached.fresh()) {
			return Optional.of(cached.snapshot());
		}
		Optional<TenantSnapshot> loaded = jdbcTemplate.query("""
				SELECT t.tenant_code, t.schema_name, t.tenant_status_code, t.use_yn
				FROM pmis.tenant t
				JOIN pmis.tenant_domain d ON d.tenant_pk = t.tenant_pk
				WHERE d.domain_name = ? AND d.use_yn = 'Y'
				""", (resultSet, row) -> new TenantSnapshot(
				resultSet.getString("tenant_code"),
				resultSet.getString("schema_name"),
				resultSet.getString("tenant_status_code"),
				"Y".equals(resultSet.getString("use_yn"))), domainName).stream().findFirst();
		loaded.ifPresent(snapshot -> {
			byDomain.put(domainName, new Cached(snapshot));
			byCode.put(snapshot.tenantCode(), new Cached(snapshot));
		});
		return loaded;
	}

	@Override
	public void evict(String tenantCode) {
		if (tenantCode == null) {
			return;
		}
		byCode.remove(tenantCode);
		byDomain.entrySet().removeIf(entry -> tenantCode.equals(entry.getValue().snapshot().tenantCode()));
	}

	private record Cached(TenantSnapshot snapshot, Instant storedAt) {
		private Cached(TenantSnapshot snapshot) {
			this(snapshot, Instant.now());
		}

		private boolean fresh() {
			return storedAt.plus(TTL).isAfter(Instant.now());
		}
	}

}
