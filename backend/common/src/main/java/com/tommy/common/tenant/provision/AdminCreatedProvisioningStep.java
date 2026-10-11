package com.tommy.common.tenant.provision;

import org.springframework.stereotype.Component;

@Component
public class AdminCreatedProvisioningStep implements ProvisioningStep {

	private final TenantAdminWriter adminWriter;

	public AdminCreatedProvisioningStep(TenantAdminWriter adminWriter) {
		this.adminWriter = adminWriter;
	}

	@Override
	public ProvisioningStepCode code() {
		return ProvisioningStepCode.ADMIN_CREATED;
	}

	@Override
	public void execute(ProvisioningWork work) {
		adminWriter.createInitialAdmin(work);
	}

}
