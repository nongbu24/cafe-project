package com.example.cafe.order.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "order_event_outbox")
public class OrderEventOutbox {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "order_id", nullable = false, unique = true)
	private Order order;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 50)
	private OrderEventType eventType;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String payload;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private OrderEventStatus status;

	@Column(nullable = false)
	private int retryCount;

	private LocalDateTime nextRetryAt;

	private LocalDateTime sentAt;

	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;

	protected OrderEventOutbox() {
	}

	private OrderEventOutbox(Order order, String payload) {
		this.order = order;
		this.eventType = OrderEventType.ORDER_PAID;
		this.payload = payload;
		this.status = OrderEventStatus.PENDING;
	}

	public static OrderEventOutbox orderPaid(Order order, String payload) {
		return new OrderEventOutbox(order, payload);
	}

	@PrePersist
	void onCreate() {
		createdAt = LocalDateTime.now();
	}

	public void markSent(LocalDateTime sentAt) {
		this.status = OrderEventStatus.SENT;
		this.sentAt = sentAt;
	}

	public Long getId() {
		return id;
	}

	public Order getOrder() {
		return order;
	}

	public OrderEventType getEventType() {
		return eventType;
	}

	public String getPayload() {
		return payload;
	}

	public OrderEventStatus getStatus() {
		return status;
	}
}
