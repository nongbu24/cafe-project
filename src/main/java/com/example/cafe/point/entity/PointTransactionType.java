package com.example.cafe.point.entity;

public enum PointTransactionType {
	CHARGE("충전"),
	PAYMENT("결제");

	private final String description;

	PointTransactionType(String description) {
		this.description = description;
	}

	public String getDescription() {
		return description;
	}
}
