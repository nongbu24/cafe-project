package com.example.cafe.common.exception;

import com.example.cafe.common.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(ApplicationException.class)
	public ResponseEntity<ApiResponse<Void>> handleApplicationException(
		ApplicationException exception
	) {
		ErrorCode errorCode = exception.getErrorCode();

		return ResponseEntity.status(errorCode.getStatus())
			.body(ApiResponse.error(errorCode.getCode(), exception.getMessage()));
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ApiResponse<Void>> handleTypeMismatch() {
		return errorResponse(ErrorCode.INVALID_REQUEST);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ApiResponse<Void>> handleUnreadableMessage() {
		return errorResponse(ErrorCode.INVALID_REQUEST);
	}

	private ResponseEntity<ApiResponse<Void>> errorResponse(ErrorCode errorCode) {
		return ResponseEntity.status(errorCode.getStatus())
			.body(ApiResponse.error(errorCode.getCode(), errorCode.getMessage()));
	}
}
