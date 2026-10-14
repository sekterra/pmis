package com.tommy.common.tenant.provision;

public final class ProvisioningResult {

	private final String tenantCode;
	private final String schemaName;
	private final String tenantStatus;
	private final ProvisioningStepCode currentStep;
	private final String executionStatus;
	private final TemporaryPassword temporaryPassword;
	private final boolean adminAlreadyPresent;
	private final String adminLoginId;

	public ProvisioningResult(String tenantCode, String schemaName, String tenantStatus,
			ProvisioningStepCode currentStep, String executionStatus, TemporaryPassword temporaryPassword,
			boolean adminAlreadyPresent, String adminLoginId) {
		this.tenantCode = tenantCode;
		this.schemaName = schemaName;
		this.tenantStatus = tenantStatus;
		this.currentStep = currentStep;
		this.executionStatus = executionStatus;
		this.temporaryPassword = temporaryPassword;
		this.adminAlreadyPresent = adminAlreadyPresent;
		this.adminLoginId = adminLoginId;
	}

	public String tenantCode() {
		return tenantCode;
	}

	public String schemaName() {
		return schemaName;
	}

	public String tenantStatus() {
		return tenantStatus;
	}

	public ProvisioningStepCode currentStep() {
		return currentStep;
	}

	public String executionStatus() {
		return executionStatus;
	}

	public TemporaryPassword temporaryPassword() {
		return temporaryPassword;
	}

	public boolean adminAlreadyPresent() {
		return adminAlreadyPresent;
	}

	public String adminLoginId() {
		return adminLoginId;
	}

	@Override
	public String toString() {
		return "ProvisioningResult[tenantCode=" + tenantCode + ", schemaName=" + schemaName
				+ ", tenantStatus=" + tenantStatus + ", currentStep=" + currentStep
				+ ", executionStatus=" + executionStatus + ", temporaryPassword=masked, adminAlreadyPresent="
				+ adminAlreadyPresent + "]";
	}

}
