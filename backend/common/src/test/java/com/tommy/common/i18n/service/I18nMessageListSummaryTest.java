package com.tommy.common.i18n.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.lang.reflect.Field;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tommy.common.global.response.ApiResponse;
import com.tommy.common.i18n.dto.MessageResponse;
import com.tommy.common.i18n.entity.I18nMessage;
import com.tommy.common.i18n.entity.I18nNamespace;
import com.tommy.common.i18n.entity.SupportLanguage;
import com.tommy.common.i18n.repository.I18nMessageRepository;
import com.tommy.common.i18n.repository.I18nMessageTextRepository;
import com.tommy.common.i18n.repository.I18nNamespaceRepository;
import com.tommy.common.i18n.repository.MessageTextSlice;
import com.tommy.common.i18n.repository.SupportLanguageRepository;

class I18nMessageListSummaryTest {

	@Test
	void summarizesThePageInOneQueryAndIgnoresInactiveOrBlankLanguages() {
		I18nMessageRepository messages = mock(I18nMessageRepository.class);
		I18nMessageTextRepository texts = mock(I18nMessageTextRepository.class);
		SupportLanguageRepository languages = mock(SupportLanguageRepository.class);
		I18nMessageService service = new I18nMessageService(messages, texts, mock(I18nNamespaceRepository.class), languages,
				mock(BundleService.class));
		I18nNamespace namespace = I18nNamespace.create("SUM", "요약", null, null);
		I18nMessage partial = message(namespace, 1L, "PART");
		I18nMessage full = message(namespace, 2L, "ALL");
		I18nMessage none = message(namespace, 3L, "NONE");
		I18nMessage inactive = message(namespace, 4L, "OLD");
		I18nMessage blank = message(namespace, 5L, "BLANK");
		when(messages.findAll(org.mockito.ArgumentMatchers.<Specification<I18nMessage>>any(), any(Pageable.class)))
				.thenReturn(new PageImpl<>(List.of(partial, full, none, inactive, blank), PageRequest.of(0, 20), 5));
		when(languages.findByUseYnOrderBySortOrderAscSupportLanguageCodeAsc("Y")).thenReturn(List.of(
				SupportLanguage.create("ko-KR", "한국어", "한국어", "ko", null, "LTR", null, null, null, true, 1),
				SupportLanguage.create("en-US", "영어", "English", "en", "ko-KR", "LTR", null, null, null, false, 2)));
		when(texts.findSlicesByMessagePkIn(List.of(1L, 2L, 3L, 4L, 5L))).thenReturn(List.of(
				slice(1L, "ko-KR", "안녕", "APPROVED"),
				slice(1L, "fr-FR", "프랑스", "REVIEWED"),
				slice(2L, "en-US", "all", "APPROVED"),
				slice(2L, "ko-KR", "전체", "DRAFT"),
				slice(4L, "fr-FR", "이전", "REVIEWED"),
				slice(5L, "ko-KR", " ", "DRAFT")));

		var page = service.list(null, null, null, null, null, PageRequest.of(0, 20));

		verify(texts, times(1)).findSlicesByMessagePkIn(List.of(1L, 2L, 3L, 4L, 5L));
		verify(texts, never()).findByMessageMessagePk(any());
		verify(languages, times(1)).findByUseYnOrderBySortOrderAscSupportLanguageCodeAsc("Y");
		assertEquals(List.of("ko-KR"), page.items().get(0).translatedLanguageCodes());
		assertEquals(List.of("en-US"), page.items().get(0).missingLanguageCodes());
		assertEquals(List.of("APPROVED"), page.items().get(0).translationStatusCodes());
		assertEquals(List.of(), page.items().get(0).translations());
		assertEquals(List.of("ko-KR", "en-US"), page.items().get(1).translatedLanguageCodes());
		assertEquals(List.of(), page.items().get(1).missingLanguageCodes());
		assertEquals(List.of("DRAFT", "APPROVED"), page.items().get(1).translationStatusCodes());
		assertEquals(List.of(), page.items().get(2).translatedLanguageCodes());
		assertEquals(List.of("ko-KR", "en-US"), page.items().get(2).missingLanguageCodes());
		assertEquals(List.of(), page.items().get(2).translationStatusCodes());
		assertEquals(List.of(), page.items().get(3).translatedLanguageCodes());
		assertEquals(List.of("ko-KR", "en-US"), page.items().get(3).missingLanguageCodes());
		assertEquals(List.of(), page.items().get(3).translationStatusCodes());
		assertEquals(List.of(), page.items().get(4).translatedLanguageCodes());
		assertEquals(List.of("ko-KR", "en-US"), page.items().get(4).missingLanguageCodes());
	}

	@Test
	void omitsSummaryFieldsFromTheSingleMessageResponse() throws Exception {
		MessageResponse detail = new MessageResponse(1L, "SUM", "PART", "LABEL", "기본", null, "Y", List.of(), null, null, null);
		MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new DetailController(detail)).build();
		mockMvc.perform(get("/detail"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.messageKey").value("PART"))
				.andExpect(jsonPath("$.data.translatedLanguageCodes").doesNotExist())
				.andExpect(jsonPath("$.data.missingLanguageCodes").doesNotExist())
				.andExpect(jsonPath("$.data.translationStatusCodes").doesNotExist());
		String body = mockMvc.perform(get("/detail")).andReturn().getResponse().getContentAsString();
		assertFalse(body.contains("translatedLanguageCodes"));
	}

	private static I18nMessage message(I18nNamespace namespace, long messagePk, String key) {
		I18nMessage message = I18nMessage.create(namespace, key, "LABEL", "기본", null);
		try {
			Field field = I18nMessage.class.getDeclaredField("messagePk");
			field.setAccessible(true);
			field.set(message, messagePk);
		}
		catch (ReflectiveOperationException ex) {
			throw new IllegalStateException(ex);
		}
		return message;
	}

	private static MessageTextSlice slice(long messagePk, String language, String text, String status) {
		return new MessageTextSlice() {
			@Override
			public Long getMessagePk() {
				return messagePk;
			}

			@Override
			public String getSupportLanguageCode() {
				return language;
			}

			@Override
			public String getMessageText() {
				return text;
			}

			@Override
			public String getTranslationStatusCode() {
				return status;
			}
		};
	}

	@RestController
	static class DetailController {

		private final MessageResponse detail;

		DetailController(MessageResponse detail) {
			this.detail = detail;
		}

		@GetMapping("/detail")
		ApiResponse<MessageResponse> detail() {
			return ApiResponse.ok(detail);
		}
	}

}
