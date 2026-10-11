package com.tommy.common.global.response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
		boolean success,
		T data,
		String errorCode,
		String errorMessage,
		Integer showType,
		String traceId,
		List<FieldErrorItem> fieldErrors) {

	public static <T> ApiResponse<T> ok(T data) {
		return new ApiResponse<>(true, data, null, null, null, null, null);
	}

	public static ApiResponse<Void> fail(String errorCode, String errorMessage, int showType, String traceId,
			List<FieldErrorItem> fieldErrors) {
		return new ApiResponse<>(false, null, errorCode, errorMessage, showType, traceId, fieldErrors);
	}

}
