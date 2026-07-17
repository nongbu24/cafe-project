package com.example.cafe.common.exception;

import com.example.cafe.common.response.ErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(ApplicationException.class)
	public ResponseEntity<ErrorResponse> handleApplicationException(
		ApplicationException exception
	) {
		ErrorCode errorCode = exception.getErrorCode();

		return ResponseEntity.status(errorCode.getStatus())
			.body(ErrorResponse.of(errorCode.getCode(), exception.getMessage()));
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ErrorResponse> handleTypeMismatch() {
		return errorResponse(ErrorCode.INVALID_REQUEST);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ErrorResponse> handleUnreadableMessage() {
		return errorResponse(ErrorCode.INVALID_REQUEST);
	}

	private ResponseEntity<ErrorResponse> errorResponse(ErrorCode errorCode) {
		return ResponseEntity.status(errorCode.getStatus())
			.body(ErrorResponse.of(errorCode.getCode(), errorCode.getMessage()));
	}
}
