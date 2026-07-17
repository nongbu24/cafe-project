package com.example.cafe.user.entity;

public enum UserStatus {

	ADMIN("관리자"),
	USER("일반 회원");

	private final String description;

	UserStatus(String description) {
		this.description = description;
	}

	public String getDescription() {
		return description;
	}
}
