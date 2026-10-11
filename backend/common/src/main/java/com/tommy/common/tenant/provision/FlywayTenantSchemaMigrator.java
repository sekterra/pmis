package com.tommy.common.tenant.provision;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.springframework.stereotype.Component;

import com.tommy.common.global.multitenancy.SchemaNames;

@Component
public class FlywayTenantSchemaMigrator implements TenantSchemaMigrator {

	private final DataSource dataSource;

	public FlywayTenantSchemaMigrator(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	@Override
	public String migrate(String schemaName) {
		SchemaNames.requireTenantSchema(schemaName);
		Flyway flyway = Flyway.configure()
				.dataSource(new SearchPathResetDataSource(dataSource))
				.schemas(schemaName)
				.defaultSchema(schemaName)
				.table("flyway_schema_history")
				.locations("classpath:db/migration/tenant")
				.createSchemas(false)
				.cleanDisabled(true)
				.load();
		flyway.migrate();
		if (flyway.info().current() == null || flyway.info().current().getVersion() == null) {
			throw new IllegalStateException("테넌트 마이그레이션 버전을 확인하지 못했습니다.");
		}
		return flyway.info().current().getVersion().getVersion();
	}

}
