package com.example.cafe.auth.controller;

import com.example.cafe.auth.dto.LoginRequest;
import com.example.cafe.auth.dto.LoginResponse;
import com.example.cafe.auth.dto.SignupRequest;
import com.example.cafe.auth.dto.UserResponse;
import com.example.cafe.auth.service.AuthService;
import com.example.cafe.auth.service.AuthenticationInterceptor;
import com.example.cafe.auth.service.TokenClaims;
import com.example.cafe.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/signup")
	public ResponseEntity<ApiResponse<UserResponse>> signup(@RequestBody SignupRequest request) {
		UserResponse response = authService.signup(request);

		return ResponseEntity.created(URI.create("/api/v1/users/" + response.userId()))
			.body(ApiResponse.success(response));
	}

	@PostMapping("/login")
	public ApiResponse<LoginResponse> login(@RequestBody LoginRequest request) {
		return ApiResponse.success(authService.login(request));
	}

	@PostMapping("/logout")
	public ApiResponse<Void> logout(HttpServletRequest request) {
		TokenClaims claims = (TokenClaims) request.getAttribute(AuthenticationInterceptor.TOKEN_CLAIMS);
		authService.logout(claims);

		return ApiResponse.success(null);
	}
}
