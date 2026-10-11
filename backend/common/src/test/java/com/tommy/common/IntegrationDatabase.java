package com.tommy.common;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

final class IntegrationDatabase {

	private IntegrationDatabase() {
	}

	static void requirePmis(Connection connection) throws Exception {
		try (Statement statement = connection.createStatement();
				ResultSet resultSet = statement.executeQuery("SELECT current_database()")) {
			if (!resultSet.next() || !"pmis".equals(resultSet.getString(1))) {
				throw new IllegalStateException("접속 DB가 pmis 가 아닙니다.");
			}
		}
	}

}
