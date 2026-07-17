package com.example.cafe.auth.service;

import com.example.cafe.auth.store.TokenBlacklistStore;
import com.example.cafe.common.exception.ApplicationException;
import com.example.cafe.common.exception.ErrorCode;
import com.example.cafe.user.entity.User;
import com.example.cafe.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthenticationInterceptor implements HandlerInterceptor {

	public static final String AUTHENTICATED_USER_ID = "authenticatedUserId";
	public static final String TOKEN_CLAIMS = "tokenClaims";

	private final JwtTokenProvider jwtTokenProvider;
	private final TokenBlacklistStore tokenBlacklistStore;
	private final UserRepository userRepository;

	public AuthenticationInterceptor(
		JwtTokenProvider jwtTokenProvider,
		TokenBlacklistStore tokenBlacklistStore,
		UserRepository userRepository
	) {
		this.jwtTokenProvider = jwtTokenProvider;
		this.tokenBlacklistStore = tokenBlacklistStore;
		this.userRepository = userRepository;
	}

	@Override
	public boolean preHandle(
		HttpServletRequest request,
		HttpServletResponse response,
		Object handler
	) {
		String authorization = request.getHeader("Authorization");

		if (authorization == null || !authorization.startsWith("Bearer ")) {
			throw new ApplicationException(ErrorCode.AUTHENTICATION_REQUIRED);
		}

		TokenClaims claims = jwtTokenProvider.parse(authorization.substring(7));

		if (tokenBlacklistStore.contains(claims.tokenId())) {
			throw new ApplicationException(ErrorCode.BLACKLISTED_TOKEN);
		}

		User user = userRepository.findById(claims.userId())
			.filter(found -> !found.isWithdrawn())
			.orElseThrow(() -> new ApplicationException(ErrorCode.INVALID_TOKEN));

		request.setAttribute(AUTHENTICATED_USER_ID, user.getId());
		request.setAttribute(TOKEN_CLAIMS, claims);

		return true;
	}
}
