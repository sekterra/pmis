package com.tommy.common.i18n.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

public record MessageResponse(Long messagePk, String namespaceCode, String messageKey, String messageTypeCode,
		String defaultMessage, String paramDescription, String useYn, List<MessageTextResponse> translations,
		@JsonInclude(JsonInclude.Include.NON_NULL) List<String> translatedLanguageCodes,
		@JsonInclude(JsonInclude.Include.NON_NULL) List<String> missingLanguageCodes,
		@JsonInclude(JsonInclude.Include.NON_NULL) List<String> translationStatusCodes) {
}
