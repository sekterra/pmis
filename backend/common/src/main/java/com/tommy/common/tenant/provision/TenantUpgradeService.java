package com.tommy.common.tenant.provision;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.tommy.common.global.exception.ApiException;
import com.tommy.common.global.multitenancy.SchemaNames;
import com.tommy.common.global.multitenancy.TenantDirectory;

@Service
public class TenantUpgradeService {

	private final PgTenantAdvisoryLock advisoryLock;
	private final ProvisioningJournal journal;
	private final TenantSchemaMigrator migrator;
	private final TenantSeedRunner seedRunner;
	private final TenantDirectory tenantDirectory;

	public TenantUpgradeService(PgTenantAdvisoryLock advisoryLock, ProvisioningJournal journal,
			TenantSchemaMigrator migrator, TenantSeedRunner seedRunner, TenantDirectory tenantDirectory) {
		this.advisoryLock = advisoryLock;
		this.journal = journal;
		this.migrator = migrator;
		this.seedRunner = seedRunner;
		this.tenantDirectory = tenantDirectory;
	}

	public UpgradeTenantResult upgradeTenant(String tenantCode, Long executedByUserPk) {
		try {
			SchemaNames.requireTenantCode(tenantCode);
		}
		catch (IllegalArgumentException ex) {
			throw new ApiException(400, "COMMON.VALIDATION_FAILED", ex.getMessage(), 2);
		}
		ProvisioningJournal.TenantState tenant = journal.findTenant(tenantCode)
				.orElseThrow(() -> new ApiException(404, "TENANT.NOT_FOUND", "테넌트를 찾을 수 없습니다.", 2));
		if (!"ACTIVE".equals(tenant.statusCode())) {
			throw new ApiException(409, "TENANT.INACTIVE", "사용 중인 테넌트만 업그레이드할 수 있습니다.", 2);
		}
		UpgradeTenantResult result = upgradeOne(tenant, executedByUserPk);
		return new UpgradeTenantResult(tenantCode, result.schemaName(), result.executionStatus(), result.errorMessage());
	}

	public UpgradeBatchResult upgradeAll(Long executedByUserPk) {
		List<UpgradeTenantResult> results = new ArrayList<>();
		List<String> skipped = new ArrayList<>();
		advisoryLock.runWithUpgradeBatchLock(() -> {
			List<ProvisioningJournal.TenantState> targets = new ArrayList<>();
			for (ProvisioningJournal.TenantState tenant : journal.listTenants()) {
				if ("ACTIVE".equals(tenant.statusCode())) {
					targets.add(tenant);
				}
				else {
					skipped.add(tenant.tenantCode());
				}
			}
			results.addAll(upgradeLocked(targets, executedByUserPk));
		});
		return new UpgradeBatchResult(results, skipped);
	}

	public UpgradeBatchResult upgradeTenants(List<String> tenantCodes, Long executedByUserPk) {
		List<ProvisioningJournal.TenantState> targets = new ArrayList<>();
		for (String tenantCode : tenantCodes) {
			targets.add(journal.findTenant(tenantCode)
					.orElseThrow(() -> new ApiException(404, "TENANT.NOT_FOUND", "테넌트를 찾을 수 없습니다.", 2)));
		}
		List<UpgradeTenantResult> results = new ArrayList<>();
		advisoryLock.runWithUpgradeBatchLock(() -> results.addAll(upgradeLocked(targets, executedByUserPk)));
		return new UpgradeBatchResult(results, List.of());
	}

	private List<UpgradeTenantResult> upgradeLocked(List<ProvisioningJournal.TenantState> tenants, Long executedByUserPk) {
		List<UpgradeTenantResult> results = new ArrayList<>();
		for (ProvisioningJournal.TenantState tenant : tenants) {
			results.add(upgradeOne(tenant, executedByUserPk));
		}
		return results;
	}

	private UpgradeTenantResult upgradeOne(ProvisioningJournal.TenantState tenant, Long executedByUserPk) {
		try {
			SchemaNames.requireTenantSchema(tenant.schemaName());
		}
		catch (IllegalArgumentException ex) {
			return failed(tenant, executedByUserPk, ErrorMessages.mask(ex));
		}
		try {
			advisoryLock.runWithTenantLock(tenant.schemaName(), () -> migrate(tenant, executedByUserPk));
			tenantDirectory.evict(tenant.tenantCode());
			return new UpgradeTenantResult(tenant.tenantCode(), tenant.schemaName(), "SUCCESS", null);
		}
		catch (RuntimeException ex) {
			String message = ErrorMessages.mask(ex);
			if (ex instanceof ApiException apiException
					&& "TENANT.PROVISIONING_IN_PROGRESS".equals(apiException.errorCode())) {
				recordFailure(tenant, executedByUserPk, message);
			}
			tenantDirectory.evict(tenant.tenantCode());
			return new UpgradeTenantResult(tenant.tenantCode(), tenant.schemaName(), "FAILED", message);
		}
	}

	private void migrate(ProvisioningJournal.TenantState tenant, Long executedByUserPk) {
		long migrationPk = journal.beginMigration(tenant.tenantPk(), "UPGRADE", executedByUserPk);
		try {
			String version = migrator.migrate(tenant.schemaName());
			journal.updateSchemaVersion(tenant.tenantPk(), version, executedByUserPk);
			seedRunner.run(tenant.schemaName());
			journal.finishMigration(migrationPk, version, "SUCCESS", null);
		}
		catch (RuntimeException ex) {
			journal.finishMigration(migrationPk, "pending", "FAILED", ErrorMessages.mask(ex));
			throw ex;
		}
	}

	private UpgradeTenantResult failed(ProvisioningJournal.TenantState tenant, Long executedByUserPk, String message) {
		recordFailure(tenant, executedByUserPk, message);
		return new UpgradeTenantResult(tenant.tenantCode(), tenant.schemaName(), "FAILED", message);
	}

	private void recordFailure(ProvisioningJournal.TenantState tenant, Long executedByUserPk, String message) {
		journal.insertMigration(tenant.tenantPk(), "UPGRADE", "pending", "FAILED", message, executedByUserPk);
	}

	public record UpgradeTenantResult(String tenantCode, String schemaName, String executionStatus, String errorMessage) {
	}

	public record UpgradeBatchResult(List<UpgradeTenantResult> results, List<String> skippedTenantCodes) {
	}

}
