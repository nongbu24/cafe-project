package com.example.cafe.order.service;

import com.example.cafe.common.util.DateTimeUtils;
import com.example.cafe.order.dto.OrderPaidEventPayload;
import com.example.cafe.order.dto.OrderPaidMenuPayload;
import com.example.cafe.order.entity.OrderEventOutbox;
import com.example.cafe.order.entity.OrderEventStatus;
import com.example.cafe.order.repository.OrderEventOutboxRepository;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
public class OrderOutboxSender {

	private static final Logger log = LoggerFactory.getLogger(OrderOutboxSender.class);

	private final MockOrderDataCollector dataCollector;
	private final OrderEventOutboxRepository orderEventOutboxRepository;
	private final ObjectMapper objectMapper;

	public OrderOutboxSender(
		MockOrderDataCollector dataCollector,
		OrderEventOutboxRepository orderEventOutboxRepository,
		ObjectMapper objectMapper
	) {
		this.dataCollector = dataCollector;
		this.orderEventOutboxRepository = orderEventOutboxRepository;
		this.objectMapper = objectMapper;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void send(long eventId) {
		OrderEventOutbox event = orderEventOutboxRepository.findById(eventId)
			.orElseThrow(() -> new IllegalStateException("Outbox 이벤트를 찾을 수 없습니다."));

		if (event.getStatus() == OrderEventStatus.SENT || event.getStatus() == OrderEventStatus.FAILED) {
			return;
		}

		try {
			dataCollector.send(toPayload(event));
			event.markSent(DateTimeUtils.utcNow());
		} catch (Exception exception) {
			event.markSendFailed(DateTimeUtils.utcNow());
			log.warn(
				"Outbox 이벤트 전송에 실패했습니다. eventId={}, retryCount={}, nextRetryAt={}, status={}",
				event.getId(),
				event.getRetryCount(),
				event.getNextRetryAt(),
				event.getStatus(),
				exception
			);
		}
	}

	private OrderPaidEventPayload toPayload(OrderEventOutbox event) {
		try {
			JsonNode payload = objectMapper.readTree(event.getPayload());
			return new OrderPaidEventPayload(
				payload.path("eventId").asLong(),
				payload.path("eventType").asText(),
				OffsetDateTime.parse(payload.path("occurredAt").asText()),
				payload.path("userId").asLong(),
				toItems(payload.path("items")),
				payload.path("paymentAmount").asLong()
			);
		} catch (Exception exception) {
			throw new IllegalStateException("Outbox payload를 전송 형식으로 읽을 수 없습니다.", exception);
		}
	}

	private List<OrderPaidMenuPayload> toItems(JsonNode itemsNode) {
		List<OrderPaidMenuPayload> items = new ArrayList<>();
		for (JsonNode itemNode : itemsNode) {
			items.add(new OrderPaidMenuPayload(
				itemNode.path("menuId").asLong(),
				itemNode.path("quantity").asInt()
			));
		}
		return items;
	}
}
