package com.tommy.common.tenant.provision;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;

import com.tommy.common.global.exception.ApiException;

class ProvisioningBehaviorTest {

	@Test
	void skipsFinishedStepsAndPausesWithoutHandler() {
		Set<ProvisioningStepCode> handlers = EnumSet.of(
				ProvisioningStepCode.REQUESTED,
				ProvisioningStepCode.SCHEMA_CREATED,
				ProvisioningStepCode.MIGRATED);
		ProvisioningPlanner.Decision first = ProvisioningPlanner.plan(null, handlers);
		assertEquals(java.util.List.of(
				ProvisioningStepCode.REQUESTED,
				ProvisioningStepCode.SCHEMA_CREATED,
				ProvisioningStepCode.MIGRATED), first.toRun());
		assertTrue(first.paused());

		ProvisioningPlanner.Decision retry = ProvisioningPlanner.plan(ProvisioningStepCode.SCHEMA_CREATED, handlers);
		assertEquals(java.util.List.of(ProvisioningStepCode.MIGRATED), retry.toRun());

		ProvisioningPlanner.Decision done = ProvisioningPlanner.plan(ProvisioningStepCode.MIGRATED, handlers);
		assertTrue(done.toRun().isEmpty());
		assertTrue(done.paused());

		Set<ProvisioningStepCode> withSeed = EnumSet.of(
				ProvisioningStepCode.REQUESTED,
				ProvisioningStepCode.SCHEMA_CREATED,
				ProvisioningStepCode.MIGRATED,
				ProvisioningStepCode.SEEDED);
		ProvisioningPlanner.Decision seeded = ProvisioningPlanner.plan(ProvisioningStepCode.MIGRATED, withSeed);
		assertEquals(java.util.List.of(ProvisioningStepCode.SEEDED), seeded.toRun());
		assertTrue(seeded.paused());
	}

	@Test
	void seedFilesAreSortedByName() {
		java.util.List<String> names = new TenantSeedRunner(mock(javax.sql.DataSource.class)).fileNames();
		assertEquals(java.util.List.of(
				"S01_support_language.sql",
				"S02_nation.sql",
				"S03_code.sql",
				"S04_i18n.sql",
				"S05_role_menu_permission.sql"), names);
	}

	@Test
	void lockFailureRejectsImmediately() throws Exception {
		DataSource dataSource = mock(DataSource.class);
		Connection connection = mock(Connection.class);
		Statement statement = mock(Statement.class);
		PreparedStatement prepared = mock(PreparedStatement.class);
		when(dataSource.getConnection()).thenReturn(connection);
		when(connection.createStatement()).thenReturn(statement);
		when(connection.prepareStatement(anyString())).thenReturn(prepared);
		when(statement.executeQuery(anyString())).thenAnswer(invocation -> databaseResult("pmis"));
		when(prepared.executeQuery()).thenAnswer(invocation -> lockResult(false));
		AtomicBoolean ran = new AtomicBoolean();
		ApiException exception = assertThrows(ApiException.class,
				() -> new PgTenantAdvisoryLock(dataSource).runWithTenantLock("t_it_abc", () -> ran.set(true)));
		assertEquals("TENANT.PROVISIONING_IN_PROGRESS", exception.errorCode());
		assertFalse(ran.get());
		verify(prepared, never()).executeUpdate();
	}

	@Test
	void temporaryPasswordIsMasked() {
		TemporaryPassword password = TemporaryPassword.generate();
		ProvisioningResult result = new ProvisioningResult("it-ab", "t_it_ab", "PROVISIONING",
				ProvisioningStepCode.MIGRATED, "RUNNING", password, false, null);
		assertFalse(password.toString().contains(password.reveal()));
		assertEquals("TemporaryPassword[masked]", password.toString());
		assertFalse(result.toString().contains(password.reveal()));
		assertTrue(ErrorMessages.mask(new IllegalStateException("jdbc:postgresql://db.example/pmis password=secret"))
				.contains("[masked-url]"));
		assertFalse(ErrorMessages.mask(new IllegalStateException("password=secret")).contains("secret"));
	}

	private static ResultSet databaseResult(String name) throws Exception {
		ResultSet resultSet = mock(ResultSet.class);
		when(resultSet.next()).thenReturn(true);
		when(resultSet.getString(1)).thenReturn(name);
		return resultSet;
	}

	private static ResultSet lockResult(boolean acquired) throws Exception {
		ResultSet resultSet = mock(ResultSet.class);
		when(resultSet.next()).thenReturn(true);
		when(resultSet.getBoolean(1)).thenReturn(acquired);
		return resultSet;
	}

}
