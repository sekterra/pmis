package com.tommy.common.tenant.provision;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Component;

import com.tommy.common.global.multitenancy.DatabaseNameGuard;
import com.tommy.common.global.multitenancy.SchemaNames;

@Component
public class TenantSeedRunner {

	static final String LOCATION = "classpath:db/seed/tenant/S*.sql";

	private final DataSource dataSource;
	private final ResourcePatternResolver resolver;

	public TenantSeedRunner(DataSource dataSource) {
		this.dataSource = dataSource;
		this.resolver = new PathMatchingResourcePatternResolver();
	}

	public void run(String schemaName) {
		SchemaNames.requireTenantSchema(schemaName);
		List<Resource> scripts = scripts();
		if (scripts.isEmpty()) {
			throw new IllegalStateException("테넌트 Seed 파일이 없습니다.");
		}
		for (Resource script : scripts) {
			runFile(schemaName, script);
		}
	}

	public List<String> fileNames() {
		return scripts().stream().map(Resource::getFilename).toList();
	}

	private List<Resource> scripts() {
		try {
			Resource[] resources = resolver.getResources(LOCATION);
			return Arrays.stream(resources)
					.sorted(Comparator.comparing(resource -> resource.getFilename() == null ? "" : resource.getFilename()))
					.toList();
		}
		catch (java.io.IOException ex) {
			throw new IllegalStateException("테넌트 Seed 파일을 읽지 못했습니다.");
		}
	}

	private void runFile(String schemaName, Resource script) {
		try (Connection connection = dataSource.getConnection()) {
			requireDatabase(connection);
			connection.setAutoCommit(false);
			try {
				setSearchPath(connection, schemaName);
				ScriptUtils.executeSqlScript(connection, new EncodedResource(script, java.nio.charset.StandardCharsets.UTF_8));
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
			throw new IllegalStateException("테넌트 Seed 를 실행하지 못했습니다.");
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
		catch (SQLException ignored) {
			// 실패한 커넥션은 풀에 되돌리지 못한다.
		}
	}

}
