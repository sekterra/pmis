package com.tommy.common.i18n.dto;

public record LanguageItem(String supportLanguageCode, String languageName, String nativeName, boolean defaultLanguage,
		String fallbackLanguageCode, String textDirectionCode, String dateFormat, String datetimeFormat,
		String numberFormat) {
}
