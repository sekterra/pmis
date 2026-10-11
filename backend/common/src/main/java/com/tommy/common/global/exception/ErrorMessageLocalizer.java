package com.tommy.common.global.exception;

import jakarta.servlet.http.HttpServletRequest;

public interface ErrorMessageLocalizer {

	String localize(String key, String fallback, HttpServletRequest request);

}
