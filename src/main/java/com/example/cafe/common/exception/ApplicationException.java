package com.example.cafe.common.exception;

public class ApplicationException extends RuntimeException {

	private final ErrorCode errorCode;

	public ApplicationException(ErrorCode errorCode) {
		this(errorCode, errorCode.getMessage());
	}

	public ApplicationException(ErrorCode errorCode, String message) {
		super(message);
		this.errorCode = errorCode;
	}

	public static ApplicationException invalidRequest() {
		return new ApplicationException(ErrorCode.INVALID_REQUEST);
	}

	public static ApplicationException invalidRequest(String message) {
		return new ApplicationException(ErrorCode.INVALID_REQUEST, message);
	}

	public ErrorCode getErrorCode() {
		return errorCode;
	}
}
