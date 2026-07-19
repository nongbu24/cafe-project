package com.example.cafe.user.dto;

import com.example.cafe.common.util.DateTimeUtils;
import com.example.cafe.user.entity.User;
import com.example.cafe.user.entity.UserStatus;
import java.time.OffsetDateTime;

public record MyUserResponse(
	long userId,
	String username,
	UserStatus userStatus,
	long pointBalance,
	OffsetDateTime createdAt,
	OffsetDateTime updatedAt
) {

	public static MyUserResponse from(User user) {
		OffsetDateTime updatedAt = null;
		if (user.getUpdatedAt() != null) {
			updatedAt = DateTimeUtils.toKoreaOffsetDateTime(user.getUpdatedAt());
		}

		return new MyUserResponse(
			user.getId(),
			user.getUsername(),
			user.getUserStatus(),
			user.getPointBalance(),
			DateTimeUtils.toKoreaOffsetDateTime(user.getCreatedAt()),
			updatedAt
		);
	}
}
