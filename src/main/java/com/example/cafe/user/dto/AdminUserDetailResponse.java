package com.example.cafe.user.dto;

import com.example.cafe.common.util.DateTimeUtils;
import com.example.cafe.user.entity.User;
import com.example.cafe.user.entity.UserStatus;
import java.time.OffsetDateTime;

public record AdminUserDetailResponse(
	long userId,
	String username,
	UserStatus userStatus,
	long pointBalance,
	boolean deleted,
	OffsetDateTime createdAt,
	OffsetDateTime updatedAt
) {

	public static AdminUserDetailResponse from(User user) {
		OffsetDateTime updatedAt = null;
		if (user.getUpdatedAt() != null) {
			updatedAt = DateTimeUtils.toKoreaOffsetDateTime(user.getUpdatedAt());
		}

		return new AdminUserDetailResponse(
			user.getId(),
			user.getUsername(),
			user.getUserStatus(),
			user.getPointBalance(),
			user.isDeleted(),
			DateTimeUtils.toKoreaOffsetDateTime(user.getCreatedAt()),
			updatedAt
		);
	}
}
