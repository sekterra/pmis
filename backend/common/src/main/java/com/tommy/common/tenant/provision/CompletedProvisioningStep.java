package com.tommy.common.tenant.provision;

import org.springframework.stereotype.Component;

@Component
public class CompletedProvisioningStep implements ProvisioningStep {

	private final TenantSchemaMigrator migrator;
	private final ProvisioningJournal journal;

	public CompletedProvisioningStep(TenantSchemaMigrator migrator, ProvisioningJournal journal) {
		this.migrator = migrator;
		this.journal = journal;
	}

	@Override
	public ProvisioningStepCode code() {
		return ProvisioningStepCode.COMPLETED;
	}

	@Override
	public void execute(ProvisioningWork work) {
		ProvisioningJournal.TenantState tenant = journal.findTenant(work.command().tenantCode()).orElse(null);
		if (tenant != null && "ACTIVE".equals(tenant.statusCode())) {
			return;
		}
		String version = migrator.migrate(work.schemaName());
		journal.complete(work.tenantPk(), version, work.command().executedByUserPk());
	}

}
