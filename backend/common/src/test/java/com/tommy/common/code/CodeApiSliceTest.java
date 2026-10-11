package com.tommy.common.code;

import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import com.tommy.common.code.controller.CodeGroupController;
import com.tommy.common.code.service.CodeGroupService;
import com.tommy.common.global.exception.ApiExceptionHandler;
import com.tommy.common.global.response.PageResponse;
import com.tommy.common.global.exception.ApiException;
import com.tommy.common.global.security.PermissionChecker;
import com.tommy.common.global.security.PermissionInterceptor;

class CodeApiSliceTest {

	private CodeGroupService codeGroupService;

	@BeforeEach
	void setUp() {
		codeGroupService = mock(CodeGroupService.class);
		org.mockito.Mockito.when(codeGroupService.list(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
				org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
				.thenReturn(new PageResponse<>(List.of(), 0, 1, 20));
	}

	@Test
	void wrapsPageAndRejectsSortAndValidation() throws Exception {
		mockMvc(allow()).perform(get("/api/mdm/v1/code-groups").param("sort", "unknown,asc"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.errorCode").value("COMMON.INVALID_SORT"));
		mockMvc(allow()).perform(get("/api/mdm/v1/code-groups").param("page", "0"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errorCode").value("COMMON.INVALID_PAGE"));
		mockMvc(allow()).perform(post("/api/mdm/v1/code-groups")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"codeGroupCode\":\"bad\",\"codeGroupName\":\"\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errorCode").value("COMMON.VALIDATION_FAILED"))
				.andExpect(jsonPath("$.fieldErrors").isArray());
		mockMvc(allow()).perform(get("/api/mdm/v1/code-groups"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.items").isArray())
				.andExpect(jsonPath("$.data.total").value(0))
				.andExpect(jsonPath("$.data.page").value(1))
				.andExpect(jsonPath("$.data.size").value(20));
	}

	@Test
	void deniesManagementWhenProfileIsNotDev() throws Exception {
		mockMvc(deny()).perform(get("/api/mdm/v1/code-groups"))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.errorCode").value("AUTH.FORBIDDEN"));
	}

	private static PermissionChecker allow() {
		return (menu, action) -> {
		};
	}

	private static PermissionChecker deny() {
		return (menu, action) -> {
			throw new ApiException(403, "AUTH.FORBIDDEN", "이 기능을 사용할 권한이 없습니다.", 2);
		};
	}

	private MockMvc mockMvc(PermissionChecker checker) {
		LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();
		return MockMvcBuilders.standaloneSetup(new CodeGroupController(codeGroupService))
				.setControllerAdvice(new ApiExceptionHandler())
				.setValidator(validator)
				.addInterceptors(new PermissionInterceptor(checker))
				.build();
	}

}
