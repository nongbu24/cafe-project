package com.example.cafe.order.service;

import com.example.cafe.order.dto.OrderPaidEventPayload;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class OrderPaidKafkaProducer {

	private final KafkaTemplate<String, OrderPaidEventPayload> kafkaTemplate;
	private final String topic;
	private final long sendTimeoutSeconds;

	public OrderPaidKafkaProducer(
		KafkaTemplate<String, OrderPaidEventPayload> kafkaTemplate,
		@Value("${cafe.order.kafka.topic.order-paid}") String topic,
		@Value("${cafe.order.kafka.send-timeout-seconds}") long sendTimeoutSeconds
	) {
		this.kafkaTemplate = kafkaTemplate;
		this.topic = topic;
		this.sendTimeoutSeconds = sendTimeoutSeconds;
	}

	public void send(OrderPaidEventPayload payload) {
		try {
			kafkaTemplate
				.send(topic, String.valueOf(payload.eventId()), payload)
				.get(sendTimeoutSeconds, TimeUnit.SECONDS);
		} catch (Exception exception) {
			throw new IllegalStateException("주문 완료 이벤트를 Kafka로 발행하지 못했습니다.", exception);
		}
	}
}
