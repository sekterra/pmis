package com.tommy.common.global.multitenancy;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.hibernate.cfg.AvailableSettings;

@Configuration
public class MultitenancyConfiguration {

	@Bean
	public SchemaMultiTenantConnectionProvider multiTenantConnectionProvider(DataSource dataSource) {
		return new SchemaMultiTenantConnectionProvider(dataSource);
	}

	@Bean
	public RequestTenantIdentifierResolver currentTenantIdentifierResolver() {
		return new RequestTenantIdentifierResolver();
	}

	@Bean
	public HibernatePropertiesCustomizer multitenancyHibernateProperties(
			RequestTenantIdentifierResolver resolver,
			SchemaMultiTenantConnectionProvider provider) {
		return properties -> {
			properties.put(AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER, resolver);
			properties.put(AvailableSettings.MULTI_TENANT_CONNECTION_PROVIDER, provider);
		};
	}

	@Bean
	public ApplicationRunner databaseNameVerifier(DataSource dataSource) {
		return arguments -> verifyDatabaseName(dataSource);
	}

	@Bean
	public org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy databaseNameFlywayStrategy(
			DataSource dataSource) {
		return (Flyway flyway) -> {
			verifyDatabaseName(dataSource);
			flyway.migrate();
		};
	}

	@Bean
	@Profile("dev")
	public HeaderTenantResolver headerTenantResolver() {
		return new HeaderTenantResolver();
	}

	static void verifyDatabaseName(DataSource dataSource) {
		try (Connection connection = dataSource.getConnection();
				Statement statement = connection.createStatement();
				ResultSet resultSet = statement.executeQuery("SELECT current_database()")) {
			if (!resultSet.next()) {
				throw new IllegalStateException("접속 DB가 pmis 가 아닙니다.");
			}
			DatabaseNameGuard.require(resultSet.getString(1));
		}
		catch (java.sql.SQLException ex) {
			throw new IllegalStateException("접속 DB가 pmis 가 아닙니다.");
		}
	}

}
