package com.example.cafe.point.dto;

public record PortOnePaymentWebhookResponse(
	String paymentId,
	String status,
	boolean charged
) {
}
