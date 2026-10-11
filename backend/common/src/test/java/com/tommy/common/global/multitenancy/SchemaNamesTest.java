package com.tommy.common.global.multitenancy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SchemaNamesTest {

	@Test
	void rejectsReservedAndInvalidNames() {
		assertFalse(SchemaNames.isTenantSchema("pmis"));
		assertFalse(SchemaNames.isTenantSchema("public"));
		assertFalse(SchemaNames.isTenantSchema("pg_catalog"));
		assertFalse(SchemaNames.isTenantSchema("information_schema"));
		assertFalse(SchemaNames.isTenantSchema("t_Bad"));
		assertFalse(SchemaNames.isTenantSchema("tenant"));
		assertFalse(SchemaNames.isTenantSchema(null));
		assertTrue(SchemaNames.isTenantSchema("t_it_abc"));
		assertThrows(IllegalArgumentException.class, () -> SchemaNames.requireTenantSchema("pmis"));
		assertThrows(IllegalArgumentException.class, () -> SchemaNames.requireTenantSchema("public"));
		assertThrows(IllegalArgumentException.class, () -> SchemaNames.requireSearchPath("pg_catalog"));
		assertEquals("t_it_ab12", SchemaNames.schemaNameForTenantCode("it-ab12"));
		assertThrows(IllegalArgumentException.class, () -> SchemaNames.schemaNameForTenantCode("IT-AB"));
		assertThrows(IllegalArgumentException.class, () -> SchemaNames.requireTenantCode("has_underscore"));
	}

}
