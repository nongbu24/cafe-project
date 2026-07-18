package com.example.cafe.order.entity;

import com.example.cafe.common.util.DateTimeUtils;
import com.example.cafe.menu.entity.Menu;
import com.example.cafe.user.entity.User;
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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
public class Order {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "menu_id", nullable = false)
	private Menu menu;

	@Column(nullable = false, length = 100)
	private String menuName;

	@Column(nullable = false)
	private long paymentAmount;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private OrderStatus status;

	@Column(nullable = false)
	private LocalDateTime paidAt;

	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;

	protected Order() {
	}

	private Order(User user, Menu menu, LocalDateTime paidAt) {
		this.user = user;
		this.menu = menu;
		this.menuName = menu.getName();
		this.paymentAmount = menu.getPrice();
		this.status = OrderStatus.PAID;
		this.paidAt = paidAt;
	}

	public static Order paid(User user, Menu menu, LocalDateTime paidAt) {
		return new Order(user, menu, paidAt);
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

	public Menu getMenu() {
		return menu;
	}

	public String getMenuName() {
		return menuName;
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
}
