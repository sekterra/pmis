package com.tommy.common;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;

import javax.sql.DataSource;

import com.tommy.common.global.security.JwtTokenService;

public final class TestAccess {

	private TestAccess() {
	}

	public static String issueAdmin(JwtTokenService tokens, DataSource dataSource, String schema, String tenantCode)
			throws Exception {
		if (!schema.matches("^t_it_[a-z0-9]{16}$")) {
			throw new IllegalArgumentException("테스트 스키마가 아닙니다.");
		}
		long userPk;
		try (Connection connection = dataSource.getConnection()) {
			IntegrationDatabase.requirePmis(connection);
			try (Statement statement = connection.createStatement()) {
				statement.execute("SET search_path TO " + schema);
				try (ResultSet resultSet = statement.executeQuery(
						"SELECT user_pk FROM user_account WHERE login_id = 'admin@example.com'")) {
					if (!resultSet.next()) {
						throw new IllegalStateException("관리자 계정이 없습니다.");
					}
					userPk = resultSet.getLong(1);
				}
			}
		}
		return tokens.issue(Long.toString(userPk), JwtTokenService.TENANT_AUDIENCE, JwtTokenService.TENANT_TYPE, tenantCode,
				false, Duration.ofMinutes(15));
	}

}
