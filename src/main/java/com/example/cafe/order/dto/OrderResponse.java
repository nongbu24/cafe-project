package com.example.cafe.order.dto;

import java.time.OffsetDateTime;

public record OrderResponse(
	long orderId,
	long userId,
	OrderMenuResponse menu,
	long paymentAmount,
	long pointBalance,
	String status,
	OffsetDateTime paidAt
) {
}
