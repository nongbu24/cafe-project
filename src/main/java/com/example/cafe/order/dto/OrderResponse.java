package com.example.cafe.order.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record OrderResponse(
	long orderId,
	long userId,
	List<OrderItemResponse> items,
	long paymentAmount,
	long pointBalance,
	String status,
	OffsetDateTime paidAt
) {
}
