package com.example.cafe.order.entity;

public enum OrderEventStatus {
	PENDING("전송 대기"),
	SENDING("전송 중"),
	SENT("전송 완료"),
	FAILED("전송 실패");

	private final String description;

	OrderEventStatus(String description) {
		this.description = description;
	}

	public String getDescription() {
		return description;
	}
}
