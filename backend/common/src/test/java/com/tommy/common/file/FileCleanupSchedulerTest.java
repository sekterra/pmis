package com.tommy.common.file;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import com.tommy.common.file.FileCleanupScheduler.CleanupSummary;
import com.tommy.common.file.dto.FileDtos.CleanupResponse;
import com.tommy.common.tenant.provision.ProvisioningJournal;
import com.tommy.common.tenant.provision.ProvisioningJournal.TenantState;

class FileCleanupSchedulerTest {

	private final ApplicationContextRunner runner = new ApplicationContextRunner()
			.withUserConfiguration(FileCleanupSchedulingConfig.class, FileCleanupScheduler.class);

	@Test
	void beanExistsOnlyWhenCleanupIsEnabled() {
		runner.withPropertyValues("mdm.file.cleanup.enabled=false")
				.run(context -> assertEquals(0, context.getBeanNamesForType(FileCleanupScheduler.class).length));
		runner.withBean(FileService.class, () -> mock(FileService.class))
				.withBean(ProvisioningJournal.class, () -> mock(ProvisioningJournal.class))
				.withPropertyValues("mdm.file.cleanup.enabled=true", "mdm.file.cleanup.cron=0 0 3 * * *", "mdm.file.cleanup.zone=Asia/Seoul")
				.run(context -> assertEquals(1, context.getBeanNamesForType(FileCleanupScheduler.class).length));
	}

	@Test
	void oneTenantFailureDoesNotStopTheNextAndOverlapIsSkipped() {
		FileService files = mock(FileService.class);
		FileCleanupScheduler scheduler = new FileCleanupScheduler(files, mock(ProvisioningJournal.class));
		TenantState failed = new TenantState(1, "t_it_missing00000001", "ACTIVE", "it-missing");
		TenantState cleaned = new TenantState(2, "t_it_cleaned00000001", "ACTIVE", "it-cleaned");
		when(files.cleanupInternal(null)).thenThrow(new IllegalStateException("hidden path")).thenReturn(new CleanupResponse(3, 1, 2));

		CleanupSummary summary = scheduler.runOnce(List.of(failed, cleaned));

		assertEquals(2, summary.tenantCount());
		assertEquals(1, summary.failureCount());
		assertEquals(1, summary.successCount());
		assertEquals(3, summary.expiredTempCount());
		assertEquals(1, summary.purgedCount());
		assertEquals(2, summary.diskFailureCount());
		assertFalse(summary.overlap());

		AtomicReference<CleanupSummary> overlap = new AtomicReference<>();
		when(files.cleanupInternal(null)).thenAnswer(invocation -> {
			overlap.set(scheduler.runOnce(List.of(cleaned)));
			return new CleanupResponse(1, 0, 0);
		});
		CleanupSummary outer = scheduler.runOnce(List.of(cleaned));
		assertTrue(overlap.get().overlap());
		assertEquals(1, outer.successCount());
		verify(files, times(3)).cleanupInternal(null);
	}

}
