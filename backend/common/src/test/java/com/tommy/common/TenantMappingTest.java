package com.tommy.common;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import com.tommy.common.tenant.entity.Tenant;

import jakarta.persistence.Column;

class TenantMappingTest {

	@Test
	void tenantCodeAndSchemaNameAreNotUpdatable() throws Exception {
		assertFalse(column("tenantCode").updatable());
		assertFalse(column("schemaName").updatable());
		for (Method method : Tenant.class.getDeclaredMethods()) {
			String name = method.getName();
			assertFalse(name.equals("setTenantCode") || name.equals("setSchemaName") || name.equals("changeTenantCode")
					|| name.equals("changeSchemaName"));
		}
		assertTrue(Tenant.class.getMethod("rename", String.class) != null);
	}

	private static Column column(String fieldName) throws Exception {
		return Tenant.class.getDeclaredField(fieldName).getAnnotation(Column.class);
	}

}
