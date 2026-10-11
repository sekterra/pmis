package com.tommy.common.code.service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.tommy.common.global.exception.ApiException;

import org.springframework.stereotype.Service;

import com.tommy.common.global.multitenancy.TenantContext;

import jakarta.persistence.EntityManager;

@Service
public class CodeLanguageResolver {

	private final EntityManager entityManager;
	private final Map<String, Map<String, String>> fallbackCache = new ConcurrentHashMap<>();

	public CodeLanguageResolver(EntityManager entityManager) {
		this.entityManager = entityManager;
	}

	public String choose(String lang, String acceptLanguage) {
		if (lang != null && !lang.isBlank()) {
			return lang.trim();
		}
		if (acceptLanguage != null && !acceptLanguage.isBlank()) {
			String first = acceptLanguage.split(",")[0].trim();
			int quality = first.indexOf(';');
			return (quality >= 0 ? first.substring(0, quality) : first).trim();
		}
		return tenantDefaultLanguageValue();
	}

	public String name(String requestedLanguage, Map<String, String> namesByLanguage, String codeName) {
		Set<String> seen = new HashSet<>();
		String current = requestedLanguage;
		Map<String, String> fallbacks = fallbacks();
		while (current != null && !current.isBlank() && seen.add(current)) {
			String translated = namesByLanguage.get(current);
			if (translated != null && !translated.isBlank()) {
				return translated;
			}
			current = fallbacks.get(current);
		}
		return codeName;
	}

	public void requireSupported(String supportLanguageCode) {
		Number count = (Number) entityManager.createNativeQuery(
				"SELECT count(*) FROM support_language WHERE support_language_code = ?")
				.setParameter(1, supportLanguageCode)
				.getSingleResult();
		if (count.longValue() == 0) {
			throw new ApiException(400, "CODE.LANGUAGE_NOT_FOUND", "지원하지 않는 언어입니다.", 2);
		}
	}

	public Map<String, String> fallbackMap() {
		Map<String, String> copy = new HashMap<>();
		fallbacks().forEach((code, fallback) -> {
			if (fallback != null) {
				copy.put(code, fallback);
			}
		});
		return copy;
	}

	public String tenantDefaultLanguage() {
		return tenantDefaultLanguageValue();
	}

	public void evict() {
		String schema = TenantContext.schema();
		if (schema != null) {
			fallbackCache.remove(schema);
		}
	}

	@SuppressWarnings("unchecked")
	private Map<String, String> fallbacks() {
		String schema = TenantContext.schema() == null ? "" : TenantContext.schema();
		return fallbackCache.computeIfAbsent(schema, key -> {
			List<Object[]> rows = entityManager.createNativeQuery(
					"SELECT support_language_code, fallback_language_code FROM support_language").getResultList();
			Map<String, String> map = new HashMap<>();
			for (Object[] row : rows) {
				map.put((String) row[0], (String) row[1]);
			}
			return map;
		});
	}

	private String tenantDefaultLanguageValue() {
		String schema = TenantContext.schema();
		Object value = entityManager.createNativeQuery(
				"SELECT default_language_code FROM pmis.tenant WHERE schema_name = ?")
				.setParameter(1, schema)
				.getSingleResult();
		return value == null ? null : value.toString();
	}

}
