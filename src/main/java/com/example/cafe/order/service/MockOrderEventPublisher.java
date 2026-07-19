package com.example.cafe.order.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class MockOrderEventPublisher {

	private final OrderOutboxSender orderOutboxSender;

	public MockOrderEventPublisher(OrderOutboxSender orderOutboxSender) {
		this.orderOutboxSender = orderOutboxSender;
	}

	@Async
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void publish(OrderPaidOutboxEvent publishedEvent) {
		orderOutboxSender.send(publishedEvent.eventId());
	}
}
