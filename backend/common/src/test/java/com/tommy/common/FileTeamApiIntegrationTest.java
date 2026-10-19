package com.tommy.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.tommy.common.access.entity.Permission;
import com.tommy.common.access.entity.Role;
import com.tommy.common.access.entity.RolePermission;
import com.tommy.common.access.entity.UserAccount;
import com.tommy.common.access.entity.UserPasswordHistory;
import com.tommy.common.access.entity.UserRole;
import com.tommy.common.file.entity.FileAttachment;
import com.tommy.common.file.entity.FileStorage;
import com.tommy.common.file.entity.StoredFile;
import com.tommy.common.global.multitenancy.HeaderTenantResolver;
import com.tommy.common.global.security.JwtTokenService;
import com.tommy.common.i18n.entity.Nation;
import com.tommy.common.i18n.entity.SupportLanguage;
import com.tommy.common.menu.entity.Menu;
import com.tommy.common.org.entity.Company;
import com.tommy.common.org.entity.Dept;
import com.tommy.common.org.entity.DeptHistory;
import com.tommy.common.org.entity.Person;
import com.tommy.common.org.entity.PersonAssignment;
import com.tommy.common.team.entity.Team;
import com.tommy.common.team.entity.TeamMember;
import com.tommy.common.tenant.provision.ProvisionTenantCommand;
import com.tommy.common.tenant.provision.ProvisioningResult;
import com.tommy.common.tenant.provision.TenantProvisioningService;
import com.tommy.common.tenant.provision.TenantUpgradeService;

import jakarta.persistence.EntityManager;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class FileTeamApiIntegrationTest {

	private static final String SCHEMA_PATTERN = "^t_it_[a-z0-9]{16}$";

	private static final Path ROOT = Path.of("build", "it-files-" + UUID.randomUUID().toString().replace("-", ""));

	private static final byte[] PNG = new byte[] { (byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 0x00 };

	private static final byte[] PDF = new byte[] { '%', 'P', 'D', 'F', '-', '1' };

	private static Long realTenantCount;

	@DynamicPropertySource
	static void datasource(DynamicPropertyRegistry registry) {
		TestcontainersConfiguration.registerDatasource(registry);
		TestJwtKey.register(registry);
		registry.add("mdm.file.local-root", ROOT::toString);
	}

	@AfterAll
	static void deleteRoot() throws Exception {
		if (Files.exists(ROOT)) {
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
	}

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private TenantProvisioningService provisioningService;

	@Autowired
	private TenantUpgradeService upgradeService;

	@Autowired
	private DataSource dataSource;

	@Autowired
	private JwtTokenService jwtTokenService;

	@Autowired
	private EntityManager entityManager;

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
	void schemaSeedAndFileTeamFlows() throws Exception {
		String tenant = provision();
		String other = provision();
		validateSchema(schema(tenant));
		HttpHeaders admin = auth(tenant);
		assertEquals("1", scalar(schema(tenant), "SELECT count(*)::text FROM menu WHERE menu_code = 'FILE_STORAGE' AND use_yn = 'Y'"));
		assertEquals("0", scalar(schema(tenant), """
				SELECT count(*)::text FROM role_permission rp
				JOIN role r ON r.role_pk = rp.role_pk
				JOIN permission p ON p.permission_pk = rp.permission_pk
				WHERE r.role_code IN ('MANAGER', 'USER') AND p.permission_code LIKE 'FILE_STORAGE:%' AND rp.use_yn = 'Y'
				"""));

		mockMvc.perform(get("/api/mdm/v1/files").header(HeaderTenantResolver.HEADER, tenant)).andExpect(status().isUnauthorized());
		mockMvc.perform(post("/api/mdm/v1/file-storages").headers(admin).contentType(MediaType.APPLICATION_JSON)
				.content("{\"storageCode\":\"BAD\",\"storageName\":\"나쁨\",\"storageTypeCode\":\"LOCAL\",\"basePath\":\"../out\"}"))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.errorCode").value("FILE.INVALID_PATH"));
		mockMvc.perform(post("/api/mdm/v1/file-storages").headers(admin).contentType(MediaType.APPLICATION_JSON)
				.content("{\"storageCode\":\"SECRET\",\"storageName\":\"비밀\",\"storageTypeCode\":\"LOCAL\",\"basePath\":\".\",\"connectionConfig\":\"{\\\"apiPassword\\\":\\\"x\\\"}\"}"))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.errorCode").value("FILE.SECRET_IN_CONFIG"));
		mockMvc.perform(post("/api/mdm/v1/file-storages").headers(admin).contentType(MediaType.APPLICATION_JSON)
				.content("{\"storageCode\":\"CLOUD\",\"storageName\":\"클라우드\",\"storageTypeCode\":\"S3\",\"basePath\":\"bucket/docs\",\"connectionConfig\":\"{\\\"region\\\":\\\"local\\\"}\"}"))
				.andExpect(status().isOk());

		SetBefore before = filesNow();
		MvcResult uploaded = mockMvc.perform(multipart("/api/mdm/v1/files").file(file("photo.png", PNG)).headers(admin))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.fileStatusCode").value("TEMP"))
				.andExpect(jsonPath("$.data.checksumSha256").value(sha256(PNG)))
				.andReturn();
		String uploadBody = uploaded.getResponse().getContentAsString();
		assertFalse(uploadBody.contains("storageKey"));
		assertFalse(uploadBody.contains(ROOT.toString().replace("\\", "/")));
		assertFalse(uploadBody.contains("basePath"));
		long filePk = json(uploaded).get("data").get("filePk").asLong();
		Path stored = newest(before);
		assertTrue(stored.toString().contains(tenant));
		mockMvc.perform(get("/api/mdm/v1/files/" + filePk + "/download").headers(admin).header("Range", "bytes=0-1"))
				.andExpect(status().isOk())
				.andExpect(header().string("X-Content-Type-Options", "nosniff"))
				.andExpect(header().string("Cache-Control", "private, no-store"))
				.andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("filename*=UTF-8''")));
		assertEquals(PNG.length, mockMvc.perform(get("/api/mdm/v1/files/" + filePk + "/download").headers(admin)).andReturn().getResponse()
				.getContentAsByteArray().length);

		mockMvc.perform(multipart("/api/mdm/v1/files").file(file("note.exe", new byte[] { 1 })).param("storageCode", "DEFAULT").headers(admin))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.errorCode").value("FILE.EXTENSION_NOT_ALLOWED"));
		mockMvc.perform(multipart("/api/mdm/v1/files").file(file("a.exe.png", PNG)).param("storageCode", "DEFAULT").headers(admin))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.errorCode").value("FILE.EXTENSION_NOT_ALLOWED"));
		mockMvc.perform(multipart("/api/mdm/v1/files").file(file("bad.png", PDF)).param("storageCode", "DEFAULT").headers(admin))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.errorCode").value("FILE.MIME_MISMATCH"));
		mockMvc.perform(multipart("/api/mdm/v1/files").file(file("big.png", new byte[1024 * 1024 + 8])).param("storageCode", "CLOUD").headers(admin))
				.andExpect(status().is(501)).andExpect(jsonPath("$.errorCode").value("FILE.STORAGE_TYPE_UNSUPPORTED"));

		long limitedRole = pk(mockMvc.perform(post("/api/mdm/v1/roles").headers(admin).contentType(MediaType.APPLICATION_JSON)
				.content("{\"roleCode\":\"PERSON_ONLY\",\"roleName\":\"사람조회\"}")).andExpect(status().isOk()).andReturn(), "rolePk");
		mockMvc.perform(put("/api/mdm/v1/roles/" + limitedRole + "/permissions").headers(admin).contentType(MediaType.APPLICATION_JSON)
				.content("{\"permissionCodes\":[\"PERSON:READ\"]}")).andExpect(status().isOk());
		long person = pk(mockMvc.perform(post("/api/mdm/v1/people").headers(admin).contentType(MediaType.APPLICATION_JSON)
				.content("{\"personName\":\"첨부대상\"}")).andExpect(status().isOk()).andReturn(), "personPk");
		MvcResult reader = mockMvc.perform(post("/api/mdm/v1/users").headers(admin).contentType(MediaType.APPLICATION_JSON)
				.content("{\"loginId\":\"files.reader@example.com\",\"personPk\":" + person + ",\"roleCodes\":[\"PERSON_ONLY\"]}"))
				.andExpect(status().isOk()).andReturn();
		long readerPk = json(reader).get("data").get("userPk").asLong();
		HttpHeaders readerHeaders = bearer(tenant, readerPk);
		execute(schema(tenant), "UPDATE file SET upload_user_pk = " + readerPk + " WHERE file_pk = " + filePk);
		mockMvc.perform(get("/api/mdm/v1/files").headers(readerHeaders)).andExpect(status().isForbidden());
		mockMvc.perform(get("/api/mdm/v1/files/" + filePk + "/download").headers(readerHeaders)).andExpect(status().isOk());
		long adminPk = Long.parseLong(scalar(schema(tenant), "SELECT user_pk::text FROM user_account WHERE login_id = 'admin@example.com'"));
		execute(schema(tenant), "UPDATE file SET upload_user_pk = " + adminPk + " WHERE file_pk = " + filePk);

		mockMvc.perform(post("/api/mdm/v1/attachments").headers(readerHeaders).contentType(MediaType.APPLICATION_JSON)
				.content("{\"fileId\":" + filePk + ",\"targetTableName\":\"person\",\"targetKey\":\"" + person + "\"}"))
				.andExpect(status().isForbidden());
		mockMvc.perform(post("/api/mdm/v1/attachments").headers(admin).contentType(MediaType.APPLICATION_JSON)
				.content("{\"fileId\":" + filePk + ",\"targetTableName\":\"invoice\",\"targetKey\":\"1\"}"))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.errorCode").value("FILE.TARGET_NOT_ALLOWED"));
		MvcResult attachment = mockMvc.perform(post("/api/mdm/v1/attachments").headers(admin).contentType(MediaType.APPLICATION_JSON)
				.content("{\"fileId\":" + filePk + ",\"targetTableName\":\"person\",\"targetKey\":\"" + person + "\",\"sortOrder\":1}"))
				.andExpect(status().isOk()).andReturn();
		long attachmentPk = json(attachment).get("data").get("attachmentPk").asLong();
		assertEquals("ACTIVE", scalar(schema(tenant), "SELECT file_status_code FROM file WHERE file_pk = " + filePk));
		mockMvc.perform(post("/api/mdm/v1/attachments").headers(admin).contentType(MediaType.APPLICATION_JSON)
				.content("{\"fileId\":" + filePk + ",\"targetTableName\":\"person\",\"targetKey\":\"" + person + "\"}"))
				.andExpect(status().isConflict()).andExpect(jsonPath("$.errorCode").value("FILE.ATTACHMENT_DUPLICATE"));
		mockMvc.perform(get("/api/mdm/v1/attachments/" + attachmentPk + "/download").headers(readerHeaders)).andExpect(status().isOk());
		mockMvc.perform(delete("/api/mdm/v1/files/" + filePk).headers(admin)).andExpect(status().isConflict())
				.andExpect(jsonPath("$.errorCode").value("FILE.IN_USE"));
		mockMvc.perform(delete("/api/mdm/v1/attachments/" + attachmentPk).headers(admin)).andExpect(status().isOk());
		assertEquals("ACTIVE", scalar(schema(tenant), "SELECT file_status_code FROM file WHERE file_pk = " + filePk));
		assertEquals("N", scalar(schema(tenant), "SELECT use_yn FROM file_attachment WHERE attachment_pk = " + attachmentPk));
		MvcResult revived = mockMvc.perform(post("/api/mdm/v1/attachments").headers(admin).contentType(MediaType.APPLICATION_JSON)
				.content("{\"fileId\":" + filePk + ",\"targetTableName\":\"person\",\"targetKey\":\"" + person + "\"}"))
				.andExpect(status().isOk()).andReturn();
		assertEquals(attachmentPk, json(revived).get("data").get("attachmentPk").asLong());

		long tiny = pk(mockMvc.perform(post("/api/mdm/v1/file-storages").headers(admin).contentType(MediaType.APPLICATION_JSON)
				.content("{\"storageCode\":\"TINY\",\"storageName\":\"작음\",\"storageTypeCode\":\"LOCAL\",\"basePath\":\".\",\"maxFileSizeMb\":1}"))
				.andExpect(status().isOk()).andReturn(), "storagePk");
		mockMvc.perform(multipart("/api/mdm/v1/files").file(file("large.png", new byte[1024 * 1024 + 16])).param("storageCode", "TINY").headers(admin))
				.andExpect(status().is(413)).andExpect(jsonPath("$.errorCode").value("FILE.TOO_LARGE"));
		long defaultPk = Long.parseLong(scalar(schema(tenant), "SELECT storage_pk::text FROM file_storage WHERE storage_code = 'DEFAULT'"));
		mockMvc.perform(delete("/api/mdm/v1/file-storages/" + defaultPk).headers(admin)).andExpect(status().isConflict())
				.andExpect(jsonPath("$.errorCode").value("FILE.STORAGE_IN_USE"));
		mockMvc.perform(put("/api/mdm/v1/file-storages/" + tiny).headers(admin).contentType(MediaType.APPLICATION_JSON)
				.content("{\"storageName\":\"작음\",\"storageTypeCode\":\"LOCAL\",\"basePath\":\".\",\"maxFileSizeMb\":1,\"defaultYn\":\"Y\"}"))
				.andExpect(status().isOk());
		mockMvc.perform(delete("/api/mdm/v1/file-storages/" + tiny).headers(admin)).andExpect(status().isConflict())
				.andExpect(jsonPath("$.errorCode").value("FILE.DEFAULT_STORAGE"));

		MvcResult secondImage = mockMvc.perform(multipart("/api/mdm/v1/files").file(file("next.png", PNG)).headers(admin)).andExpect(status().isOk())
				.andReturn();
		long secondPk = json(secondImage).get("data").get("filePk").asLong();
		mockMvc.perform(put("/api/mdm/v1/people/" + person + "/profile-image").headers(admin).contentType(MediaType.APPLICATION_JSON)
				.content("{\"fileId\":" + secondPk + "}")).andExpect(status().isOk());
		mockMvc.perform(get("/api/mdm/v1/people/" + person).headers(admin)).andExpect(status().isOk())
				.andExpect(jsonPath("$.data.hasProfileImage").value(true));
		mockMvc.perform(get("/api/mdm/v1/people").headers(admin).param("keyword", "첨부대상")).andExpect(status().isOk())
				.andExpect(jsonPath("$.data.items[0].hasProfileImage").value(true));
		Statistics statistics = entityManager.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
		statistics.setStatisticsEnabled(true);
		statistics.clear();
		mockMvc.perform(get("/api/mdm/v1/people").headers(admin).param("page", "1").param("size", "1")).andExpect(status().isOk());
		long one = statistics.getPrepareStatementCount();
		statistics.clear();
		mockMvc.perform(get("/api/mdm/v1/people").headers(admin).param("page", "1").param("size", "2")).andExpect(status().isOk());
		assertEquals(one, statistics.getPrepareStatementCount());
		long pdf = json(mockMvc.perform(multipart("/api/mdm/v1/files").file(file("doc.pdf", PDF)).headers(admin)).andExpect(status().isOk())
				.andReturn()).get("data").get("filePk").asLong();
		mockMvc.perform(put("/api/mdm/v1/people/" + person + "/profile-image").headers(admin).contentType(MediaType.APPLICATION_JSON)
				.content("{\"fileId\":" + pdf + "}")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.errorCode").value("FILE.NOT_IMAGE"));
		mockMvc.perform(delete("/api/mdm/v1/files/" + secondPk).headers(admin)).andExpect(status().isConflict());
		mockMvc.perform(get("/api/mdm/v1/people/" + person + "/profile-image").headers(admin)).andExpect(status().isOk())
				.andExpect(header().string("X-Content-Type-Options", "nosniff"));
		mockMvc.perform(get("/api/mdm/v1/people/" + person + "/profile-image").headers(readerHeaders)).andExpect(status().isOk());
		mockMvc.perform(delete("/api/mdm/v1/people/" + person + "/profile-image").headers(admin)).andExpect(status().isOk());
		mockMvc.perform(get("/api/mdm/v1/people/" + person).headers(admin)).andExpect(jsonPath("$.data.hasProfileImage").value(false));

		SetBefore looseBefore = filesNow();
		long loose = json(mockMvc.perform(multipart("/api/mdm/v1/files").file(file("temp.png", PNG)).headers(admin)).andExpect(status().isOk())
				.andReturn()).get("data").get("filePk").asLong();
		Path loosePath = newest(looseBefore);
		execute(schema(tenant), "UPDATE file SET upload_datetime = CURRENT_TIMESTAMP - INTERVAL '48 hours' WHERE file_pk = " + loose);
		mockMvc.perform(post("/api/mdm/v1/files/cleanup").headers(admin)).andExpect(status().isOk())
				.andExpect(jsonPath("$.data.expiredTempCount").value(org.hamcrest.Matchers.greaterThan(0)));
		assertEquals("DELETED", scalar(schema(tenant), "SELECT file_status_code FROM file WHERE file_pk = " + loose));
		assertFalse(Files.exists(loosePath));
		SetBefore keptBefore = filesNow();
		long kept = json(mockMvc.perform(multipart("/api/mdm/v1/files").file(file("keep.png", PNG)).headers(admin)).andExpect(status().isOk())
				.andReturn()).get("data").get("filePk").asLong();
		Path keptPath = newest(keptBefore);
		mockMvc.perform(delete("/api/mdm/v1/files/" + kept).headers(admin)).andExpect(status().isOk());
		assertTrue(Files.exists(keptPath));
		execute(schema(tenant), "UPDATE file SET delete_scheduled_datetime = TIMESTAMP '2000-01-01 00:00:00' WHERE file_pk = " + kept);
		mockMvc.perform(post("/api/mdm/v1/files/cleanup").headers(admin)).andExpect(status().isOk())
				.andExpect(jsonPath("$.data.diskFailureCount").value(0))
				.andExpect(jsonPath("$.data.purgedCount").value(org.hamcrest.Matchers.greaterThan(0)));
		assertEquals("1", scalar(schema(tenant), "SELECT count(*)::text FROM file WHERE file_pk = " + kept));
		assertFalse(Files.exists(keptPath));
		SetBefore blockedBefore = filesNow();
		long blocked = json(mockMvc.perform(multipart("/api/mdm/v1/files").file(file("block.png", PNG)).headers(admin)).andExpect(status().isOk())
				.andReturn()).get("data").get("filePk").asLong();
		Path blockedPath = newest(blockedBefore);
		Files.delete(blockedPath);
		Files.createDirectories(blockedPath);
		Files.write(blockedPath.resolve("child"), new byte[] { 1 });
		execute(schema(tenant), "UPDATE file SET upload_datetime = CURRENT_TIMESTAMP - INTERVAL '48 hours' WHERE file_pk = " + blocked);
		mockMvc.perform(post("/api/mdm/v1/files/cleanup").headers(admin)).andExpect(status().isOk())
				.andExpect(jsonPath("$.data.diskFailureCount").value(org.hamcrest.Matchers.greaterThan(0)));
		assertEquals("TEMP", scalar(schema(tenant), "SELECT file_status_code FROM file WHERE file_pk = " + blocked));

		mockMvc.perform(post("/api/mdm/v1/teams").headers(admin).contentType(MediaType.APPLICATION_JSON)
				.content("{\"teamCode\":\"bad\",\"teamName\":\"나쁨\",\"teamTypeCode\":\"PROJECT\"}"))
				.andExpect(status().isBadRequest());
		MvcResult team = mockMvc.perform(post("/api/mdm/v1/teams").headers(admin).contentType(MediaType.APPLICATION_JSON).content("""
				{"teamCode":"ALPHA","teamName":"알파","teamTypeCode":"PROJECT","startDate":"2026-01-01","translations":[{"supportLanguageCode":"en-US","text":"Alpha"}]}
				""")).andExpect(status().isOk()).andExpect(jsonPath("$.data.translations[0].text").value("Alpha")).andReturn();
		long teamPk = json(team).get("data").get("teamPk").asLong();
		mockMvc.perform(get("/api/mdm/v1/teams").headers(admin).param("keyword", "ALPHA")).andExpect(status().isOk())
				.andExpect(jsonPath("$.data.items[0].translations").doesNotExist());
		mockMvc.perform(post("/api/mdm/v1/teams").headers(admin).contentType(MediaType.APPLICATION_JSON)
				.content("{\"teamCode\":\"ALPHA\",\"teamName\":\"중복\",\"teamTypeCode\":\"PROJECT\"}"))
				.andExpect(status().isConflict()).andExpect(jsonPath("$.errorCode").value("TEAM.DUPLICATE"));
		mockMvc.perform(put("/api/mdm/v1/teams/" + teamPk).headers(admin).contentType(MediaType.APPLICATION_JSON)
				.content("{\"teamName\":\"알파\",\"teamTypeCode\":\"PROJECT\",\"startDate\":\"2026-02-01\",\"endDate\":\"2026-01-01\"}"))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.errorCode").value("TEAM.INVALID_DATE"));
		MvcResult member = mockMvc.perform(post("/api/mdm/v1/teams/" + teamPk + "/members").headers(admin).contentType(MediaType.APPLICATION_JSON)
				.content("{\"personPk\":" + person + ",\"teamRoleCode\":\"LEADER\",\"startDate\":\"2026-01-01\"}")).andExpect(status().isOk())
				.andExpect(jsonPath("$.data.personName").value("첨부대상")).andReturn();
		long memberPk = json(member).get("data").get("teamMemberPk").asLong();
		mockMvc.perform(get("/api/mdm/v1/teams/" + teamPk).headers(admin)).andExpect(jsonPath("$.data.memberCount").value(1));
		mockMvc.perform(post("/api/mdm/v1/teams/" + teamPk + "/members").headers(admin).contentType(MediaType.APPLICATION_JSON)
				.content("{\"personPk\":" + person + ",\"teamRoleCode\":\"MEMBER\",\"startDate\":\"2026-02-01\"}"))
				.andExpect(status().isConflict()).andExpect(jsonPath("$.errorCode").value("TEAM.MEMBER_DUPLICATE"));
		mockMvc.perform(put("/api/mdm/v1/teams/" + teamPk).headers(admin).contentType(MediaType.APPLICATION_JSON)
				.content("{\"teamName\":\"알파종료\",\"teamTypeCode\":\"PROJECT\",\"startDate\":\"2026-01-01\",\"endDate\":\"" + LocalDate.now().minusDays(1)
						+ "\"}"))
				.andExpect(status().isOk());
		mockMvc.perform(delete("/api/mdm/v1/teams/" + teamPk).headers(admin)).andExpect(status().isConflict())
				.andExpect(jsonPath("$.errorCode").value("TEAM.HAS_ACTIVE_MEMBER"));
		mockMvc.perform(post("/api/mdm/v1/team-members/" + memberPk + "/end").headers(admin).contentType(MediaType.APPLICATION_JSON)
				.content("{\"endDate\":\"2026-03-01\"}")).andExpect(status().isOk());
		mockMvc.perform(post("/api/mdm/v1/team-members/" + memberPk + "/end").headers(admin).contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isConflict()).andExpect(jsonPath("$.errorCode").value("TEAM.MEMBER_ALREADY_ENDED"));
		MvcResult rejoined = mockMvc.perform(post("/api/mdm/v1/teams/" + teamPk + "/members").headers(admin).contentType(MediaType.APPLICATION_JSON)
				.content("{\"personPk\":" + person + ",\"teamRoleCode\":\"MEMBER\",\"startDate\":\"2026-04-01\"}")).andExpect(status().isOk())
				.andReturn();
		assertTrue(json(rejoined).get("data").get("teamMemberPk").asLong() != memberPk);
		mockMvc.perform(delete("/api/mdm/v1/teams/" + teamPk).headers(admin)).andExpect(status().isConflict());

		HttpHeaders otherAdmin = auth(other);
		mockMvc.perform(post("/api/mdm/v1/teams").headers(otherAdmin).contentType(MediaType.APPLICATION_JSON)
				.content("{\"teamCode\":\"ALPHA\",\"teamName\":\"다른\",\"teamTypeCode\":\"ETC\"}")).andExpect(status().isOk());
		mockMvc.perform(post("/api/mdm/v1/file-storages").headers(otherAdmin).contentType(MediaType.APPLICATION_JSON)
				.content("{\"storageCode\":\"TINY\",\"storageName\":\"다른작음\",\"storageTypeCode\":\"LOCAL\",\"basePath\":\".\"}"))
				.andExpect(status().isOk());
		mockMvc.perform(get("/api/mdm/v1/files/" + filePk + "/download").headers(otherAdmin)).andExpect(status().isNotFound());

		execute(schema(tenant), "UPDATE i18n_message SET default_message = '바꿈' WHERE message_key = 'FILE.NOT_FOUND'");
		execute(schema(tenant), "DELETE FROM role_permission WHERE permission_pk IN (SELECT permission_pk FROM permission WHERE permission_code = 'FILE_STORAGE:EXPORT')");
		execute(schema(tenant), "DELETE FROM permission WHERE permission_code = 'FILE_STORAGE:EXPORT'");
		assertEquals("SUCCESS", upgradeService.upgradeTenants(List.of(tenant), null).results().get(0).executionStatus());
		assertEquals("바꿈", scalar(schema(tenant), "SELECT default_message FROM i18n_message WHERE message_key = 'FILE.NOT_FOUND'"));
		assertEquals("1", scalar(schema(tenant), "SELECT count(*)::text FROM permission WHERE permission_code = 'FILE_STORAGE:EXPORT' AND use_yn = 'Y'"));
	}

	private static MockMultipartFile file(String name, byte[] bytes) {
		return new MockMultipartFile("file", name, "application/octet-stream", bytes);
	}

	private static String sha256(byte[] bytes) throws Exception {
		return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
	}

	private SetBefore filesNow() throws Exception {
		return new SetBefore(snapshot());
	}

	private Path newest(SetBefore before) throws Exception {
		List<Path> paths = new ArrayList<>(snapshot());
		paths.removeAll(before.paths());
		assertFalse(paths.isEmpty());
		return paths.get(0);
	}

	private static List<Path> snapshot() throws Exception {
		if (!Files.exists(ROOT)) {
			return List.of();
		}
		try (Stream<Path> walk = Files.walk(ROOT)) {
			return walk.filter(Files::isRegularFile).toList();
		}
	}

	private static String schema(String tenantCode) {
		return "t_" + tenantCode.replace('-', '_');
	}

	private String provision() throws Exception {
		String tenantCode = "it-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
		String schema = "t_" + tenantCode.replace('-', '_');
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

	private HttpHeaders bearer(String tenantCode, long userPk) {
		HttpHeaders headers = new HttpHeaders();
		headers.set(HeaderTenantResolver.HEADER, tenantCode);
		headers.setBearerAuth(jwtTokenService.issue(Long.toString(userPk), JwtTokenService.TENANT_AUDIENCE, JwtTokenService.TENANT_TYPE,
				tenantCode, false, Duration.ofMinutes(15)));
		return headers;
	}

	private void validateSchema(String schema) {
		StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
				.applySetting(AvailableSettings.DATASOURCE, dataSource)
				.applySetting(AvailableSettings.HBM2DDL_AUTO, "validate")
				.applySetting(AvailableSettings.DEFAULT_SCHEMA, schema)
				.applySetting(AvailableSettings.PHYSICAL_NAMING_STRATEGY, CamelCaseToUnderscoresNamingStrategy.class.getName())
				.applySetting(AvailableSettings.DIALECT, "org.hibernate.dialect.PostgreSQLDialect")
				.build();
		try (org.hibernate.SessionFactory sessionFactory = new MetadataSources(registry)
				.addAnnotatedClass(Company.class)
				.addAnnotatedClass(Dept.class)
				.addAnnotatedClass(DeptHistory.class)
				.addAnnotatedClass(Person.class)
				.addAnnotatedClass(PersonAssignment.class)
				.addAnnotatedClass(Nation.class)
				.addAnnotatedClass(SupportLanguage.class)
				.addAnnotatedClass(Menu.class)
				.addAnnotatedClass(UserAccount.class)
				.addAnnotatedClass(UserPasswordHistory.class)
				.addAnnotatedClass(Role.class)
				.addAnnotatedClass(UserRole.class)
				.addAnnotatedClass(Permission.class)
				.addAnnotatedClass(RolePermission.class)
				.addAnnotatedClass(FileStorage.class)
				.addAnnotatedClass(StoredFile.class)
				.addAnnotatedClass(FileAttachment.class)
				.addAnnotatedClass(Team.class)
				.addAnnotatedClass(TeamMember.class)
				.buildMetadata()
				.buildSessionFactory()) {
			assertFalse(sessionFactory.getMetamodel().getEntities().isEmpty());
		}
		finally {
			StandardServiceRegistryBuilder.destroy(registry);
		}
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

	private static long pk(MvcResult result, String field) throws Exception {
		return json(result).get("data").get(field).asLong();
	}

	private static JsonNode json(MvcResult result) throws Exception {
		return JsonMapper.builder().build().readTree(result.getResponse().getContentAsString());
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

	private record SetBefore(List<Path> paths) {
	}

}
