package com.example.cafe.point.service;

public record PortOnePayment(
	String paymentId,
	String storeId,
	String status,
	long totalAmount,
	String transactionId
) {
}
