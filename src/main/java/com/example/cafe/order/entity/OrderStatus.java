package com.example.cafe.order.entity;

public enum OrderStatus {
	PAID("결제 완료");

	private final String description;

	OrderStatus(String description) {
		this.description = description;
	}

	public String getDescription() {
		return description;
	}
}
