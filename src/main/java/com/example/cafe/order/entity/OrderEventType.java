package com.example.cafe.order.entity;

public enum OrderEventType {
	ORDER_PAID("주문 결제 완료");

	private final String description;

	OrderEventType(String description) {
		this.description = description;
	}

	public String getDescription() {
		return description;
	}
}
