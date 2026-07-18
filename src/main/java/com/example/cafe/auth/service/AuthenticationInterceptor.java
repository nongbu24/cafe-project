package com.example.cafe.auth.service;

import com.example.cafe.auth.store.TokenBlacklistStore;
import com.example.cafe.common.exception.ApplicationException;
import com.example.cafe.common.exception.ErrorCode;
import com.example.cafe.user.entity.User;
import com.example.cafe.user.facade.UserFacade;
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
	private final UserFacade userFacade;

	public AuthenticationInterceptor(
		JwtTokenProvider jwtTokenProvider,
		TokenBlacklistStore tokenBlacklistStore,
		UserFacade userFacade
	) {
		this.jwtTokenProvider = jwtTokenProvider;
		this.tokenBlacklistStore = tokenBlacklistStore;
		this.userFacade = userFacade;
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

		User user = userFacade.getActiveUser(claims.userId());

		request.setAttribute(AUTHENTICATED_USER_ID, user.getId());
		request.setAttribute(TOKEN_CLAIMS, claims);

		return true;
	}
}
