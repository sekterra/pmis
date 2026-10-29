package com.tommy.common.supportaccess;

import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class SupportAuditStore {

	private final JdbcTemplate jdbcTemplate;

	private final TransactionTemplate requiresNew;

	private final AtomicBoolean failNextInsert = new AtomicBoolean();

	public SupportAuditStore(JdbcTemplate jdbcTemplate, PlatformTransactionManager transactionManager) {
		this.jdbcTemplate = jdbcTemplate;
		TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		this.requiresNew = template;
	}

	public void failNextInsert() {
		failNextInsert.set(true);
	}

	public long insert(long supportAccessPk, String method, String path, boolean sensitive) {
		if (failNextInsert.compareAndSet(true, false)) {
			throw new IllegalStateException("지원 접속 감사를 기록하지 못했습니다.");
		}
		Long actionPk = requiresNew.execute(status -> jdbcTemplate.queryForObject("""
				INSERT INTO pmis.tenant_support_access_action (
				    action_pk, support_access_pk, occurred_datetime, http_method, path, sensitive_yn)
				VALUES (nextval('pmis.tenant_support_access_action_seq'), ?, CURRENT_TIMESTAMP, ?, ?, ?)
				RETURNING action_pk
				""", Long.class, supportAccessPk, method, path, sensitive ? "Y" : "N"));
		if (actionPk == null) {
			throw new IllegalStateException("지원 접속 감사를 기록하지 못했습니다.");
		}
		return actionPk;
	}

	public void complete(long actionPk, int statusCode) {
		requiresNew.executeWithoutResult(status -> jdbcTemplate.update("""
				UPDATE pmis.tenant_support_access_action
				SET status_code = ?
				WHERE action_pk = ?
				""", statusCode, actionPk));
	}

}
