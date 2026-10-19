package com.tommy.common.access;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import com.tommy.common.access.controller.PermissionController;
import com.tommy.common.access.controller.RoleController;
import com.tommy.common.access.controller.UserAccountController;
import com.tommy.common.access.dto.IssuedCredential;
import com.tommy.common.access.dto.RoleGrantRequest;
import com.tommy.common.access.entity.Role;
import com.tommy.common.access.entity.UserAccount;
import com.tommy.common.access.repository.RoleRepository;
import com.tommy.common.access.repository.UserAccountRepository;
import com.tommy.common.access.repository.UserRoleRepository;
import com.tommy.common.access.service.PermissionAdminService;
import com.tommy.common.access.service.RoleAdminService;
import com.tommy.common.access.service.UserAccountService;
import com.tommy.common.global.exception.ApiException;
import com.tommy.common.global.exception.ApiExceptionHandler;
import com.tommy.common.global.security.CurrentTenantUser;
import com.tommy.common.global.time.TenantClock;

@ExtendWith(MockitoExtension.class)
class AccessSliceTest {

	@Mock
	private UserAccountService userAccounts;

	@Mock
	private RoleAdminService roleAdmin;

	@Mock
	private PermissionAdminService permissionAdmin;

	@Mock
	private UserAccountRepository users;

	@Mock
	private UserRoleRepository links;

	@Mock
	private RoleRepository roles;

	@Mock
	private CurrentTenantUser currentUser;

	@Mock
	private TenantClock clock;

	@Test
	void normalizesLoginIdAndRejectsBadShapes() {
		assertEquals("ab.c_1@x-y", ReflectionTestUtils.invokeMethod(UserAccountService.class, "loginId", "  Ab.C_1@X-Y "));
		ApiException shortId = assertThrows(ApiException.class,
				() -> ReflectionTestUtils.invokeMethod(UserAccountService.class, "loginId", "ab"));
		assertEquals("COMMON.VALIDATION_FAILED", shortId.errorCode());
		assertEquals("loginId", shortId.fieldErrors().get(0).field());
	}

	@Test
	void rejectsRoleDateOrderAndDuplicateCodes() {
		UserAccountService service = new UserAccountService(users, links, null, roles, null, null, null, null, currentUser, clock);
		UserAccount account = UserAccount.create("user@example.com", "NORMAL", "hash", LocalDateTime.now(), 1L);
		ReflectionTestUtils.setField(account, "userPk", 3L);
		when(users.findById(3L)).thenReturn(Optional.of(account));
		when(currentUser.pkOrNull()).thenReturn(9L);
		ApiException dates = assertThrows(ApiException.class, () -> service.replaceRoles(3L,
				List.of(new RoleGrantRequest("AUDIT", LocalDate.of(2026, 5, 2), LocalDate.of(2026, 5, 1)))));
		assertEquals("ROLE.INVALID_DATE", dates.errorCode());
		ApiException duplicate = assertThrows(ApiException.class, () -> service.replaceRoles(3L,
				List.of(new RoleGrantRequest("AUDIT", null, null), new RoleGrantRequest("AUDIT", null, null))));
		assertEquals("COMMON.VALIDATION_FAILED", duplicate.errorCode());
		assertEquals("roleCodes", duplicate.fieldErrors().get(0).field());
	}

	@Test
	void rejectsRoleCodeShapeOnCreate() throws Exception {
		mockMvc(new RoleController(roleAdmin)).perform(post("/api/mdm/v1/roles").contentType(MediaType.APPLICATION_JSON)
				.content("{\"roleCode\":\"A\",\"roleName\":\"짧은코드\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errorCode").value("COMMON.VALIDATION_FAILED"))
				.andExpect(jsonPath("$.fieldErrors[?(@.field == 'roleCode')]").exists());
	}

	@Test
	void rejectsUnknownSort() throws Exception {
		mockMvc(new UserAccountController(userAccounts)).perform(get("/api/mdm/v1/users").param("sort", "passwordHash,asc"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errorCode").value("COMMON.INVALID_SORT"));
	}

	@Test
	void hidesTemporaryPasswordFromCacheAndText() throws Exception {
		when(userAccounts.create(any())).thenReturn(new IssuedCredential(1L, "user@example.com", "secret-value", LocalDateTime.now(), "Y"));
		mockMvc(new UserAccountController(userAccounts)).perform(post("/api/mdm/v1/users").contentType(MediaType.APPLICATION_JSON)
				.content("{\"loginId\":\"user@example.com\"}"))
				.andExpect(status().isOk())
				.andExpect(header().string("Cache-Control", "no-store"));
		IssuedCredential credential = new IssuedCredential(1L, "user@example.com", "secret-value", LocalDateTime.now(), "Y");
		assertFalse(credential.toString().contains("secret-value"));
		assertTrue(credential.toString().contains("masked"));
	}

	@Test
	void rejectsPasswordResetWhenTheAccountHasNoHash() {
		UserAccountService service = new UserAccountService(users, links, null, null, null, null, null, null, currentUser, null);
		UserAccount account = UserAccount.create("user@example.com", "NORMAL", "hash", LocalDateTime.now(), 1L);
		ReflectionTestUtils.setField(account, "userPk", 5L);
		ReflectionTestUtils.setField(account, "passwordHash", null);
		when(users.findById(5L)).thenReturn(Optional.of(account));
		when(currentUser.pkOrNull()).thenReturn(9L);
		ApiException error = assertThrows(ApiException.class, () -> service.resetPassword(5L));
		assertEquals("USER.NO_PASSWORD_ACCOUNT", error.errorCode());
		assertFalse(error.toString().toLowerCase().contains("hash"));
	}

	@Test
	void loadsRoleCodesOncePerUserPage() {
		when(clock.today()).thenReturn(LocalDate.of(2026, 10, 19));
		UserAccount account = UserAccount.create("user@example.com", "NORMAL", "hash", LocalDateTime.now(), 1L);
		ReflectionTestUtils.setField(account, "userPk", 4L);
		when(users.findAll(any(Specification.class), any(Pageable.class)))
				.thenReturn(new PageImpl<>(List.of(account), PageRequest.of(0, 20), 1));
		when(users.findCurrentRoleCodes(List.of(4L), LocalDate.of(2026, 10, 19))).thenReturn(List.of());
		UserAccountService service = new UserAccountService(users, links, null, null, null, null, null, null, currentUser, clock);
		service.list(null, null, null, null, null, PageRequest.of(0, 20));
		verify(users).findCurrentRoleCodes(List.of(4L), LocalDate.of(2026, 10, 19));
		when(users.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));
		service.list(null, null, null, null, null, PageRequest.of(0, 20));
		verify(users, times(1)).findCurrentRoleCodes(anyCollection(), any());
	}

	@Test
	void loadsRoleUserCountsOncePerRolePage() {
		when(clock.today()).thenReturn(LocalDate.of(2026, 10, 19));
		Role role = Role.create("AUDIT", "감사", 1, null, 1L);
		ReflectionTestUtils.setField(role, "rolePk", 8L);
		when(roles.findAll(any(Specification.class), any(Pageable.class)))
				.thenReturn(new PageImpl<>(List.of(role), PageRequest.of(0, 20), 1));
		when(roles.countCurrentUsers(List.of(8L), LocalDate.of(2026, 10, 19))).thenReturn(List.of());
		RoleAdminService service = new RoleAdminService(roles, links, null, null, null, null, null, currentUser, clock);
		service.list(null, null, PageRequest.of(0, 20));
		verify(roles).countCurrentUsers(List.of(8L), LocalDate.of(2026, 10, 19));
		when(roles.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));
		service.list(null, null, PageRequest.of(0, 20));
		verify(roles, times(1)).countCurrentUsers(anyCollection(), any());
	}

	@Test
	void permissionSortWhitelistRejectsPasswordFields() throws Exception {
		mockMvc(new PermissionController(permissionAdmin)).perform(get("/api/mdm/v1/permissions").param("sort", "password,asc"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errorCode").value("COMMON.INVALID_SORT"));
	}

	private static MockMvc mockMvc(Object controller) {
		LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();
		return MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(new ApiExceptionHandler()).setValidator(validator).build();
	}

}
