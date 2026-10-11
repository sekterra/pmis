package com.tommy.common.tenant.provision;

import org.springframework.stereotype.Component;

@Component
public class SeededProvisioningStep implements ProvisioningStep {

	private final TenantSeedRunner seedRunner;

	public SeededProvisioningStep(TenantSeedRunner seedRunner) {
		this.seedRunner = seedRunner;
	}

	@Override
	public ProvisioningStepCode code() {
		return ProvisioningStepCode.SEEDED;
	}

	@Override
	public void execute(ProvisioningWork work) {
		seedRunner.run(work.schemaName());
	}

}
