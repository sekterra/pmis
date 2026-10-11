package com.tommy.common.global.multitenancy;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.hibernate.service.UnknownUnwrapTypeException;

public class SchemaMultiTenantConnectionProvider implements MultiTenantConnectionProvider<String> {

	private final DataSource dataSource;

	public SchemaMultiTenantConnectionProvider(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	@Override
	public Connection getAnyConnection() throws SQLException {
		Connection connection = dataSource.getConnection();
		try {
			applySearchPath(connection, SchemaNames.PUBLIC_SCHEMA);
			return connection;
		}
		catch (SQLException | RuntimeException ex) {
			closeQuietly(connection);
			throw ex;
		}
	}

	@Override
	public void releaseAnyConnection(Connection connection) throws SQLException {
		release(connection);
	}

	@Override
	public Connection getConnection(String tenantIdentifier) throws SQLException {
		SchemaNames.requireSearchPath(tenantIdentifier);
		Connection connection = dataSource.getConnection();
		try {
			applySearchPath(connection, tenantIdentifier);
			return connection;
		}
		catch (SQLException | RuntimeException ex) {
			closeQuietly(connection);
			throw ex;
		}
	}

	@Override
	public void releaseConnection(String tenantIdentifier, Connection connection) throws SQLException {
		release(connection);
	}

	@Override
	public boolean supportsAggressiveRelease() {
		return false;
	}

	@Override
	public boolean handlesConnectionSchema() {
		return true;
	}

	@Override
	public boolean isUnwrappableAs(Class<?> unwrapType) {
		return unwrapType.isInstance(this) || unwrapType.isInstance(dataSource);
	}

	@Override
	public <T> T unwrap(Class<T> unwrapType) {
		if (unwrapType.isInstance(this)) {
			return unwrapType.cast(this);
		}
		if (unwrapType.isInstance(dataSource)) {
			return unwrapType.cast(dataSource);
		}
		throw new UnknownUnwrapTypeException(unwrapType);
	}

	private static void release(Connection connection) throws SQLException {
		try {
			resetSearchPath(connection);
			connection.close();
		}
		catch (SQLException ex) {
			abort(connection);
			throw ex;
		}
	}

	static void applySearchPath(Connection connection, String schemaName) throws SQLException {
		SchemaNames.requireSearchPath(schemaName);
		try (Statement statement = connection.createStatement()) {
			statement.execute("SET search_path TO \"" + schemaName + "\"");
		}
	}

	static void resetSearchPath(Connection connection) throws SQLException {
		try (Statement statement = connection.createStatement()) {
			statement.execute("RESET search_path");
		}
	}

	private static void abort(Connection connection) {
		try {
			connection.abort(Runnable::run);
		}
		catch (SQLException ignored) {
			closeQuietly(connection);
		}
	}

	private static void closeQuietly(Connection connection) {
		try {
			connection.close();
		}
		catch (SQLException ignored) {
			// 이미 실패한 커넥션은 풀에 되돌리지 못하면 닫기만 시도한다.
		}
	}

}
