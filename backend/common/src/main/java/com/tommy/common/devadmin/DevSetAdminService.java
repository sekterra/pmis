package com.tommy.common.devadmin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;

import javax.sql.DataSource;

import org.springframework.stereotype.Service;

import com.tommy.common.devadmin.DevSetAdminRules.Command;
import com.tommy.common.global.multitenancy.SchemaNames;
import com.tommy.common.global.multitenancy.TenantContext;
import com.tommy.common.global.security.PasswordHasher;

@Service
public class DevSetAdminService {

	private final DataSource dataSource;

	private final PasswordHasher passwordHasher;

	public DevSetAdminService(DataSource dataSource, PasswordHasher passwordHasher) {
		this.dataSource = dataSource;
		this.passwordHasher = passwordHasher;
	}

	public String apply(Command command) {
		requireDatabase();
		if ("platform".equals(command.target())) {
			return inConnection(connection -> platform(connection, command));
		}
		TenantRow tenant = requireTenant(command.tenantCode());
		TenantContext.beginRequest();
		try {
			TenantContext.setSchema(tenant.schemaName());
			return inConnection(connection -> tenant(connection, tenant.schemaName(), command, tenant.languageCode(), tenant.timezoneId()));
		}
		finally {
			TenantContext.clear();
		}
	}

	private void requireDatabase() {
		try (Connection connection = dataSource.getConnection();
				Statement statement = connection.createStatement();
				ResultSet resultSet = statement.executeQuery("SELECT current_database()")) {
			if (!resultSet.next() || !"pmis".equals(resultSet.getString(1))) {
				throw new IllegalStateException("데이터베이스 이름이 pmis 가 아닙니다.");
			}
		}
		catch (SQLException ex) {
			throw new IllegalStateException("데이터베이스 이름이 pmis 가 아닙니다.");
		}
	}

	private TenantRow requireTenant(String tenantCode) {
		try (Connection connection = dataSource.getConnection();
				PreparedStatement statement = connection.prepareStatement("""
						SELECT schema_name, tenant_status_code, btrim(use_yn), default_language_code, timezone_id
						FROM pmis.tenant
						WHERE tenant_code = ?
						""")) {
			statement.setString(1, tenantCode);
			String schemaName;
			String status;
			String useYn;
			String languageCode;
			String timezoneId;
			try (ResultSet resultSet = statement.executeQuery()) {
				if (!resultSet.next()) {
					throw new IllegalStateException("테넌트를 찾을 수 없습니다.");
				}
				schemaName = resultSet.getString(1);
				status = resultSet.getString(2);
				useYn = resultSet.getString(3);
				languageCode = resultSet.getString(4);
				timezoneId = resultSet.getString(5);
			}
			if (!"Y".equals(useYn)) {
				throw new IllegalStateException("테넌트를 찾을 수 없습니다.");
			}
			if (!"ACTIVE".equals(status)) {
				throw new IllegalStateException("테넌트를 사용할 수 없습니다.");
			}
			try {
				SchemaNames.requireTenantSchema(schemaName);
			}
			catch (IllegalArgumentException ex) {
				throw new IllegalStateException("테넌트 스키마가 없습니다.");
			}
			if (!schemaExists(connection, schemaName)) {
				throw new IllegalStateException("테넌트 스키마가 없습니다.");
			}
			return new TenantRow(schemaName, languageCode, timezoneId);
		}
		catch (SQLException ex) {
			throw new IllegalStateException("테넌트를 찾을 수 없습니다.");
		}
	}

	private static boolean schemaExists(Connection connection, String schemaName) throws SQLException {
		try (PreparedStatement statement = connection.prepareStatement(
				"SELECT 1 FROM pg_catalog.pg_namespace WHERE nspname = ?")) {
			statement.setString(1, schemaName);
			try (ResultSet resultSet = statement.executeQuery()) {
				return resultSet.next();
			}
		}
	}

	private String platform(Connection connection, Command command) throws SQLException {
		String hash = passwordHasher.hash(command.password());
		Long adminPk = findPlatformAdmin(connection, command.loginId());
		boolean created = adminPk == null;
		if (created) {
			adminPk = insertPlatformAdmin(connection, command.loginId(), hash);
		}
		else {
			updatePlatformAdmin(connection, adminPk, hash);
		}
		revokePlatformRefresh(connection, adminPk);
		return DevSetAdminRules.line("platform", null, command.loginId(), created);
	}

	private String tenant(Connection connection, String schemaName, Command command, String languageCode, String timezoneId)
			throws SQLException {
		setSearchPath(connection, schemaName);
		String hash = passwordHasher.hash(command.password());
		Long userPk = findUser(connection, command.loginId());
		boolean created = userPk == null;
		if (created) {
			userPk = insertUser(connection, command.loginId(), hash, languageCode, timezoneId);
		}
		else {
			updateUser(connection, userPk, hash);
		}
		long rolePk = requireTenantAdminRole(connection);
		grantTenantAdmin(connection, userPk, rolePk);
		insertHistory(connection, userPk, hash);
		revokeUserRefresh(connection, userPk);
		return DevSetAdminRules.line("tenant", command.tenantCode(), command.loginId(), created);
	}

	private String inConnection(SqlWork work) {
		try (Connection connection = dataSource.getConnection()) {
			connection.setAutoCommit(false);
			try {
				String line = work.apply(connection);
				connection.commit();
				return line;
			}
			catch (RuntimeException | SQLException ex) {
				rollback(connection);
				if (ex instanceof IllegalStateException illegal) {
					throw illegal;
				}
				throw new IllegalStateException("개발용 관리자 계정을 바꾸지 못했습니다.");
			}
			finally {
				resetSearchPath(connection);
			}
		}
		catch (SQLException ex) {
			throw new IllegalStateException("개발용 관리자 계정을 바꾸지 못했습니다.");
		}
	}

	private static Long findPlatformAdmin(Connection connection, String loginId) throws SQLException {
		try (PreparedStatement statement = connection.prepareStatement(
				"SELECT platform_admin_pk FROM pmis.platform_admin WHERE login_id = ?")) {
			statement.setString(1, loginId);
			try (ResultSet resultSet = statement.executeQuery()) {
				if (!resultSet.next()) {
					return null;
				}
				return resultSet.getLong(1);
			}
		}
	}

	private static long insertPlatformAdmin(Connection connection, String loginId, String hash) throws SQLException {
		try (PreparedStatement statement = connection.prepareStatement("""
				INSERT INTO pmis.platform_admin (
				    login_id, admin_name, email, password_hash, must_change_password_yn, admin_status_code,
				    password_expire_datetime, login_fail_count, locked_datetime, use_yn)
				VALUES (?, ?, ?, ?, 'N', 'ACTIVE', NULL, 0, NULL, 'Y')
				RETURNING platform_admin_pk
				""")) {
			statement.setString(1, loginId);
			statement.setString(2, loginId);
			statement.setString(3, loginId);
			statement.setString(4, hash);
			try (ResultSet resultSet = statement.executeQuery()) {
				resultSet.next();
				return resultSet.getLong(1);
			}
		}
	}

	private static void updatePlatformAdmin(Connection connection, long adminPk, String hash) throws SQLException {
		try (PreparedStatement statement = connection.prepareStatement("""
				UPDATE pmis.platform_admin
				SET password_hash = ?,
				    password_changed_datetime = CURRENT_TIMESTAMP,
				    password_expire_datetime = NULL,
				    must_change_password_yn = 'N',
				    admin_status_code = 'ACTIVE',
				    login_fail_count = 0,
				    locked_datetime = NULL,
				    use_yn = 'Y',
				    update_datetime = CURRENT_TIMESTAMP
				WHERE platform_admin_pk = ?
				""")) {
			statement.setString(1, hash);
			statement.setLong(2, adminPk);
			if (statement.executeUpdate() != 1) {
				throw new IllegalStateException("플랫폼 운영자 계정을 바꾸지 못했습니다.");
			}
		}
	}

	private static void revokePlatformRefresh(Connection connection, long adminPk) throws SQLException {
		try (PreparedStatement statement = connection.prepareStatement("""
				UPDATE pmis.platform_admin_refresh_token
				SET revoked_datetime = CURRENT_TIMESTAMP, revoke_reason_code = 'ADMIN'
				WHERE platform_admin_pk = ? AND revoked_datetime IS NULL
				""")) {
			statement.setLong(1, adminPk);
			statement.executeUpdate();
		}
	}

	private static Long findUser(Connection connection, String loginId) throws SQLException {
		try (PreparedStatement statement = connection.prepareStatement(
				"SELECT user_pk FROM user_account WHERE login_id = ?")) {
			statement.setString(1, loginId);
			try (ResultSet resultSet = statement.executeQuery()) {
				if (!resultSet.next()) {
					return null;
				}
				return resultSet.getLong(1);
			}
		}
	}

	private static long insertUser(Connection connection, String loginId, String hash, String languageCode, String timezoneId)
			throws SQLException {
		try (PreparedStatement statement = connection.prepareStatement("""
				INSERT INTO user_account (
				    person_pk, account_type_code, login_id, password_hash,
				    password_changed_datetime, password_expire_datetime, must_change_password_yn,
				    user_status_code, login_fail_count, locked_datetime,
				    support_language_code, timezone_id, use_yn, create_user_pk)
				VALUES (NULL, 'NORMAL', ?, ?, CURRENT_TIMESTAMP, NULL, 'N', 'ACTIVE', 0, NULL, ?, ?, 'Y', NULL)
				RETURNING user_pk
				""")) {
			statement.setString(1, loginId);
			statement.setString(2, hash);
			statement.setString(3, languageCode);
			statement.setString(4, timezoneId);
			try (ResultSet resultSet = statement.executeQuery()) {
				resultSet.next();
				return resultSet.getLong(1);
			}
		}
	}

	private static void updateUser(Connection connection, long userPk, String hash) throws SQLException {
		try (PreparedStatement statement = connection.prepareStatement("""
				UPDATE user_account
				SET password_hash = ?,
				    password_changed_datetime = CURRENT_TIMESTAMP,
				    password_expire_datetime = NULL,
				    must_change_password_yn = 'N',
				    user_status_code = 'ACTIVE',
				    login_fail_count = 0,
				    locked_datetime = NULL,
				    use_yn = 'Y',
				    update_datetime = CURRENT_TIMESTAMP
				WHERE user_pk = ?
				""")) {
			statement.setString(1, hash);
			statement.setLong(2, userPk);
			if (statement.executeUpdate() != 1) {
				throw new IllegalStateException("테넌트 계정을 바꾸지 못했습니다.");
			}
		}
	}

	private static long requireTenantAdminRole(Connection connection) throws SQLException {
		try (PreparedStatement statement = connection.prepareStatement(
				"SELECT role_pk FROM role WHERE role_code = 'TENANT_ADMIN' AND use_yn = 'Y'")) {
			try (ResultSet resultSet = statement.executeQuery()) {
				if (!resultSet.next()) {
					throw new IllegalStateException("TENANT_ADMIN 역할이 없습니다.");
				}
				return resultSet.getLong(1);
			}
		}
	}

	private static void grantTenantAdmin(Connection connection, long userPk, long rolePk) throws SQLException {
		Long userRolePk = null;
		LocalDate validFrom = null;
		LocalDate validTo = null;
		try (PreparedStatement statement = connection.prepareStatement("""
				SELECT user_role_pk, valid_from_date, valid_to_date
				FROM user_role
				WHERE user_pk = ? AND role_pk = ?
				""")) {
			statement.setLong(1, userPk);
			statement.setLong(2, rolePk);
			try (ResultSet resultSet = statement.executeQuery()) {
				if (resultSet.next()) {
					userRolePk = resultSet.getLong(1);
					validFrom = resultSet.getObject(2, LocalDate.class);
					validTo = resultSet.getObject(3, LocalDate.class);
				}
			}
		}
		LocalDate today = LocalDate.now();
		if (validFrom != null && validFrom.isAfter(today)) {
			validFrom = null;
		}
		if (validTo != null && validTo.isBefore(today)) {
			validTo = null;
		}
		if (userRolePk == null) {
			try (PreparedStatement statement = connection.prepareStatement("""
					INSERT INTO user_role (user_pk, role_pk, valid_from_date, valid_to_date, use_yn, create_user_pk)
					VALUES (?, ?, ?, ?, 'Y', NULL)
					""")) {
				statement.setLong(1, userPk);
				statement.setLong(2, rolePk);
				statement.setObject(3, validFrom);
				statement.setObject(4, validTo);
				statement.executeUpdate();
			}
			return;
		}
		try (PreparedStatement statement = connection.prepareStatement("""
				UPDATE user_role
				SET use_yn = 'Y', valid_from_date = ?, valid_to_date = ?, update_datetime = CURRENT_TIMESTAMP
				WHERE user_role_pk = ?
				""")) {
			statement.setObject(1, validFrom);
			statement.setObject(2, validTo);
			statement.setLong(3, userRolePk);
			if (statement.executeUpdate() != 1) {
				throw new IllegalStateException("TENANT_ADMIN 역할을 부여하지 못했습니다.");
			}
		}
	}

	private static void insertHistory(Connection connection, long userPk, String hash) throws SQLException {
		try (PreparedStatement statement = connection.prepareStatement("""
				INSERT INTO user_password_history (
				    user_pk, password_hash, change_type_code, changed_datetime, changed_user_pk)
				VALUES (?, ?, 'RESET', CURRENT_TIMESTAMP, NULL)
				""")) {
			statement.setLong(1, userPk);
			statement.setString(2, hash);
			statement.executeUpdate();
		}
	}

	private static void revokeUserRefresh(Connection connection, long userPk) throws SQLException {
		try (PreparedStatement statement = connection.prepareStatement("""
				UPDATE user_refresh_token
				SET revoked_datetime = CURRENT_TIMESTAMP, revoke_reason_code = 'ADMIN'
				WHERE user_pk = ? AND revoked_datetime IS NULL
				""")) {
			statement.setLong(1, userPk);
			statement.executeUpdate();
		}
	}

	private static void setSearchPath(Connection connection, String schemaName) throws SQLException {
		SchemaNames.requireTenantSchema(schemaName);
		try (Statement statement = connection.createStatement()) {
			statement.execute("SET search_path TO \"" + schemaName + "\"");
		}
	}

	private static void resetSearchPath(Connection connection) {
		try (Statement statement = connection.createStatement()) {
			statement.execute("RESET search_path");
		}
		catch (SQLException ex) {
			try {
				connection.abort(Runnable::run);
			}
			catch (SQLException ignored) {
				// 반납에 실패하면 풀이 이 연결을 다시 쓰지 못한다.
			}
		}
	}

	private static void rollback(Connection connection) {
		try {
			connection.rollback();
		}
		catch (SQLException ex) {
			try {
				connection.abort(Runnable::run);
			}
			catch (SQLException ignored) {
				// 반납에 실패하면 풀이 이 연결을 다시 쓰지 못한다.
			}
		}
	}

	@FunctionalInterface
	private interface SqlWork {
		String apply(Connection connection) throws SQLException;
	}

	private record TenantRow(String schemaName, String languageCode, String timezoneId) {
	}

}
