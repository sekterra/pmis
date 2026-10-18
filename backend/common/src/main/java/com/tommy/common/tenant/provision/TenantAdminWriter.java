package com.tommy.common.tenant.provision;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Locale;

import javax.sql.DataSource;

import org.springframework.stereotype.Component;

import com.tommy.common.global.exception.ApiException;
import com.tommy.common.global.security.AccountStatusCache;
import com.tommy.common.global.security.PasswordHasher;

@Component
public class TenantAdminWriter {

	private final DataSource dataSource;
	private final PasswordHasher passwordHasher;
	private final AccountStatusCache accountStatusCache;

	public TenantAdminWriter(DataSource dataSource, PasswordHasher passwordHasher, AccountStatusCache accountStatusCache) {
		this.dataSource = dataSource;
		this.passwordHasher = passwordHasher;
		this.accountStatusCache = accountStatusCache;
	}

	public void createInitialAdmin(ProvisioningWork work) {
		TenantSchemaWork.run(dataSource, work.schemaName(), connection -> create(connection, work));
	}

	public IssuedAdminPassword reissue(long tenantPk, String schemaName, Long executorPk) {
		IssuedAdminPassword[] created = new IssuedAdminPassword[1];
		long[] userPk = new long[1];
		TenantSchemaWork.run(dataSource, schemaName, connection -> created[0] = replacePassword(connection, tenantPk, executorPk, userPk));
		accountStatusCache.evict(schemaName, userPk[0]);
		return created[0];
	}

	public String storedAdminLoginId(String schemaName) {
		String[] loginId = new String[1];
		TenantSchemaWork.run(dataSource, schemaName, connection -> loginId[0] = selectStoredAdminLoginId(connection));
		return loginId[0];
	}

	static CreationPlan plan(Long existingUserPk, TemporaryPassword generated) {
		if (existingUserPk != null) {
			return new CreationPlan(true, null);
		}
		return new CreationPlan(false, generated);
	}

	private void create(Connection connection, ProvisioningWork work) throws SQLException {
		TenantProfile profile = loadProfile(connection, work.tenantPk());
		String loginId = loginId(profile.adminEmail());
		Long existingUserPk = findUserPk(connection, loginId);
		CreationPlan plan = plan(existingUserPk, null);
		if (plan.alreadyPresent()) {
			work.setAdminAlreadyPresent(true);
			work.setAdminLoginId(readLoginId(connection, existingUserPk));
			return;
		}
		int validDays = SecurityPolicyValues.positiveDays(readValidDays(connection));
		long rolePk = requireTenantAdminRole(connection);
		TemporaryPassword password = TemporaryPassword.generate();
		String hash = passwordHasher.hash(password.reveal());
		long companyPk = insertCompany(connection, profile);
		long personPk = insertPerson(connection, profile);
		insertAssignment(connection, personPk, companyPk);
		long userPk = insertUser(connection, personPk, loginId, hash, validDays, profile);
		insertPasswordHistory(connection, userPk, hash, "INITIAL", null);
		insertUserRole(connection, userPk, rolePk);
		work.setTemporaryPassword(password);
		work.setAdminLoginId(readLoginId(connection, userPk));
	}

	private IssuedAdminPassword replacePassword(Connection connection, long tenantPk, Long executorPk, long[] userPkOut)
			throws SQLException {
		int validDays = SecurityPolicyValues.positiveDays(readValidDays(connection));
		String loginId = loginId(loadAdminEmail(connection, tenantPk));
		AdminAccount account = findAdminAccount(connection, loginId);
		if (account == null) {
			throw new ApiException(404, "TENANT.NOT_FOUND", "최초 관리자 계정이 없습니다.", 2);
		}
		long userPk = account.userPk();
		userPkOut[0] = userPk;
		TemporaryPassword password = TemporaryPassword.generate();
		String hash = passwordHasher.hash(password.reveal());
		try (PreparedStatement statement = connection.prepareStatement("""
				UPDATE user_account
				SET password_hash = ?,
				    password_changed_datetime = CURRENT_TIMESTAMP,
				    password_expire_datetime = CURRENT_TIMESTAMP + (? * INTERVAL '1 day'),
				    must_change_password_yn = 'Y',
				    user_status_code = 'ACTIVE',
				    login_fail_count = 0,
				    locked_datetime = NULL,
				    update_datetime = CURRENT_TIMESTAMP,
				    update_user_pk = ?
				WHERE user_pk = ?
				""")) {
			statement.setString(1, hash);
			statement.setInt(2, validDays);
			if (executorPk == null) {
				statement.setNull(3, Types.BIGINT);
			}
			else {
				statement.setLong(3, executorPk);
			}
			statement.setLong(4, userPk);
			if (statement.executeUpdate() != 1) {
				throw new ApiException(404, "TENANT.NOT_FOUND", "최초 관리자 계정이 없습니다.", 2);
			}
		}
		insertPasswordHistory(connection, userPk, hash, "RESET", executorPk);
		revokeRefreshTokens(connection, userPk, "ADMIN");
		return new IssuedAdminPassword(password, readLoginId(connection, userPk));
	}

	private static TenantProfile loadProfile(Connection connection, long tenantPk) throws SQLException {
		try (PreparedStatement statement = connection.prepareStatement("""
				SELECT t.tenant_code, t.tenant_name, t.default_language_code, t.timezone_id,
				       p.admin_name, p.admin_email
				FROM pmis.tenant t
				JOIN pmis.tenant_provisioning p ON p.tenant_pk = t.tenant_pk
				WHERE t.tenant_pk = ?
				ORDER BY p.provisioning_pk DESC
				LIMIT 1
				""")) {
			statement.setLong(1, tenantPk);
			try (ResultSet resultSet = statement.executeQuery()) {
				if (!resultSet.next()) {
					throw new ApiException(404, "TENANT.NOT_FOUND", "테넌트를 찾을 수 없습니다.", 2);
				}
				return new TenantProfile(
						resultSet.getString("tenant_code"),
						resultSet.getString("tenant_name"),
						resultSet.getString("default_language_code"),
						resultSet.getString("timezone_id"),
						resultSet.getString("admin_name"),
						resultSet.getString("admin_email"));
			}
		}
	}

	private static String loadAdminEmail(Connection connection, long tenantPk) throws SQLException {
		try (PreparedStatement statement = connection.prepareStatement("""
				SELECT admin_email
				FROM pmis.tenant_provisioning
				WHERE tenant_pk = ?
				ORDER BY provisioning_pk DESC
				LIMIT 1
				""")) {
			statement.setLong(1, tenantPk);
			try (ResultSet resultSet = statement.executeQuery()) {
				if (!resultSet.next()) {
					throw new ApiException(404, "TENANT.NOT_FOUND", "프로비저닝 기록이 없습니다.", 2);
				}
				return resultSet.getString(1);
			}
		}
	}

	private static String readValidDays(Connection connection) throws SQLException {
		try (PreparedStatement statement = connection.prepareStatement("""
				SELECT c.ext_attr1
				FROM common_code c
				JOIN code_group g ON g.code_group_pk = c.code_group_pk
				WHERE g.code_group_code = ?
				  AND c.code_value = ?
				  AND g.use_yn = 'Y'
				  AND c.use_yn = 'Y'
				""")) {
			statement.setString(1, SecurityPolicyValues.GROUP);
			statement.setString(2, SecurityPolicyValues.TEMP_PASSWORD_VALID_DAYS);
			try (ResultSet resultSet = statement.executeQuery()) {
				if (!resultSet.next()) {
					return null;
				}
				return resultSet.getString(1);
			}
		}
	}

	private static long requireTenantAdminRole(Connection connection) throws SQLException {
		try (PreparedStatement statement = connection.prepareStatement("""
				SELECT role_pk FROM role WHERE role_code = 'TENANT_ADMIN' AND use_yn = 'Y'
				""")) {
			try (ResultSet resultSet = statement.executeQuery()) {
				if (!resultSet.next()) {
					throw new ApiException(500, "TENANT.PROVISIONING_FAILED", "TENANT_ADMIN 역할이 없습니다.", 2);
				}
				return resultSet.getLong(1);
			}
		}
	}

	private static Long findUserPk(Connection connection, String loginId) throws SQLException {
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

	private static AdminAccount findAdminAccount(Connection connection, String loginId) throws SQLException {
		try (PreparedStatement statement = connection.prepareStatement("""
				SELECT u.user_pk
				FROM user_account u
				JOIN user_role ur ON ur.user_pk = u.user_pk AND ur.use_yn = 'Y'
				JOIN role r ON r.role_pk = ur.role_pk AND r.role_code = 'TENANT_ADMIN' AND r.use_yn = 'Y'
				WHERE u.login_id = ?
				""")) {
			statement.setString(1, loginId);
			try (ResultSet resultSet = statement.executeQuery()) {
				if (!resultSet.next()) {
					return null;
				}
				return new AdminAccount(resultSet.getLong(1));
			}
		}
	}

	private static String readLoginId(Connection connection, long userPk) throws SQLException {
		try (PreparedStatement statement = connection.prepareStatement(
				"SELECT login_id FROM user_account WHERE user_pk = ?")) {
			statement.setLong(1, userPk);
			try (ResultSet resultSet = statement.executeQuery()) {
				if (!resultSet.next()) {
					throw new ApiException(404, "TENANT.NOT_FOUND", "최초 관리자 계정이 없습니다.", 2);
				}
				return resultSet.getString(1);
			}
		}
	}

	private static String selectStoredAdminLoginId(Connection connection) throws SQLException {
		try (PreparedStatement statement = connection.prepareStatement("""
				SELECT u.login_id
				FROM user_account u
				JOIN user_role ur ON ur.user_pk = u.user_pk AND ur.use_yn = 'Y'
				JOIN role r ON r.role_pk = ur.role_pk AND r.role_code = 'TENANT_ADMIN' AND r.use_yn = 'Y'
				ORDER BY u.user_pk
				LIMIT 1
				""")) {
			try (ResultSet resultSet = statement.executeQuery()) {
				if (!resultSet.next()) {
					return null;
				}
				return resultSet.getString(1);
			}
		}
	}

	private static long insertCompany(Connection connection, TenantProfile profile) throws SQLException {
		try (PreparedStatement statement = connection.prepareStatement("""
				INSERT INTO company (company_code, company_name, company_type_code, create_user_pk)
				VALUES (?, ?, 'OWN', NULL)
				RETURNING company_pk
				""")) {
			statement.setString(1, profile.tenantCode());
			statement.setString(2, profile.tenantName());
			try (ResultSet resultSet = statement.executeQuery()) {
				resultSet.next();
				return resultSet.getLong(1);
			}
		}
	}

	private static long insertPerson(Connection connection, TenantProfile profile) throws SQLException {
		try (PreparedStatement statement = connection.prepareStatement("""
				INSERT INTO person (person_name, email, create_user_pk)
				VALUES (?, ?, NULL)
				RETURNING person_pk
				""")) {
			statement.setString(1, profile.adminName());
			statement.setString(2, profile.adminEmail());
			try (ResultSet resultSet = statement.executeQuery()) {
				resultSet.next();
				return resultSet.getLong(1);
			}
		}
	}

	private static void insertAssignment(Connection connection, long personPk, long companyPk) throws SQLException {
		try (PreparedStatement statement = connection.prepareStatement("""
				INSERT INTO person_assignment (
				    person_pk, company_pk, employment_type_code, assignment_status_code,
				    primary_yn, dept_head_yn, start_date, create_user_pk)
				VALUES (?, ?, 'REGULAR', 'ACTIVE', 'Y', 'N', CURRENT_DATE, NULL)
				""")) {
			statement.setLong(1, personPk);
			statement.setLong(2, companyPk);
			statement.executeUpdate();
		}
	}

	private static long insertUser(Connection connection, long personPk, String loginId, String hash, int validDays,
			TenantProfile profile) throws SQLException {
		try (PreparedStatement statement = connection.prepareStatement("""
				INSERT INTO user_account (
				    person_pk, account_type_code, login_id, password_hash,
				    password_changed_datetime, password_expire_datetime, must_change_password_yn,
				    user_status_code, support_language_code, timezone_id, create_user_pk)
				VALUES (?, 'NORMAL', ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + (? * INTERVAL '1 day'), 'Y',
				        'ACTIVE', ?, ?, NULL)
				RETURNING user_pk
				""")) {
			statement.setLong(1, personPk);
			statement.setString(2, loginId);
			statement.setString(3, hash);
			statement.setInt(4, validDays);
			statement.setString(5, profile.defaultLanguageCode());
			statement.setString(6, profile.timezoneId());
			try (ResultSet resultSet = statement.executeQuery()) {
				resultSet.next();
				return resultSet.getLong(1);
			}
		}
	}

	private static void revokeRefreshTokens(Connection connection, long userPk, String reason) throws SQLException {
		try (PreparedStatement statement = connection.prepareStatement("""
				UPDATE user_refresh_token
				SET revoked_datetime = CURRENT_TIMESTAMP, revoke_reason_code = ?
				WHERE user_pk = ? AND revoked_datetime IS NULL
				""")) {
			statement.setString(1, reason);
			statement.setLong(2, userPk);
			statement.executeUpdate();
		}
	}

	private static void insertPasswordHistory(Connection connection, long userPk, String hash, String changeType,
			Long changedUserPk) throws SQLException {
		try (PreparedStatement statement = connection.prepareStatement("""
				INSERT INTO user_password_history (
				    user_pk, password_hash, change_type_code, changed_datetime, changed_user_pk)
				VALUES (?, ?, ?, CURRENT_TIMESTAMP, ?)
				""")) {
			statement.setLong(1, userPk);
			statement.setString(2, hash);
			statement.setString(3, changeType);
			if (changedUserPk == null) {
				statement.setNull(4, Types.BIGINT);
			}
			else {
				statement.setLong(4, changedUserPk);
			}
			statement.executeUpdate();
		}
	}

	private static void insertUserRole(Connection connection, long userPk, long rolePk) throws SQLException {
		try (PreparedStatement statement = connection.prepareStatement("""
				INSERT INTO user_role (user_pk, role_pk, use_yn, create_user_pk)
				VALUES (?, ?, 'Y', NULL)
				""")) {
			statement.setLong(1, userPk);
			statement.setLong(2, rolePk);
			statement.executeUpdate();
		}
	}

	private static String loginId(String adminEmail) {
		if (adminEmail == null || adminEmail.isBlank()) {
			throw new ApiException(400, "COMMON.VALIDATION_FAILED", "관리자 이메일이 없습니다.", 2);
		}
		String loginId = adminEmail.trim().toLowerCase(Locale.ROOT);
		if (loginId.length() > 100) {
			throw new ApiException(400, "COMMON.VALIDATION_FAILED", "관리자 이메일이 너무 깁니다.", 2);
		}
		return loginId;
	}

	record CreationPlan(boolean alreadyPresent, TemporaryPassword temporaryPassword) {
	}

	public record IssuedAdminPassword(TemporaryPassword temporaryPassword, String adminLoginId) {
	}

	private record AdminAccount(long userPk) {
	}

	private record TenantProfile(String tenantCode, String tenantName, String defaultLanguageCode, String timezoneId,
			String adminName, String adminEmail) {
	}

}
