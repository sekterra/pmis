package com.tommy.common.tenant.provision;

import org.springframework.stereotype.Component;

@Component
public class MigratedProvisioningStep implements ProvisioningStep {

	private final TenantSchemaMigrator migrator;
	private final ProvisioningJournal journal;

	public MigratedProvisioningStep(TenantSchemaMigrator migrator, ProvisioningJournal journal) {
		this.migrator = migrator;
		this.journal = journal;
	}

	@Override
	public ProvisioningStepCode code() {
		return ProvisioningStepCode.MIGRATED;
	}

	@Override
	public void execute(ProvisioningWork work) {
		String version = migrator.migrate(work.schemaName());
		journal.updateSchemaVersion(work.tenantPk(), version, work.command().executedByUserPk());
		journal.insertMigration(work.tenantPk(), "PROVISION", version, "SUCCESS", null, work.command().executedByUserPk());
	}

}
