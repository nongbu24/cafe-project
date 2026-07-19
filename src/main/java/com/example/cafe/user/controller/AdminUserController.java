package com.example.cafe.user.controller;

import com.example.cafe.auth.service.AuthenticationInterceptor;
import com.example.cafe.common.exception.ApplicationException;
import com.example.cafe.common.response.ApiResponse;
import com.example.cafe.common.response.PageResponse;
import com.example.cafe.user.dto.AdminUserDetailResponse;
import com.example.cafe.user.dto.AdminUserSummaryResponse;
import com.example.cafe.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/users")
public class AdminUserController {

	private final UserService userService;

	public AdminUserController(UserService userService) {
		this.userService = userService;
	}

	@GetMapping
	public ApiResponse<PageResponse<AdminUserSummaryResponse>> getUsers(
		HttpServletRequest request,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "10") int size
	) {
		validatePagination(page, size);

		return ApiResponse.success(
			"SUCCESS",
			"회원 목록 조회가 완료되었습니다.",
			userService.getUsersForAdmin(getAuthenticatedUserId(request), page, size)
		);
	}

	@GetMapping("/{userId}")
	public ApiResponse<AdminUserDetailResponse> getUser(
		HttpServletRequest request,
		@PathVariable long userId
	) {
		return ApiResponse.success(
			"SUCCESS",
			"회원 조회가 완료되었습니다.",
			userService.getUserForAdmin(getAuthenticatedUserId(request), userId)
		);
	}

	private long getAuthenticatedUserId(HttpServletRequest request) {
		return (long) request.getAttribute(AuthenticationInterceptor.AUTHENTICATED_USER_ID);
	}

	private void validatePagination(int page, int size) {
		if (page < 0) {
			throw ApplicationException.invalidRequest("page는 0 이상이어야 합니다.");
		}

		if (size < 1 || size > 100) {
			throw ApplicationException.invalidRequest("size는 1 이상 100 이하여야 합니다.");
		}
	}
}
