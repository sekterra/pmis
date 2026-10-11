package com.tommy.common.tenant.provision;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import com.tommy.common.global.multitenancy.DatabaseNameGuard;
import com.tommy.common.global.multitenancy.SchemaNames;

final class TenantSchemaWork {

	private TenantSchemaWork() {
	}

	static void run(DataSource dataSource, String schemaName, SqlWork work) {
		SchemaNames.requireTenantSchema(schemaName);
		try (Connection connection = dataSource.getConnection()) {
			requireDatabase(connection);
			connection.setAutoCommit(false);
			try {
				setSearchPath(connection, schemaName);
				work.accept(connection);
				connection.commit();
			}
			catch (RuntimeException | SQLException ex) {
				rollback(connection);
				throw ex;
			}
			finally {
				resetSearchPath(connection);
			}
		}
		catch (SQLException ex) {
			throw new IllegalStateException(ErrorMessages.mask(ex));
		}
	}

	private static void requireDatabase(Connection connection) throws SQLException {
		try (Statement statement = connection.createStatement();
				java.sql.ResultSet resultSet = statement.executeQuery("SELECT current_database()")) {
			if (!resultSet.next()) {
				throw new IllegalStateException("접속 DB가 pmis 가 아닙니다.");
			}
			DatabaseNameGuard.require(resultSet.getString(1));
		}
	}

	private static void setSearchPath(Connection connection, String schemaName) throws SQLException {
		try (Statement statement = connection.createStatement()) {
			statement.execute("SET search_path TO \"" + schemaName + "\"");
		}
	}

	private static void resetSearchPath(Connection connection) {
		try (Statement statement = connection.createStatement()) {
			statement.execute("RESET search_path");
		}
		catch (SQLException ex) {
			abort(connection);
		}
	}

	private static void rollback(Connection connection) {
		try {
			connection.rollback();
		}
		catch (SQLException ex) {
			abort(connection);
		}
	}

	private static void abort(Connection connection) {
		try {
			connection.abort(Runnable::run);
		}
		catch (SQLException ex) {
			// 반납에 실패하면 풀이 이 연결을 다시 쓰지 못한다.
		}
	}

	@FunctionalInterface
	interface SqlWork {
		void accept(Connection connection) throws SQLException;
	}

}
