package com.example.cafe.point.entity;

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
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "point_transaction")
public class PointTransaction {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(name = "order_id", unique = true)
	private Long orderId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private PointTransactionType type;

	@Column(nullable = false)
	private long amount;

	@Column(nullable = false)
	private long balanceAfter;

	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;

	protected PointTransaction() {
	}

	private PointTransaction(
		User user,
		Long orderId,
		PointTransactionType type,
		long amount,
		long balanceAfter,
		LocalDateTime createdAt
	) {
		this.user = user;
		this.orderId = orderId;
		this.type = type;
		this.amount = amount;
		this.balanceAfter = balanceAfter;
		this.createdAt = createdAt;
	}

	public static PointTransaction charge(
		User user,
		long amount,
		long balanceAfter,
		LocalDateTime chargedAt
	) {
		return new PointTransaction(
			user,
			null,
			PointTransactionType.CHARGE,
			amount,
			balanceAfter,
			chargedAt
		);
	}

	public static PointTransaction payment(
		User user,
		long orderId,
		long amount,
		long balanceAfter,
		LocalDateTime paidAt
	) {
		return new PointTransaction(
			user,
			orderId,
			PointTransactionType.PAYMENT,
			amount,
			balanceAfter,
			paidAt
		);
	}
}
