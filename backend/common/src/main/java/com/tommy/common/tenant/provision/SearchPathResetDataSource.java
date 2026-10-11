package com.tommy.common.tenant.provision;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.springframework.jdbc.datasource.AbstractDataSource;

final class SearchPathResetDataSource extends AbstractDataSource {

	private final DataSource delegate;

	SearchPathResetDataSource(DataSource delegate) {
		this.delegate = delegate;
	}

	@Override
	public Connection getConnection() throws SQLException {
		return resetOnClose(delegate.getConnection());
	}

	@Override
	public Connection getConnection(String username, String password) throws SQLException {
		return resetOnClose(delegate.getConnection(username, password));
	}

	private static Connection resetOnClose(Connection connection) {
		InvocationHandler handler = (proxy, method, args) -> {
			if ("close".equals(method.getName()) && args == null) {
				reset(connection);
				return method.invoke(connection);
			}
			try {
				return method.invoke(connection, args);
			}
			catch (InvocationTargetException ex) {
				throw ex.getCause();
			}
		};
		return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(), new Class<?>[] { Connection.class },
				handler);
	}

	private static void reset(Connection connection) throws SQLException {
		try (Statement statement = connection.createStatement()) {
			statement.execute("RESET search_path");
		}
		catch (SQLException ex) {
			connection.abort(Runnable::run);
			throw ex;
		}
	}

}
