package com.tommy.common.global.exception;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.tommy.common.global.response.ApiResponse;
import com.tommy.common.global.response.FieldErrorItem;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class ApiExceptionHandler {

	private ErrorMessageLocalizer localizer = (key, fallback, request) -> fallback;

	@Autowired(required = false)
	public void setLocalizer(ErrorMessageLocalizer localizer) {
		if (localizer != null) {
			this.localizer = localizer;
		}
	}

	@ExceptionHandler(ApiException.class)
	public ResponseEntity<ApiResponse<Void>> handleApiException(ApiException exception, HttpServletRequest request) {
		return ResponseEntity.status(exception.status())
				.body(ApiResponse.fail(exception.errorCode(), text(exception.errorCode(), exception.getMessage(), request),
						exception.showType(), traceId(), exception.fieldErrors()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException exception, HttpServletRequest request) {
		List<FieldErrorItem> fieldErrors = exception.getBindingResult().getFieldErrors().stream()
				.map(error -> new FieldErrorItem(error.getField(), text(error.getDefaultMessage(), error.getDefaultMessage(), request)))
				.toList();
		return ResponseEntity.badRequest().body(ApiResponse.fail(
				"COMMON.VALIDATION_FAILED",
				text("COMMON.VALIDATION_FAILED", "요청 값이 올바르지 않습니다.", request),
				2,
				traceId(),
				fieldErrors));
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ApiResponse<Void>> handleUnreadable(HttpMessageNotReadableException exception, HttpServletRequest request) {
		return ResponseEntity.badRequest().body(ApiResponse.fail(
				"COMMON.VALIDATION_FAILED",
				text("COMMON.VALIDATION_FAILED", "요청 값이 올바르지 않습니다.", request),
				2,
				traceId(),
				null));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception exception, HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(ApiResponse.fail("COMMON.INTERNAL", text("COMMON.INTERNAL", "요청을 처리하지 못했습니다.", request), 2, traceId(), null));
	}

	private String text(String key, String fallback, HttpServletRequest request) {
		try {
			String localized = localizer.localize(key, fallback, request);
			return localized == null || localized.isBlank() ? fallback : localized;
		}
		catch (RuntimeException ex) {
			return fallback;
		}
	}

	private static String traceId() {
		return UUID.randomUUID().toString();
	}

}
