package com.example.cafe.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;

public record ApiResponse<T>(String code, String message, @JsonInclude(JsonInclude.Include.NON_NULL) T data) {

	public static <T> ApiResponse<T> success(T data) {
		return new ApiResponse<>("SUCCESS", "요청이 성공적으로 처리되었습니다.", data);
	}

	public static <T> ApiResponse<T> success(String code, String message, T data) {
		return new ApiResponse<>(code, message, data);
	}

	public static ApiResponse<Void> successMessage(String message) {
		return new ApiResponse<>("SUCCESS", message, null);
	}
}
