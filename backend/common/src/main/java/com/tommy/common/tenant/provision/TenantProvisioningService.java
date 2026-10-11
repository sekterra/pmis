package com.tommy.common.tenant.provision;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.tommy.common.global.exception.ApiException;
import com.tommy.common.global.multitenancy.SchemaNames;
import com.tommy.common.global.multitenancy.TenantDirectory;

@Service
public class TenantProvisioningService {

	private final PgTenantAdvisoryLock advisoryLock;
	private final ProvisioningJournal journal;
	private final TenantDirectory tenantDirectory;
	private final Map<ProvisioningStepCode, ProvisioningStep> steps;

	public TenantProvisioningService(PgTenantAdvisoryLock advisoryLock, ProvisioningJournal journal,
			TenantDirectory tenantDirectory, List<ProvisioningStep> steps) {
		this.advisoryLock = advisoryLock;
		this.journal = journal;
		this.tenantDirectory = tenantDirectory;
		this.steps = new EnumMap<>(ProvisioningStepCode.class);
		for (ProvisioningStep step : steps) {
			this.steps.put(step.code(), step);
		}
	}

	public ProvisioningResult provision(ProvisionTenantCommand command) {
		String schemaName = resolveSchema(command);
		ProvisioningWork[] completed = new ProvisioningWork[1];
		advisoryLock.runWithTenantLock(schemaName, () -> completed[0] = provisionLocked(command, schemaName));
		tenantDirectory.evict(command.tenantCode());
		return readResult(command.tenantCode(), completed[0]);
	}

	private ProvisioningWork provisionLocked(ProvisionTenantCommand command, String schemaName) {
		ProvisioningWork work = new ProvisioningWork(command, schemaName);
		ProvisioningJournal.TenantState existing = journal.findTenant(command.tenantCode()).orElse(null);
		if (existing != null) {
			if (!schemaName.equals(existing.schemaName())) {
				throw new ApiException(409, "TENANT.SCHEMA_MISMATCH", "저장된 스키마명과 다릅니다.", 2);
			}
			rejectClosedStatus(existing.statusCode());
			work.setTenantPk(existing.tenantPk());
			ProvisioningJournal.ProvisioningState provisioning = journal.findProvisioning(existing.tenantPk())
					.orElseThrow(() -> new ApiException(409, "TENANT.PROVISIONING_FAILED", "프로비저닝 기록이 없습니다.", 2));
			work.setLastSuccess(provisioning.currentStep());
			work.setRetryCount(provisioning.retryCount());
			if ("FAILED".equals(existing.statusCode())) {
				journal.resume(work);
			}
		}
		ProvisioningPlanner.Decision decision = ProvisioningPlanner.plan(work.lastSuccess(), steps.keySet());
		try {
			for (ProvisioningStepCode code : decision.toRun()) {
				steps.get(code).execute(work);
				work.setLastSuccess(code);
				if (code != ProvisioningStepCode.COMPLETED) {
					journal.markStep(work, code);
				}
			}
			if (decision.paused()) {
				journal.markPaused(work);
			}
			return work;
		}
		catch (RuntimeException ex) {
			journal.markFailed(work, ErrorMessages.mask(ex));
			tenantDirectory.evict(command.tenantCode());
			if (ex instanceof ApiException apiException) {
				throw apiException;
			}
			if (ex instanceof DataIntegrityViolationException) {
				throw new ApiException(409, "TENANT.DUPLICATE", "테넌트 코드 또는 스키마명이 이미 있습니다.", 2);
			}
			throw new ApiException(500, "TENANT.PROVISIONING_FAILED", ErrorMessages.mask(ex), 2);
		}
	}

	private ProvisioningResult readResult(String tenantCode, ProvisioningWork work) {
		ProvisioningJournal.TenantState tenant = journal.findTenant(tenantCode)
				.orElseThrow(() -> new ApiException(404, "TENANT.NOT_FOUND", "테넌트를 찾을 수 없습니다.", 2));
		ProvisioningJournal.ProvisioningState provisioning = journal.findProvisioning(tenant.tenantPk())
				.orElseThrow(() -> new ApiException(404, "TENANT.NOT_FOUND", "프로비저닝 기록이 없습니다.", 2));
		TemporaryPassword password = work == null ? null : work.temporaryPassword();
		boolean adminAlreadyPresent = work != null && work.adminAlreadyPresent();
		return new ProvisioningResult(tenantCode, tenant.schemaName(), tenant.statusCode(), provisioning.currentStep(),
				provisioning.executionStatus(), password, adminAlreadyPresent);
	}

	private static void rejectClosedStatus(String statusCode) {
		if ("ACTIVE".equals(statusCode)) {
			throw new ApiException(409, "TENANT.ALREADY_ACTIVE", "이미 사용 중인 테넌트입니다.", 2);
		}
		if ("SUSPENDED".equals(statusCode) || "TERMINATED".equals(statusCode)) {
			throw new ApiException(403, "TENANT.INACTIVE", "테넌트를 사용할 수 없습니다.", 2);
		}
	}

	private static String resolveSchema(ProvisionTenantCommand command) {
		try {
			SchemaNames.requireTenantCode(command.tenantCode());
			requireText(command.tenantName(), "테넌트 이름이 없습니다.");
			requireText(command.defaultLanguageCode(), "기본 언어가 없습니다.");
			requireText(command.adminName(), "관리자 이름이 없습니다.");
			requireText(command.adminEmail(), "관리자 이메일이 없습니다.");
			if (command.schemaName() == null || command.schemaName().isBlank()) {
				return SchemaNames.schemaNameForTenantCode(command.tenantCode());
			}
			SchemaNames.requireTenantSchema(command.schemaName());
			return command.schemaName();
		}
		catch (IllegalArgumentException ex) {
			throw new ApiException(400, "COMMON.VALIDATION_FAILED", ex.getMessage(), 2);
		}
	}

	private static void requireText(String value, String message) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(message);
		}
	}

}
