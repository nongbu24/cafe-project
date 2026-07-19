package com.example.cafe.point.entity;

public enum PointChargePaymentStatus {

	READY("결제 대기"),
	PAID("결제 완료");

	private final String description;

	PointChargePaymentStatus(String description) {
		this.description = description;
	}

	public String getDescription() {
		return description;
	}
}
