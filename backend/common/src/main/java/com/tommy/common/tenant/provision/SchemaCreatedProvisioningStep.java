package com.tommy.common.tenant.provision;

import org.springframework.stereotype.Component;

import com.tommy.common.global.exception.ApiException;
import com.tommy.common.global.multitenancy.SchemaNames;

@Component
public class SchemaCreatedProvisioningStep implements ProvisioningStep {

	private final ProvisioningJournal journal;

	public SchemaCreatedProvisioningStep(ProvisioningJournal journal) {
		this.journal = journal;
	}

	@Override
	public ProvisioningStepCode code() {
		return ProvisioningStepCode.SCHEMA_CREATED;
	}

	@Override
	public void execute(ProvisioningWork work) {
		SchemaNames.requireTenantSchema(work.schemaName());
		if (journal.schemaExists(work.schemaName())) {
			throw new ApiException(409, "TENANT.SCHEMA_NOT_OWNED", "스키마가 이미 있으나 프로비저닝 기록이 없습니다.", 2);
		}
		journal.createSchema(work.schemaName());
	}

}
