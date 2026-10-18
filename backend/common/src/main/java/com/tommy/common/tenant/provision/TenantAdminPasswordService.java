package com.tommy.common.tenant.provision;

import org.springframework.stereotype.Service;

import com.tommy.common.global.exception.ApiException;
import com.tommy.common.global.multitenancy.SchemaNames;

@Service
public class TenantAdminPasswordService {

	private final PgTenantAdvisoryLock advisoryLock;
	private final ProvisioningJournal journal;
	private final TenantAdminWriter adminWriter;

	public TenantAdminPasswordService(PgTenantAdvisoryLock advisoryLock, ProvisioningJournal journal,
			TenantAdminWriter adminWriter) {
		this.advisoryLock = advisoryLock;
		this.journal = journal;
		this.adminWriter = adminWriter;
	}

	public ReissuedPassword reissue(String tenantCode, Long executorPk) {
		try {
			SchemaNames.requireTenantCode(tenantCode);
		}
		catch (IllegalArgumentException ex) {
			throw new ApiException(400, "COMMON.VALIDATION_FAILED", ex.getMessage(), 2);
		}
		ProvisioningJournal.TenantState tenant = journal.findTenant(tenantCode)
				.orElseThrow(() -> new ApiException(404, "TENANT.NOT_FOUND", "테넌트를 찾을 수 없습니다.", 2));
		if (!"ACTIVE".equals(tenant.statusCode())) {
			throw new ApiException(409, "TENANT.INACTIVE", "사용 중인 테넌트만 임시 비밀번호를 다시 발급할 수 있습니다.", 2);
		}
		TenantAdminWriter.IssuedAdminPassword[] created = new TenantAdminWriter.IssuedAdminPassword[1];
		advisoryLock.runWithTenantLock(tenant.schemaName(),
				() -> created[0] = adminWriter.reissue(tenant.tenantPk(), tenant.schemaName(), executorPk));
		return new ReissuedPassword(tenantCode, created[0].temporaryPassword(), created[0].adminLoginId());
	}

	public String storedAdminLoginId(String tenantCode) {
		ProvisioningJournal.TenantState tenant = journal.findTenant(tenantCode)
				.orElseThrow(() -> new ApiException(404, "TENANT.NOT_FOUND", "테넌트를 찾을 수 없습니다.", 2));
		return adminWriter.storedAdminLoginId(tenant.schemaName());
	}

}
