package com.tommy.common.tenant.provision;

import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class ProvisioningJournal {

	private final JdbcTemplate jdbcTemplate;
	private final TransactionTemplate requiresNew;

	public ProvisioningJournal(JdbcTemplate jdbcTemplate, PlatformTransactionManager transactionManager) {
		this.jdbcTemplate = jdbcTemplate;
		this.requiresNew = new TransactionTemplate(transactionManager);
		this.requiresNew.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
	}

	public Optional<TenantState> findTenant(String tenantCode) {
		return requiresNew.execute(status -> jdbcTemplate.query("""
				SELECT tenant_pk, schema_name, tenant_status_code
				FROM pmis.tenant
				WHERE tenant_code = ?
				""", (resultSet, row) -> new TenantState(
				resultSet.getLong("tenant_pk"),
				resultSet.getString("schema_name"),
				resultSet.getString("tenant_status_code")), tenantCode).stream().findFirst());
	}

	public Optional<ProvisioningState> findProvisioning(long tenantPk) {
		return requiresNew.execute(status -> jdbcTemplate.query("""
				SELECT current_step_code, execution_status_code, retry_count
				FROM pmis.tenant_provisioning
				WHERE tenant_pk = ?
				ORDER BY provisioning_pk DESC
				LIMIT 1
				""", (resultSet, row) -> new ProvisioningState(
				ProvisioningStepCode.valueOf(resultSet.getString("current_step_code")),
				resultSet.getString("execution_status_code"),
				resultSet.getInt("retry_count")), tenantPk).stream().findFirst());
	}

	public long insertTenant(ProvisioningWork work) {
		Long tenantPk = requiresNew.execute(status -> {
			Long inserted = jdbcTemplate.queryForObject("""
					INSERT INTO pmis.tenant (
					    tenant_code, tenant_name, schema_name, tenant_status_code, default_language_code, use_yn, create_user_pk)
					VALUES (?, ?, ?, 'PROVISIONING', ?, 'Y', ?)
					RETURNING tenant_pk
					""", Long.class,
					work.command().tenantCode(),
					work.command().tenantName(),
					work.schemaName(),
					work.command().defaultLanguageCode(),
					work.command().executedByUserPk());
			insertHistory(inserted, null, "PROVISIONING", "REQUESTED", work.command().executedByUserPk());
			jdbcTemplate.update("""
					INSERT INTO pmis.tenant_provisioning (
					    tenant_pk, current_step_code, execution_status_code, admin_name, admin_email,
					    retry_count, requested_datetime, create_user_pk)
					VALUES (?, 'REQUESTED', 'RUNNING', ?, ?, 0, CURRENT_TIMESTAMP, ?)
					""", inserted, work.command().adminName(), work.command().adminEmail(), work.command().executedByUserPk());
			return inserted;
		});
		return tenantPk;
	}

	public void resume(ProvisioningWork work) {
		requiresNew.executeWithoutResult(status -> {
			int updated = jdbcTemplate.update("""
					UPDATE pmis.tenant
					SET tenant_status_code = 'PROVISIONING', update_datetime = CURRENT_TIMESTAMP, update_user_pk = ?
					WHERE tenant_pk = ? AND tenant_status_code = 'FAILED'
					""", work.command().executedByUserPk(), work.tenantPk());
			if (updated > 0) {
				insertHistory(work.tenantPk(), "FAILED", "PROVISIONING", "RETRY", work.command().executedByUserPk());
			}
			jdbcTemplate.update("""
					UPDATE pmis.tenant_provisioning
					SET execution_status_code = 'RUNNING', error_message = NULL,
					    update_datetime = CURRENT_TIMESTAMP, update_user_pk = ?
					WHERE tenant_pk = ?
					""", work.command().executedByUserPk(), work.tenantPk());
		});
	}

	public void markStep(ProvisioningWork work, ProvisioningStepCode step) {
		requiresNew.executeWithoutResult(status -> jdbcTemplate.update("""
				UPDATE pmis.tenant_provisioning
				SET current_step_code = ?, execution_status_code = 'RUNNING', error_message = NULL,
				    update_datetime = CURRENT_TIMESTAMP, update_user_pk = ?
				WHERE tenant_pk = ?
				""", step.name(), work.command().executedByUserPk(), work.tenantPk()));
	}

	public void markPaused(ProvisioningWork work) {
		if (work.lastSuccess() == null) {
			return;
		}
		markStep(work, work.lastSuccess());
	}

	public void markFailed(ProvisioningWork work, String errorMessage) {
		if (work.tenantPk() == 0) {
			return;
		}
		requiresNew.executeWithoutResult(status -> {
			String fromStatus = jdbcTemplate.queryForObject(
					"SELECT tenant_status_code FROM pmis.tenant WHERE tenant_pk = ?",
					String.class, work.tenantPk());
			jdbcTemplate.update("""
					UPDATE pmis.tenant
					SET tenant_status_code = 'FAILED', update_datetime = CURRENT_TIMESTAMP, update_user_pk = ?
					WHERE tenant_pk = ?
					""", work.command().executedByUserPk(), work.tenantPk());
			if (!"FAILED".equals(fromStatus)) {
				insertHistory(work.tenantPk(), fromStatus, "FAILED", "PROVISIONING_FAILED", work.command().executedByUserPk());
			}
			jdbcTemplate.update("""
					UPDATE pmis.tenant_provisioning
					SET current_step_code = ?, execution_status_code = 'FAILED', error_message = ?,
					    retry_count = retry_count + 1, update_datetime = CURRENT_TIMESTAMP, update_user_pk = ?
					WHERE tenant_pk = ?
					""",
					work.lastSuccess() == null ? "REQUESTED" : work.lastSuccess().name(),
					errorMessage,
					work.command().executedByUserPk(),
					work.tenantPk());
		});
	}

	public void complete(long tenantPk, String schemaVersion, Long executedByUserPk) {
		requiresNew.executeWithoutResult(status -> {
			String current = jdbcTemplate.queryForObject(
					"SELECT tenant_status_code FROM pmis.tenant WHERE tenant_pk = ?",
					String.class, tenantPk);
			if ("ACTIVE".equals(current)) {
				return;
			}
			jdbcTemplate.update("""
					UPDATE pmis.tenant
					SET tenant_status_code = 'ACTIVE', activated_datetime = CURRENT_TIMESTAMP, schema_version = ?,
					    update_datetime = CURRENT_TIMESTAMP, update_user_pk = ?
					WHERE tenant_pk = ?
					""", schemaVersion, executedByUserPk, tenantPk);
			insertHistory(tenantPk, current, "ACTIVE", "COMPLETED", executedByUserPk);
			jdbcTemplate.update("""
					UPDATE pmis.tenant_provisioning
					SET current_step_code = 'COMPLETED', execution_status_code = 'SUCCESS', error_message = NULL,
					    completed_datetime = CURRENT_TIMESTAMP, update_datetime = CURRENT_TIMESTAMP, update_user_pk = ?
					WHERE tenant_pk = ?
					""", executedByUserPk, tenantPk);
		});
	}

	public void updateSchemaVersion(long tenantPk, String schemaVersion, Long executedByUserPk) {
		requiresNew.executeWithoutResult(status -> jdbcTemplate.update("""
				UPDATE pmis.tenant
				SET schema_version = ?, update_datetime = CURRENT_TIMESTAMP, update_user_pk = ?
				WHERE tenant_pk = ?
				""", schemaVersion, executedByUserPk, tenantPk));
	}

	public long beginMigration(long tenantPk, String migrationType, Long executedByUserPk) {
		Long migrationPk = requiresNew.execute(status -> jdbcTemplate.queryForObject("""
				INSERT INTO pmis.tenant_schema_migration (
				    tenant_pk, migration_type_code, target_version, execution_status_code, started_datetime, executed_user_pk)
				VALUES (?, ?, 'pending', 'RUNNING', CURRENT_TIMESTAMP, ?)
				RETURNING migration_pk
				""", Long.class, tenantPk, migrationType, executedByUserPk));
		return migrationPk;
	}

	public void finishMigration(long migrationPk, String targetVersion, String executionStatus, String errorMessage) {
		requiresNew.executeWithoutResult(status -> jdbcTemplate.update("""
				UPDATE pmis.tenant_schema_migration
				SET target_version = ?, execution_status_code = ?, finished_datetime = CURRENT_TIMESTAMP, error_message = ?
				WHERE migration_pk = ?
				""", targetVersion, executionStatus, errorMessage, migrationPk));
	}

	public void insertMigration(long tenantPk, String migrationType, String targetVersion, String executionStatus,
			String errorMessage, Long executedByUserPk) {
		requiresNew.executeWithoutResult(status -> jdbcTemplate.update("""
				INSERT INTO pmis.tenant_schema_migration (
				    tenant_pk, migration_type_code, target_version, execution_status_code,
				    started_datetime, finished_datetime, error_message, executed_user_pk)
				VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, ?, ?)
				""", tenantPk, migrationType, targetVersion, executionStatus, errorMessage, executedByUserPk));
	}

	public java.util.List<TenantState> listTenants() {
		return requiresNew.execute(status -> jdbcTemplate.query("""
				SELECT tenant_pk, schema_name, tenant_status_code, tenant_code
				FROM pmis.tenant
				ORDER BY tenant_pk
				""", (resultSet, row) -> new TenantState(
				resultSet.getLong("tenant_pk"),
				resultSet.getString("schema_name"),
				resultSet.getString("tenant_status_code"),
				resultSet.getString("tenant_code"))));
	}

	public boolean schemaExists(String schemaName) {
		Boolean exists = requiresNew.execute(status -> jdbcTemplate.queryForObject(
				"SELECT EXISTS (SELECT 1 FROM pg_namespace WHERE nspname = ?)", Boolean.class, schemaName));
		return Boolean.TRUE.equals(exists);
	}

	public void createSchema(String schemaName) {
		com.tommy.common.global.multitenancy.SchemaNames.requireTenantSchema(schemaName);
		requiresNew.executeWithoutResult(status -> jdbcTemplate.execute("CREATE SCHEMA \"" + schemaName + "\""));
	}

	private void insertHistory(long tenantPk, String fromStatus, String toStatus, String reason, Long executedByUserPk) {
		if (fromStatus != null && fromStatus.equals(toStatus)) {
			return;
		}
		jdbcTemplate.update("""
				INSERT INTO pmis.tenant_status_history (
				    tenant_pk, from_status_code, to_status_code, change_reason, change_datetime, change_user_pk)
				VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, ?)
				""", tenantPk, fromStatus, toStatus, reason, executedByUserPk);
	}

	public record TenantState(long tenantPk, String schemaName, String statusCode, String tenantCode) {
		public TenantState(long tenantPk, String schemaName, String statusCode) {
			this(tenantPk, schemaName, statusCode, null);
		}
	}

	public record ProvisioningState(ProvisioningStepCode currentStep, String executionStatus, int retryCount) {
	}

}
