package com.tommy.common.file.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class StoredFilePurgeTest {

	@Test
	void markPurgedClearsScheduleSoTheNextRunSkipsIt() {
		StoredFile file = StoredFile.upload(null, "k", "a.png", "png", "image/png", 1, null, 1L, LocalDateTime.now().minusDays(2));
		file.markDeleted(LocalDateTime.now().minusMinutes(1), 1L);
		assertEquals("DELETED", file.getFileStatusCode());
		file.markPurged(null);
		assertNull(file.getDeleteScheduledDatetime());
		assertEquals("DELETED", file.getFileStatusCode());
		assertEquals("N", file.getUseYn());
		assertNull(file.getUpdateUserPk());
	}

}
