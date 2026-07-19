package com.example.cafe.point.dto;

public record PointChargePaymentResponse(
	String paymentId,
	String storeId,
	String channelKey,
	String orderName,
	long amount,
	String currency
) {
}
