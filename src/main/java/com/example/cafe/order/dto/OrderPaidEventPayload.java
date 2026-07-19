package com.example.cafe.order.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record OrderPaidEventPayload(
	Long eventId,
	String eventType,
	OffsetDateTime occurredAt,
	long userId,
	List<OrderPaidMenuPayload> items,
	long paymentAmount
) {
}
