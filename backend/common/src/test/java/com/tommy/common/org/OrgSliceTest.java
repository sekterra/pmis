package com.tommy.common.org;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import com.tommy.common.global.exception.ApiExceptionHandler;
import com.tommy.common.global.security.CurrentTenantUser;
import com.tommy.common.global.security.JwtTokenService;
import com.tommy.common.org.controller.CompanyController;
import com.tommy.common.org.entity.Person;
import com.tommy.common.org.repository.PersonAssignmentRepository;
import com.tommy.common.org.repository.PersonRepository;
import com.tommy.common.org.service.CompanyService;
import com.tommy.common.org.service.PersonService;

@ExtendWith(MockitoExtension.class)
class OrgSliceTest {

	@Mock
	private CompanyService companies;

	@Mock
	private PersonRepository people;

	@Mock
	private PersonAssignmentRepository assignments;

	@AfterEach
	void clearSecurity() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void rejectsCompanyCodeAndEmailBeforeTheService() throws Exception {
		LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();
		MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new CompanyController(companies))
				.setControllerAdvice(new ApiExceptionHandler())
				.setValidator(validator)
				.build();
		mockMvc.perform(post("/api/mdm/v1/companies")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"companyCode":"acme","companyName":"에이","companyTypeCode":"OWN","email":"not-an-email"}
						"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errorCode").value("COMMON.VALIDATION_FAILED"))
				.andExpect(jsonPath("$.fieldErrors[?(@.field == 'companyCode')]").exists())
				.andExpect(jsonPath("$.fieldErrors[?(@.field == 'email')]").exists());
	}

	@Test
	void readsOnlyTenantUserSubject() {
		CurrentTenantUser users = new CurrentTenantUser();
		authenticate("15", JwtTokenService.TENANT_TYPE);
		assertEqualsLong(15L, users.pkOrNull());
		authenticate("15", JwtTokenService.PLATFORM_TYPE);
		org.junit.jupiter.api.Assertions.assertNull(users.pkOrNull());
		SecurityContextHolder.clearContext();
		org.junit.jupiter.api.Assertions.assertNull(users.pkOrNull());
	}

	@Test
	void loadsPrimarySummariesOnceForThePage() {
		Person person = Person.create("김", 1L);
		ReflectionTestUtils.setField(person, "personPk", 9L);
		when(people.findAll(any(Specification.class), any(Pageable.class)))
				.thenReturn(new PageImpl<>(List.of(person), PageRequest.of(0, 20), 1));
		when(assignments.findCurrentPrimarySummaries(List.of(9L))).thenReturn(List.of());
		PersonService service = new PersonService(people, assignments, null, new CurrentTenantUser(), null, null);
		service.list(null, null, null, null, null, null, PageRequest.of(0, 20));
		verify(assignments).findCurrentPrimarySummaries(List.of(9L));

		when(people.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));
		service.list(null, null, null, null, null, null, PageRequest.of(0, 20));
		verify(assignments, times(1)).findCurrentPrimarySummaries(anyCollection());
	}

	private static void authenticate(String subject, String type) {
		Jwt jwt = Jwt.withTokenValue("token")
				.header("alg", "none")
				.subject(subject)
				.claim("typ", type)
				.issuedAt(Instant.now())
				.expiresAt(Instant.now().plusSeconds(60))
				.build();
		SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(jwt, "token"));
	}

	private static void assertEqualsLong(Long expected, Long actual) {
		org.junit.jupiter.api.Assertions.assertEquals(expected, actual);
	}

}
