package com.example.cafe.point.entity;

import com.example.cafe.common.entity.BaseEntity;
import com.example.cafe.common.exception.ApplicationException;
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
@Table(name = "point_charge_payment")
public class PointChargePayment extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(nullable = false, unique = true, length = 64)
	private String paymentId;

	@Column(nullable = false)
	private long amount;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private PointChargePaymentStatus status;

	@Column(length = 100)
	private String portoneTransactionId;

	@Column
	private LocalDateTime paidAt;

	protected PointChargePayment() {
	}

	private PointChargePayment(User user, String paymentId, long amount) {
		this.user = user;
		this.paymentId = paymentId;
		this.amount = amount;
		this.status = PointChargePaymentStatus.READY;
	}

	public static PointChargePayment ready(User user, String paymentId, long amount) {
		if (amount < 1) {
			throw ApplicationException.invalidRequest("amount는 1 이상이어야 합니다.");
		}

		return new PointChargePayment(user, paymentId, amount);
	}

	public Long getId() {
		return id;
	}

	public User getUser() {
		return user;
	}

	public String getPaymentId() {
		return paymentId;
	}

	public long getAmount() {
		return amount;
	}

	public PointChargePaymentStatus getStatus() {
		return status;
	}

	public boolean isPaid() {
		return status == PointChargePaymentStatus.PAID;
	}

	public void complete(String portoneTransactionId, LocalDateTime paidAt) {
		if (isPaid()) {
			return;
		}

		this.status = PointChargePaymentStatus.PAID;
		this.portoneTransactionId = portoneTransactionId;
		this.paidAt = paidAt;
	}
}
