package com.example.cafe.order.service;

import com.example.cafe.common.util.DateTimeUtils;
import com.example.cafe.order.entity.OrderEventStatus;
import com.example.cafe.order.repository.OrderEventOutboxRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class OrderOutboxRetryService {

	private final OrderEventOutboxRepository orderEventOutboxRepository;
	private final OrderOutboxSender orderOutboxSender;

	public OrderOutboxRetryService(
		OrderEventOutboxRepository orderEventOutboxRepository,
		OrderOutboxSender orderOutboxSender
	) {
		this.orderEventOutboxRepository = orderEventOutboxRepository;
		this.orderOutboxSender = orderOutboxSender;
	}

	@Scheduled(fixedDelayString = "${cafe.order.outbox.retry-fixed-delay-ms:10000}")
	public void resendDueEvents() {
		orderEventOutboxRepository
			.findTop20ByStatusAndNextRetryAtLessThanEqualOrderByNextRetryAtAscIdAsc(
				OrderEventStatus.PENDING,
				DateTimeUtils.utcNow()
			)
			.forEach(event -> orderOutboxSender.send(event.getId()));
	}
}
