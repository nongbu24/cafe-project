package com.example.cafe.user.controller;

import com.example.cafe.auth.service.AuthService;
import com.example.cafe.auth.service.AuthenticationInterceptor;
import com.example.cafe.auth.service.TokenClaims;
import com.example.cafe.common.response.ApiResponse;
import com.example.cafe.user.dto.MyUserResponse;
import com.example.cafe.user.dto.PasswordChangeRequest;
import com.example.cafe.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

	private final AuthService authService;
	private final UserService userService;

	public UserController(AuthService authService, UserService userService) {
		this.authService = authService;
		this.userService = userService;
	}

	@GetMapping("/me")
	public ApiResponse<MyUserResponse> getMe(HttpServletRequest request) {
		long userId = (long) request.getAttribute(AuthenticationInterceptor.AUTHENTICATED_USER_ID);

		return ApiResponse.success("SUCCESS", "내 정보 조회가 완료되었습니다.", userService.getMe(userId));
	}

	@PatchMapping("/me")
	public ApiResponse<Void> withdraw(HttpServletRequest request) {
		long userId = (long) request.getAttribute(AuthenticationInterceptor.AUTHENTICATED_USER_ID);
		TokenClaims claims = (TokenClaims) request.getAttribute(AuthenticationInterceptor.TOKEN_CLAIMS);
		authService.withdraw(userId, claims);

		return ApiResponse.successMessage("회원 탈퇴가 완료되었습니다.");
	}

	@PatchMapping("/me/password")
	public ApiResponse<Void> changePassword(
		HttpServletRequest request,
		@RequestBody PasswordChangeRequest passwordChangeRequest
	) {
		long userId = (long) request.getAttribute(AuthenticationInterceptor.AUTHENTICATED_USER_ID);
		userService.changePassword(userId, passwordChangeRequest);

		return ApiResponse.successMessage("비밀번호 변경이 완료되었습니다.");
	}
}
