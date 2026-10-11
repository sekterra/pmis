package com.tommy.common.tenant.provision;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.springframework.stereotype.Component;

import com.tommy.common.global.exception.ApiException;
import com.tommy.common.global.multitenancy.DatabaseNameGuard;
import com.tommy.common.global.multitenancy.SchemaNames;

@Component
public class PgTenantAdvisoryLock {

	private final DataSource dataSource;

	public PgTenantAdvisoryLock(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	public void runWithTenantLock(String schemaName, Runnable work) {
		SchemaNames.requireTenantSchema(schemaName);
		run("SELECT pg_try_advisory_lock(hashtext('pmis.tenant'), hashtext(?))",
				"SELECT pg_advisory_unlock(hashtext('pmis.tenant'), hashtext(?))",
				schemaName,
				"TENANT.PROVISIONING_IN_PROGRESS",
				"같은 테넌트 작업이 진행 중입니다.",
				work);
	}

	public void runWithUpgradeBatchLock(Runnable work) {
		run("SELECT pg_try_advisory_lock(hashtext('pmis.tenant.upgrade-all'), 0)",
				"SELECT pg_advisory_unlock(hashtext('pmis.tenant.upgrade-all'), 0)",
				null,
				"TENANT.UPGRADE_IN_PROGRESS",
				"일괄 업그레이드가 진행 중입니다.",
				work);
	}

	private void run(String lockSql, String unlockSql, String schemaName, String errorCode, String message,
			Runnable work) {
		try (Connection connection = dataSource.getConnection()) {
			requireDatabase(connection);
			boolean acquired = false;
			try {
				acquired = tryLock(connection, lockSql, schemaName);
				if (!acquired) {
					throw new ApiException(409, errorCode, message, 2);
				}
				work.run();
			}
			finally {
				if (acquired) {
					unlock(connection, unlockSql, schemaName);
				}
			}
		}
		catch (SQLException ex) {
			throw new IllegalStateException("테넌트 잠금을 처리하지 못했습니다.");
		}
	}

	private static void requireDatabase(Connection connection) throws SQLException {
		try (Statement statement = connection.createStatement();
				ResultSet resultSet = statement.executeQuery("SELECT current_database()")) {
			if (!resultSet.next()) {
				throw new IllegalStateException("접속 DB가 pmis 가 아닙니다.");
			}
			DatabaseNameGuard.require(resultSet.getString(1));
		}
	}

	private static boolean tryLock(Connection connection, String sql, String schemaName) throws SQLException {
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			if (schemaName != null) {
				statement.setString(1, schemaName);
			}
			try (ResultSet resultSet = statement.executeQuery()) {
				return resultSet.next() && resultSet.getBoolean(1);
			}
		}
	}

	private static void unlock(Connection connection, String sql, String schemaName) throws SQLException {
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			if (schemaName != null) {
				statement.setString(1, schemaName);
			}
			try (ResultSet resultSet = statement.executeQuery()) {
				resultSet.next();
			}
		}
	}

}
