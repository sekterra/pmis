package com.tommy.common.i18n.dto;

import java.util.List;

public record MessageResponse(Long messagePk, String namespaceCode, String messageKey, String messageTypeCode,
		String defaultMessage, String paramDescription, String useYn, List<MessageTextResponse> translations) {
}
