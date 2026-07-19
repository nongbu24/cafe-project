package com.example.cafe.user.dto;

import com.example.cafe.user.entity.User;
import com.example.cafe.user.entity.UserStatus;

public record AdminUserSummaryResponse(
	long userId,
	String username,
	UserStatus userStatus
) {

	public static AdminUserSummaryResponse from(User user) {
		return new AdminUserSummaryResponse(
			user.getId(),
			user.getUsername(),
			user.getUserStatus()
		);
	}
}
