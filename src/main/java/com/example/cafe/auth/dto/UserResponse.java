package com.example.cafe.auth.dto;

import com.example.cafe.user.entity.User;
import com.example.cafe.user.entity.UserStatus;

public record UserResponse(long userId, String username, UserStatus userStatus) {

	public static UserResponse from(User user) {
		return new UserResponse(user.getId(), user.getUsername(), user.getUserStatus());
	}
}
