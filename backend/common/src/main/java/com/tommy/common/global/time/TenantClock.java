package com.tommy.common.global.time;

import java.time.LocalDate;
import java.time.ZoneId;

import org.springframework.stereotype.Service;

import com.tommy.common.global.multitenancy.TenantContext;

import jakarta.persistence.EntityManager;

@Service
public class TenantClock {

	private final EntityManager entityManager;

	public TenantClock(EntityManager entityManager) {
		this.entityManager = entityManager;
	}

	public LocalDate today() {
		return LocalDate.now(zone());
	}

	private ZoneId zone() {
		try {
			String schema = TenantContext.schema();
			if (schema == null || schema.isBlank()) {
				return ZoneId.systemDefault();
			}
			Object value = entityManager.createNativeQuery(
					"SELECT timezone_id FROM pmis.tenant WHERE schema_name = ?")
					.setParameter(1, schema)
					.getSingleResult();
			if (value == null || value.toString().isBlank()) {
				return ZoneId.systemDefault();
			}
			return ZoneId.of(value.toString().trim());
		}
		catch (RuntimeException ex) {
			return ZoneId.systemDefault();
		}
	}

}
