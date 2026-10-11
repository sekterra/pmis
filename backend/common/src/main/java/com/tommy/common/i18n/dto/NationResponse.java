package com.tommy.common.i18n.dto;

public record NationResponse(Long nationPk, String nationCode, String isoAlpha3Code, String isoNumericCode,
		String nationName, String defaultLanguageCode, String phoneCode, String currencyCode, String timezoneId,
		Integer sortOrder, String useYn) {
}
