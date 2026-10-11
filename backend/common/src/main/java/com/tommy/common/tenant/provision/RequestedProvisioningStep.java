package com.tommy.common.tenant.provision;

import org.springframework.stereotype.Component;

@Component
public class RequestedProvisioningStep implements ProvisioningStep {

	private final ProvisioningJournal journal;

	public RequestedProvisioningStep(ProvisioningJournal journal) {
		this.journal = journal;
	}

	@Override
	public ProvisioningStepCode code() {
		return ProvisioningStepCode.REQUESTED;
	}

	@Override
	public void execute(ProvisioningWork work) {
		if (work.tenantPk() == 0) {
			work.setTenantPk(journal.insertTenant(work));
		}
	}

}
