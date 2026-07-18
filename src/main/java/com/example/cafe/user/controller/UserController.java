package com.example.cafe.user.controller;

import com.example.cafe.auth.service.AuthService;
import com.example.cafe.auth.service.AuthenticationInterceptor;
import com.example.cafe.auth.service.TokenClaims;
import com.example.cafe.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

	private final AuthService authService;

	public UserController(AuthService authService) {
		this.authService = authService;
	}

	@PatchMapping("/me")
	public ApiResponse<Void> withdraw(HttpServletRequest request) {
		long userId = (long) request.getAttribute(AuthenticationInterceptor.AUTHENTICATED_USER_ID);
		TokenClaims claims = (TokenClaims) request.getAttribute(AuthenticationInterceptor.TOKEN_CLAIMS);
		authService.withdraw(userId, claims);
		return ApiResponse.successMessage("회원 탈퇴가 완료되었습니다.");
	}
}
