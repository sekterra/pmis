package com.tommy.common.tenant.provision;

public final class ReissuedPassword {

	private final String tenantCode;
	private final TemporaryPassword temporaryPassword;
	private final String adminLoginId;

	public ReissuedPassword(String tenantCode, TemporaryPassword temporaryPassword, String adminLoginId) {
		this.tenantCode = tenantCode;
		this.temporaryPassword = temporaryPassword;
		this.adminLoginId = adminLoginId;
	}

	public String tenantCode() {
		return tenantCode;
	}

	public TemporaryPassword temporaryPassword() {
		return temporaryPassword;
	}

	public String adminLoginId() {
		return adminLoginId;
	}

	@Override
	public String toString() {
		return "ReissuedPassword[tenantCode=" + tenantCode + ", temporaryPassword=masked]";
	}

}
