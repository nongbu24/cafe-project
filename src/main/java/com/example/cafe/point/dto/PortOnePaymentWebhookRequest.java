package com.example.cafe.point.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PortOnePaymentWebhookRequest(
	@NotBlank(message = "웹훅 이벤트 타입 입력은 필수입니다.")
	String type,

	String timestamp,

	@Valid
	@NotNull(message = "웹훅 데이터 입력은 필수입니다.")
	Data data
) {

	public record Data(
		String paymentId,

		String storeId,

		String transactionId
	) {
	}
}
