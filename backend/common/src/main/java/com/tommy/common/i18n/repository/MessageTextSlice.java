package com.tommy.common.i18n.repository;

public interface MessageTextSlice {

	Long getMessagePk();

	String getSupportLanguageCode();

	String getMessageText();

	String getTranslationStatusCode();

}
