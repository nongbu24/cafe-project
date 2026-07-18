package com.example.cafe.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.cafe.auth.dto.SignupRequest;
import com.example.cafe.auth.store.TokenBlacklistStore;
import com.example.cafe.common.exception.ApplicationException;
import com.example.cafe.common.exception.ErrorCode;
import com.example.cafe.user.entity.User;
import com.example.cafe.user.facade.UserFacade;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

class AuthServiceTest {

	@Test
	void 회원가입_저장_중_username_제약_충돌이_발생하면_중복_username_오류로_변환한다() {
		UserFacade userFacade = mock(UserFacade.class);
		TokenBlacklistStore tokenBlacklistStore = mock(TokenBlacklistStore.class);
		JwtTokenProvider jwtTokenProvider = mock(JwtTokenProvider.class);
		AuthService authService = new AuthService(userFacade, tokenBlacklistStore, jwtTokenProvider);
		SignupRequest request = new SignupRequest("same_user", "Cafe1234!");
		when(userFacade.existsByUsername("same_user")).thenReturn(false);
		when(userFacade.save(any(User.class))).thenThrow(new DataIntegrityViolationException("duplicate username"));

		ApplicationException exception = assertThrows(
			ApplicationException.class,
			() -> authService.signup(request)
		);

		assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_USERNAME);
	}
}
