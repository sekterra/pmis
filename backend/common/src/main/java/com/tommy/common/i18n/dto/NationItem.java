package com.tommy.common.i18n.dto;

public record NationItem(String nationCode, String nationName, String defaultLanguageCode, String phoneCode,
		String currencyCode, String timezoneId) {
}
