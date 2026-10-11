package com.tommy.common.global.exception;

import java.util.List;

import com.tommy.common.global.response.FieldErrorItem;

public class ApiException extends RuntimeException {

	private final int status;
	private final String errorCode;
	private final int showType;
	private final List<FieldErrorItem> fieldErrors;

	public ApiException(int status, String errorCode, String message, int showType) {
		this(status, errorCode, message, showType, null);
	}

	public ApiException(int status, String errorCode, String message, int showType, List<FieldErrorItem> fieldErrors) {
		super(message);
		this.status = status;
		this.errorCode = errorCode;
		this.showType = showType;
		this.fieldErrors = fieldErrors;
	}

	public int status() {
		return status;
	}

	public String errorCode() {
		return errorCode;
	}

	public int showType() {
		return showType;
	}

	public List<FieldErrorItem> fieldErrors() {
		return fieldErrors;
	}

}
