package com.example.cafe.auth.service;

import com.example.cafe.auth.dto.LoginRequest;
import com.example.cafe.auth.dto.LoginResponse;
import com.example.cafe.auth.dto.SignupRequest;
import com.example.cafe.auth.dto.UserResponse;
import com.example.cafe.auth.store.TokenBlacklistStore;
import com.example.cafe.common.exception.ApplicationException;
import com.example.cafe.common.exception.ErrorCode;
import com.example.cafe.user.entity.User;
import com.example.cafe.user.facade.UserFacade;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

	private final UserFacade userFacade;
	private final TokenBlacklistStore tokenBlacklistStore;
	private final JwtTokenProvider jwtTokenProvider;
	private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

	public AuthService(
		UserFacade userFacade,
		TokenBlacklistStore tokenBlacklistStore,
		JwtTokenProvider jwtTokenProvider
	) {
		this.userFacade = userFacade;
		this.tokenBlacklistStore = tokenBlacklistStore;
		this.jwtTokenProvider = jwtTokenProvider;
	}

	@Transactional
	public UserResponse signup(SignupRequest request) {
		if (request == null) {
			throw ApplicationException.invalidRequest();
		}

		validateCredentials(request.username(), request.password());

		if (userFacade.existsByUsername(request.username())) {
			throw new ApplicationException(ErrorCode.DUPLICATE_USERNAME);
		}

		User user = User.signup(request.username(), passwordEncoder.encode(request.password()));

		return UserResponse.from(userFacade.save(user));
	}

	@Transactional(readOnly = true)
	public LoginResponse login(LoginRequest request) {
		if (request == null) {
			throw ApplicationException.invalidRequest();
		}
		validateCredentials(request.username(), request.password());

		User user = userFacade.getActiveUserByUsername(request.username());

		if (!passwordEncoder.matches(request.password(), user.getPassword())) {
			throw new ApplicationException(ErrorCode.INVALID_CREDENTIALS);
		}

		IssuedToken issuedToken = jwtTokenProvider.issue(user);

		return new LoginResponse("Bearer", issuedToken.value());
	}

	@Transactional
	public void logout(TokenClaims claims) {
		tokenBlacklistStore.add(claims.tokenId(), claims.expiresAt());
	}

	@Transactional
	public void withdraw(long userId, TokenClaims claims) {
		User user = userFacade.getUser(userId);
		user.withdraw();
		tokenBlacklistStore.add(claims.tokenId(), claims.expiresAt());
	}

	private void validateCredentials(String username, String password) {
		if (username == null || !username.matches("[A-Za-z0-9_]{4,50}")) {
			throw ApplicationException.invalidRequest(
				"username은 영문, 숫자, 밑줄을 사용하여 4자 이상 50자 이하로 입력해야 합니다."
			);
		}

		if (password == null || !password.matches("[!-~]{8,64}")) {
			throw ApplicationException.invalidRequest(
				"password는 공백 없이 영문, 숫자, 일반 특수문자로 8자 이상 64자 이하로 입력해야 합니다."
			);
		}
	}
}
