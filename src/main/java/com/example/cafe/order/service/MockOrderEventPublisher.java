package com.example.cafe.order.service;

import com.example.cafe.common.util.DateTimeUtils;
import com.example.cafe.order.dto.OrderPaidEventPayload;
import com.example.cafe.order.entity.OrderEventOutbox;
import com.example.cafe.order.repository.OrderEventOutboxRepository;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
public class MockOrderEventPublisher {

	private final MockOrderDataCollector dataCollector;
	private final OrderEventOutboxRepository orderEventOutboxRepository;
	private final ObjectMapper objectMapper;

	public MockOrderEventPublisher(
		MockOrderDataCollector dataCollector,
		OrderEventOutboxRepository orderEventOutboxRepository,
		ObjectMapper objectMapper
	) {
		this.dataCollector = dataCollector;
		this.orderEventOutboxRepository = orderEventOutboxRepository;
		this.objectMapper = objectMapper;
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void publish(OrderEventOutbox event) {
		dataCollector.send(toPayload(event));

		event.markSent(DateTimeUtils.utcNow());
		orderEventOutboxRepository.save(event);
	}

	private OrderPaidEventPayload toPayload(OrderEventOutbox event) {
		try {
			JsonNode payload = objectMapper.readTree(event.getPayload());
			return new OrderPaidEventPayload(
				payload.path("eventId").asLong(),
				payload.path("eventType").asText(),
				OffsetDateTime.parse(payload.path("occurredAt").asText()),
				payload.path("userId").asLong(),
				payload.path("menuId").asLong(),
				payload.path("paymentAmount").asLong()
			);
		} catch (Exception exception) {
			throw new IllegalStateException("Outbox payload를 전송 형식으로 읽을 수 없습니다.", exception);
		}
	}
}
