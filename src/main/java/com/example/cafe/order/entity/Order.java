package com.example.cafe.order.entity;

import com.example.cafe.common.util.DateTimeUtils;
import com.example.cafe.user.entity.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
public class Order {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(nullable = false)
	private long paymentAmount;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private OrderStatus status;

	@Column(nullable = false)
	private LocalDateTime paidAt;

	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<OrderItem> items = new ArrayList<>();

	protected Order() {
	}

	private Order(User user, List<OrderItem> items, long paymentAmount, LocalDateTime paidAt) {
		this.user = user;
		this.paymentAmount = paymentAmount;
		this.status = OrderStatus.PAID;
		this.paidAt = paidAt;
		items.forEach(this::addItem);
	}

	public static Order paid(User user, List<OrderItem> items, long paymentAmount, LocalDateTime paidAt) {
		return new Order(user, items, paymentAmount, paidAt);
	}

	private void addItem(OrderItem item) {
		item.assignOrder(this);
		items.add(item);
	}

	@PrePersist
	void onCreate() {
		createdAt = DateTimeUtils.utcNow();
	}

	public Long getId() {
		return id;
	}

	public User getUser() {
		return user;
	}

	public long getPaymentAmount() {
		return paymentAmount;
	}

	public OrderStatus getStatus() {
		return status;
	}

	public LocalDateTime getPaidAt() {
		return paidAt;
	}

	public List<OrderItem> getItems() {
		return List.copyOf(items);
	}
}
