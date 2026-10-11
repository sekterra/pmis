package com.tommy.common.i18n;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tommy.common.global.exception.ApiException;
import com.tommy.common.global.exception.ApiExceptionHandler;
import com.tommy.common.i18n.controller.I18nQueryController;
import com.tommy.common.i18n.dto.BundleView;
import com.tommy.common.i18n.dto.LanguageItem;
import com.tommy.common.i18n.service.BundleService;
import com.tommy.common.i18n.service.SupportLanguageService;

class I18nApiSliceTest {

	@Test
	void returnsLanguageArrayAndNotModifiedWhenEtagMatches() throws Exception {
		SupportLanguageService languages = mock(SupportLanguageService.class);
		BundleService bundles = mock(BundleService.class);
		when(languages.listActive()).thenReturn(List.of(new LanguageItem("ko-KR", "한국어", "한국어", true, null, "LTR", null, null, null)));
		BundleView view = new BundleView("error", "ko-KR", Map.of("COMMON.INTERNAL", "요청을 처리하지 못했습니다."));
		when(bundles.bundle(eq("error"), isNull(), isNull())).thenReturn(view);
		String etag = BundleEtags.weak(view);
		MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new I18nQueryController(languages, bundles))
				.setControllerAdvice(new ApiExceptionHandler())
				.build();
		mockMvc.perform(get("/api/mdm/v1/i18n/languages"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data").isArray())
				.andExpect(jsonPath("$.data[0].supportLanguageCode").value("ko-KR"));
		mockMvc.perform(get("/api/mdm/v1/i18n/bundles/error"))
				.andExpect(status().isOk())
				.andExpect(header().string("ETag", etag))
				.andExpect(header().string("Cache-Control", "no-cache"))
				.andExpect(jsonPath("$.data.messages['COMMON.INTERNAL']").value("요청을 처리하지 못했습니다."));
		mockMvc.perform(get("/api/mdm/v1/i18n/bundles/error").header("If-None-Match", etag))
				.andExpect(status().isNotModified())
				.andExpect(jsonPath("$.success").doesNotExist());
	}

	@Test
	void keepsTheDefaultMessageWhenBundleLookupFails() throws Exception {
		ApiExceptionHandler handler = new ApiExceptionHandler();
		handler.setLocalizer((key, fallback, request) -> {
			throw new RuntimeException("bundle down");
		});
		MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new BoomController())
				.setControllerAdvice(handler)
				.build();
		mockMvc.perform(get("/boom"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.errorMessage").value("네임스페이스를 찾을 수 없습니다."));
	}

	@RestController
	static class BoomController {
		@GetMapping("/boom")
		void boom() {
			throw new ApiException(404, "I18N.NAMESPACE_NOT_FOUND", "네임스페이스를 찾을 수 없습니다.", 2);
		}
	}

}
