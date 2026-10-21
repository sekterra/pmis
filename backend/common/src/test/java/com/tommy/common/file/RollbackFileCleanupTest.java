package com.tommy.common.file;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class RollbackFileCleanupTest {

	@Test
	void deletesTheFileOnlyWhenTheTransactionRollsBack(@TempDir Path root) throws Exception {
		LocalFileStore store = new LocalFileStore();
		Path target = root.resolve("orphan.bin");
		store.write(target, new byte[] { 1, 2, 3 });
		TransactionSynchronizationManager.initSynchronization();
		try {
			RollbackFileCleanup.deleteOnRollback(store, target);
			TransactionSynchronizationManager.getSynchronizations()
					.forEach(sync -> sync.afterCompletion(TransactionSynchronization.STATUS_COMMITTED));
		}
		finally {
			TransactionSynchronizationManager.clearSynchronization();
		}
		assertTrue(Files.exists(target));

		TransactionSynchronizationManager.initSynchronization();
		try {
			RollbackFileCleanup.deleteOnRollback(store, target);
			TransactionSynchronizationManager.getSynchronizations()
					.forEach(sync -> sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
		}
		finally {
			TransactionSynchronizationManager.clearSynchronization();
		}
		assertFalse(Files.exists(target));
	}

}
