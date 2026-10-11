package com.tommy.common.tenant.provision;

public final class ReissuedPassword {

	private final String tenantCode;
	private final TemporaryPassword temporaryPassword;

	public ReissuedPassword(String tenantCode, TemporaryPassword temporaryPassword) {
		this.tenantCode = tenantCode;
		this.temporaryPassword = temporaryPassword;
	}

	public String tenantCode() {
		return tenantCode;
	}

	public TemporaryPassword temporaryPassword() {
		return temporaryPassword;
	}

	@Override
	public String toString() {
		return "ReissuedPassword[tenantCode=" + tenantCode + ", temporaryPassword=masked]";
	}

}
