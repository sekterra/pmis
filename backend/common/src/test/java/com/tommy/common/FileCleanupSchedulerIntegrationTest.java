package com.tommy.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.tommy.common.file.FileCleanupScheduler;
import com.tommy.common.file.FileCleanupScheduler.CleanupSummary;
import com.tommy.common.global.multitenancy.HeaderTenantResolver;
import com.tommy.common.global.security.JwtTokenService;
import com.tommy.common.tenant.provision.ProvisionTenantCommand;
import com.tommy.common.tenant.provision.ProvisioningResult;
import com.tommy.common.tenant.provision.TenantProvisioningService;
import com.tommy.common.tenant.provision.ProvisioningJournal.TenantState;

import tools.jackson.databind.json.JsonMapper;

@Tag("integration")
@SpringBootTest(properties = { "mdm.file.cleanup.enabled=true", "mdm.file.cleanup.cron=0 0 0 1 1 *", "mdm.file.cleanup.zone=Asia/Seoul" })
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class FileCleanupSchedulerIntegrationTest {

	private static final String SCHEMA_PATTERN = "^t_it_[a-z0-9]{16}$";

	private static final Path ROOT = Path.of("build", "it-cleanup-" + UUID.randomUUID().toString().replace("-", ""));

	private static final byte[] PNG = new byte[] { (byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a };

	private static Long realTenantCount;

	@DynamicPropertySource
	static void datasource(DynamicPropertyRegistry registry) {
		TestcontainersConfiguration.registerDatasource(registry);
		TestJwtKey.register(registry);
		registry.add("mdm.file.local-root", ROOT::toString);
	}

	@AfterAll
	static void deleteRoot() throws Exception {
		if (!Files.exists(ROOT)) {
			return;
		}
		try (Stream<Path> walk = Files.walk(ROOT)) {
			walk.sorted(Comparator.reverseOrder()).forEach(path -> {
				try {
					Files.deleteIfExists(path);
				}
				catch (Exception ex) {
					throw new IllegalStateException(ex);
				}
			});
		}
	}

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private TenantProvisioningService provisioningService;

	@Autowired
	private DataSource dataSource;

	@Autowired
	private JwtTokenService jwtTokenService;

	@Autowired
	private ApplicationContext applicationContext;

	@Autowired
	private FileCleanupScheduler scheduler;

	private final List<String> tenantCodes = new ArrayList<>();

	private final Map<String, String> tokens = new HashMap<>();

	private final List<String> schemas = new ArrayList<>();

	@BeforeEach
	void rememberRealTenants() throws Exception {
		Files.createDirectories(ROOT);
		if (realTenantCount == null) {
			try (Connection connection = dataSource.getConnection()) {
				IntegrationDatabase.requirePmis(connection);
				realTenantCount = count(connection, "SELECT count(*) FROM pmis.tenant WHERE tenant_code NOT LIKE 'it-%'");
			}
		}
	}

	@AfterEach
	void cleanup() throws Exception {
		try (Connection connection = dataSource.getConnection()) {
			IntegrationDatabase.requirePmis(connection);
			for (String tenantCode : tenantCodes) {
				deleteTenant(connection, tenantCode);
			}
			for (String schema : schemas) {
				dropSchema(connection, schema);
			}
			assertEquals(realTenantCount, count(connection, "SELECT count(*) FROM pmis.tenant WHERE tenant_code NOT LIKE 'it-%'"));
		}
		deleteRoot();
		Files.createDirectories(ROOT);
	}

	@Test
	void schedulerBeanIsPresentAndOneFailureDoesNotStopTheOtherTenant() throws Exception {
		assertEquals(1, applicationContext.getBeanNamesForType(FileCleanupScheduler.class).length);
		String first = provision();
		String second = provision();
		long firstFile = upload(first, "old.png");
		long secondFile = upload(second, "old.png");
		execute(schema(first), "UPDATE file SET upload_datetime = TIMESTAMP '2000-01-01 00:00:00' WHERE file_pk = " + firstFile);
		execute(schema(second), "UPDATE file SET upload_datetime = TIMESTAMP '2000-01-01 00:00:00' WHERE file_pk = " + secondFile);

		CleanupSummary summary = scheduler.runOnce(List.of(
				new TenantState(0, schema(first), "ACTIVE", first),
				new TenantState(0, "t_it_missing00000001", "ACTIVE", "it-missing"),
				new TenantState(0, schema(second), "ACTIVE", second)));

		assertEquals(3, summary.tenantCount());
		assertEquals(2, summary.successCount());
		assertEquals(1, summary.failureCount());
		assertEquals(2, summary.expiredTempCount());
		assertEquals(0, summary.purgedCount());
		assertEquals("DELETED", scalar(schema(first), "SELECT file_status_code FROM file WHERE file_pk = " + firstFile));
		assertEquals("DELETED", scalar(schema(second), "SELECT file_status_code FROM file WHERE file_pk = " + secondFile));
		assertNull(scalar(schema(first), "SELECT delete_scheduled_datetime::text FROM file WHERE file_pk = " + firstFile));
	}

	private long upload(String tenantCode, String name) throws Exception {
		String body = mockMvc.perform(multipart("/api/mdm/v1/files").file(new MockMultipartFile("file", name, "application/octet-stream", PNG))
				.headers(auth(tenantCode))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		return JsonMapper.builder().build().readTree(body).get("data").get("filePk").asLong();
	}

	private String provision() throws Exception {
		String tenantCode = "it-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
		String schema = schema(tenantCode);
		assertTrue(schema.matches(SCHEMA_PATTERN));
		tenantCodes.add(tenantCode);
		schemas.add(schema);
		ProvisioningResult result = provisioningService.provision(new ProvisionTenantCommand(tenantCode, "아이티", null, "ko-KR", "관리자",
				"admin@example.com", null));
		tokens.put(tenantCode, TestAccess.issueAdmin(jwtTokenService, dataSource, schema, tenantCode));
		if (result.temporaryPassword() == null) {
			throw new IllegalStateException("테넌트 관리자 비밀번호가 없습니다.");
		}
		return tenantCode;
	}

	private HttpHeaders auth(String tenantCode) {
		HttpHeaders headers = new HttpHeaders();
		headers.set(HeaderTenantResolver.HEADER, tenantCode);
		headers.setBearerAuth(tokens.get(tenantCode));
		return headers;
	}

	private static String schema(String tenantCode) {
		return "t_" + tenantCode.replace('-', '_');
	}

	private void execute(String schema, String sql) throws Exception {
		try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
			IntegrationDatabase.requirePmis(connection);
			statement.execute("SET search_path TO " + schema);
			statement.execute(sql);
		}
	}

	private String scalar(String schema, String sql) throws Exception {
		try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
			IntegrationDatabase.requirePmis(connection);
			statement.execute("SET search_path TO " + schema);
			try (ResultSet resultSet = statement.executeQuery(sql)) {
				assertTrue(resultSet.next());
				return resultSet.getString(1);
			}
		}
	}

	private static long count(Connection connection, String sql) throws Exception {
		try (Statement statement = connection.createStatement(); ResultSet resultSet = statement.executeQuery(sql)) {
			assertTrue(resultSet.next());
			return resultSet.getLong(1);
		}
	}

	private static void deleteTenant(Connection connection, String tenantCode) throws Exception {
		Long tenantPk = null;
		try (PreparedStatement statement = connection.prepareStatement("SELECT tenant_pk FROM pmis.tenant WHERE tenant_code = ?")) {
			statement.setString(1, tenantCode);
			try (ResultSet resultSet = statement.executeQuery()) {
				if (resultSet.next()) {
					tenantPk = resultSet.getLong(1);
				}
			}
		}
		if (tenantPk == null) {
			return;
		}
		for (String table : List.of("tenant_schema_migration", "tenant_status_history", "tenant_provisioning", "tenant_domain")) {
			try (PreparedStatement statement = connection.prepareStatement("DELETE FROM pmis." + table + " WHERE tenant_pk = ?")) {
				statement.setLong(1, tenantPk);
				statement.executeUpdate();
			}
		}
		try (PreparedStatement statement = connection.prepareStatement("DELETE FROM pmis.tenant WHERE tenant_pk = ?")) {
			statement.setLong(1, tenantPk);
			statement.executeUpdate();
		}
	}

	private static void dropSchema(Connection connection, String schema) throws Exception {
		if (!schema.matches(SCHEMA_PATTERN)) {
			throw new IllegalArgumentException("테스트 스키마명이 아닙니다.");
		}
		try (Statement statement = connection.createStatement()) {
			statement.execute("DROP SCHEMA IF EXISTS " + schema + " CASCADE");
		}
	}

}
