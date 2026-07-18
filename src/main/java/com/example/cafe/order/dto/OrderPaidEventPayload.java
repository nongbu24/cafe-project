package com.example.cafe.order.dto;

import java.time.OffsetDateTime;

public record OrderPaidEventPayload(
	Long eventId,
	String eventType,
	OffsetDateTime occurredAt,
	long userId,
	long menuId,
	long paymentAmount
) {
}
