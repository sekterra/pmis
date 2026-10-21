package com.tommy.common.file;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.tommy.common.file.dto.FileDtos.CleanupResponse;
import com.tommy.common.global.multitenancy.TenantContext;
import com.tommy.common.tenant.provision.ProvisioningJournal;
import com.tommy.common.tenant.provision.ProvisioningJournal.TenantState;

@Component
@ConditionalOnProperty(prefix = "mdm.file.cleanup", name = "enabled", havingValue = "true")
public class FileCleanupScheduler {

	private static final Logger log = LoggerFactory.getLogger(FileCleanupScheduler.class);

	private final AtomicBoolean running = new AtomicBoolean(false);

	private final FileService files;

	private final ProvisioningJournal journal;

	public FileCleanupScheduler(FileService files, ProvisioningJournal journal) {
		this.files = files;
		this.journal = journal;
	}

	@Scheduled(cron = "${mdm.file.cleanup.cron}", zone = "${mdm.file.cleanup.zone}")
	public void scheduled() {
		runOnce();
	}

	public CleanupSummary runOnce() {
		return runOnce(activeTenants());
	}

	public CleanupSummary runOnce(List<TenantState> tenants) {
		if (!running.compareAndSet(false, true)) {
			log.info("file cleanup skipped because another run is active");
			return CleanupSummary.overlapped();
		}
		int success = 0;
		int failure = 0;
		int expired = 0;
		int purged = 0;
		int diskFailure = 0;
		try {
			for (TenantState tenant : tenants) {
				TenantContext.beginRequest();
				try {
					TenantContext.setSchema(tenant.schemaName());
					CleanupResponse result = files.cleanupInternal(null);
					success++;
					expired += result.expiredTempCount();
					purged += result.purgedCount();
					diskFailure += result.diskFailureCount();
				}
				catch (RuntimeException ex) {
					failure++;
					log.warn("file cleanup failed tenantCode={} exceptionType={}", tenant.tenantCode(), ex.getClass().getName());
				}
				finally {
					TenantContext.clear();
				}
			}
		}
		finally {
			running.set(false);
		}
		log.info("file cleanup tenants={} success={} failure={} expired={} purged={} diskFailure={}", tenants.size(), success, failure,
				expired, purged, diskFailure);
		return new CleanupSummary(tenants.size(), success, failure, expired, purged, diskFailure, false);
	}

	private List<TenantState> activeTenants() {
		List<TenantState> active = new ArrayList<>();
		for (TenantState tenant : journal.listTenants()) {
			if ("ACTIVE".equals(tenant.statusCode())) {
				active.add(tenant);
			}
		}
		return active;
	}

	public record CleanupSummary(int tenantCount, int successCount, int failureCount, int expiredTempCount, int purgedCount,
			int diskFailureCount, boolean overlap) {

		public static CleanupSummary overlapped() {
			return new CleanupSummary(0, 0, 0, 0, 0, 0, true);
		}
	}

}
