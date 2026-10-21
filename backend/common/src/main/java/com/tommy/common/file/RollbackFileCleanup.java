package com.tommy.common.file;

import java.nio.file.Path;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

final class RollbackFileCleanup {

	private RollbackFileCleanup() {
	}

	static void deleteOnRollback(LocalFileStore store, Path path) {
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCompletion(int status) {
				if (status == TransactionSynchronization.STATUS_ROLLED_BACK) {
					store.deleteQuiet(path);
				}
			}
		});
	}

}
