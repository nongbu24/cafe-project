package com.example.cafe.order.service;

import com.example.cafe.common.util.DateTimeUtils;
import com.example.cafe.order.dto.OrderPaidEventPayload;
import com.example.cafe.order.entity.Order;
import com.example.cafe.order.entity.OrderEventOutbox;
import com.example.cafe.order.repository.OrderEventOutboxRepository;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class MockOrderEventPublisher {

	private final MockOrderDataCollector dataCollector;
	private final OrderEventOutboxRepository orderEventOutboxRepository;

	public MockOrderEventPublisher(
		MockOrderDataCollector dataCollector,
		OrderEventOutboxRepository orderEventOutboxRepository
	) {
		this.dataCollector = dataCollector;
		this.orderEventOutboxRepository = orderEventOutboxRepository;
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void publish(OrderEventOutbox event) {
		Order order = event.getOrder();
		dataCollector.send(new OrderPaidEventPayload(
			event.getId(),
			event.getEventType().name(),
			DateTimeUtils.toKoreaOffsetDateTime(order.getPaidAt()),
			order.getUser().getId(),
			order.getMenu().getId(),
			order.getPaymentAmount()
		));

		event.markSent(LocalDateTime.now());
		orderEventOutboxRepository.save(event);
	}
}
