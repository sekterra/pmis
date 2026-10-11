package com.tommy.common.i18n.dto;

public record SupportLanguageResponse(String supportLanguageCode, String languageName, String nativeName,
		String isoLanguageCode, String fallbackLanguageCode, String textDirectionCode, String dateFormat,
		String datetimeFormat, String numberFormat, boolean defaultLanguage, Integer sortOrder, String useYn) {
}
