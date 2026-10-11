package com.tommy.common.i18n.dto;

public record MessageTextResponse(String supportLanguageCode, String messageText, String translationStatusCode,
		boolean machineTranslated, Integer versionNo) {
}
