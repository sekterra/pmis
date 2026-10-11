package com.tommy.common.i18n;

import com.tommy.common.global.exception.ApiException;

public final class I18nExceptions {

	private I18nExceptions() {
	}

	public static ApiException namespaceNotFound() {
		return new ApiException(404, "I18N.NAMESPACE_NOT_FOUND", "네임스페이스를 찾을 수 없습니다.", 2);
	}

	public static ApiException languageNotFound() {
		return new ApiException(400, "I18N.LANGUAGE_NOT_FOUND", "언어를 찾을 수 없습니다.", 2);
	}

	public static ApiException languageMissing() {
		return new ApiException(404, "I18N.LANGUAGE_NOT_FOUND", "언어를 찾을 수 없습니다.", 2);
	}

	public static ApiException nationNotFound() {
		return new ApiException(404, "I18N.NATION_NOT_FOUND", "국가를 찾을 수 없습니다.", 2);
	}

	public static ApiException messageNotFound() {
		return new ApiException(404, "I18N.MESSAGE_NOT_FOUND", "메시지를 찾을 수 없습니다.", 2);
	}

	public static ApiException duplicate() {
		return new ApiException(409, "I18N.DUPLICATE", "이미 있는 값입니다.", 2);
	}

	public static ApiException languageInUse() {
		return new ApiException(409, "I18N.LANGUAGE_IN_USE", "사용 중인 언어는 중지할 수 없습니다.", 2);
	}

	public static ApiException defaultRequired() {
		return new ApiException(400, "I18N.DEFAULT_REQUIRED", "기본 언어는 하나만 있어야 합니다.", 2);
	}

	public static ApiException immutable(String message) {
		return new ApiException(400, "COMMON.VALIDATION_FAILED", message, 2);
	}

}
