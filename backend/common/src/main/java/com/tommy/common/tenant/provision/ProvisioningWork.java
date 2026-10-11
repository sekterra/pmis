package com.tommy.common.tenant.provision;

public final class ProvisioningWork {

	private final ProvisionTenantCommand command;
	private final String schemaName;
	private long tenantPk;
	private ProvisioningStepCode lastSuccess;
	private int retryCount;
	private TemporaryPassword temporaryPassword;
	private boolean adminAlreadyPresent;

	public ProvisioningWork(ProvisionTenantCommand command, String schemaName) {
		this.command = command;
		this.schemaName = schemaName;
	}

	public ProvisionTenantCommand command() {
		return command;
	}

	public String schemaName() {
		return schemaName;
	}

	public long tenantPk() {
		return tenantPk;
	}

	public void setTenantPk(long tenantPk) {
		this.tenantPk = tenantPk;
	}

	public ProvisioningStepCode lastSuccess() {
		return lastSuccess;
	}

	public void setLastSuccess(ProvisioningStepCode lastSuccess) {
		this.lastSuccess = lastSuccess;
	}

	public int retryCount() {
		return retryCount;
	}

	public void setRetryCount(int retryCount) {
		this.retryCount = retryCount;
	}

	public TemporaryPassword temporaryPassword() {
		return temporaryPassword;
	}

	public void setTemporaryPassword(TemporaryPassword temporaryPassword) {
		this.temporaryPassword = temporaryPassword;
	}

	public boolean adminAlreadyPresent() {
		return adminAlreadyPresent;
	}

	public void setAdminAlreadyPresent(boolean adminAlreadyPresent) {
		this.adminAlreadyPresent = adminAlreadyPresent;
	}

}
