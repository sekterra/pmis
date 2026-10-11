package com.tommy.common.tenant.provision;

public interface ProvisioningStep {

	ProvisioningStepCode code();

	void execute(ProvisioningWork work);

}
