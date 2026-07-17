package com.example.cafe.user.entity;

import com.example.cafe.common.exception.ApplicationException;
import com.example.cafe.common.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private long pointBalance;

	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(nullable = false)
	private LocalDateTime updatedAt;

	protected User() {
	}

	@PrePersist
	void onCreate() {
		LocalDateTime now = LocalDateTime.now();
		createdAt = now;
		updatedAt = now;
	}

	@PreUpdate
	void onUpdate() {
		updatedAt = LocalDateTime.now();
	}

	public Long getId() {
		return id;
	}

	public long getPointBalance() {
		return pointBalance;
	}

	public void charge(long amount) {
		try {
			pointBalance = Math.addExact(pointBalance, amount);
		} catch (ArithmeticException exception) {
			throw new ApplicationException(
				ErrorCode.INVALID_REQUEST,
				"충전 후 포인트가 허용 범위를 초과합니다."
			);
		}
	}
}
