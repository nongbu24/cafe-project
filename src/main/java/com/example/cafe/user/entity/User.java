package com.example.cafe.user.entity;

import com.example.cafe.common.exception.ApplicationException;
import com.example.cafe.common.exception.ErrorCode;
import com.example.cafe.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class User extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 50)
	private String username;

	@Column(nullable = false, length = 60)
	private String password;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private UserStatus userStatus;

	@Column(nullable = false)
	private long pointBalance;

	@Column(name = "is_deleted", nullable = false, columnDefinition = "boolean default false")
	private boolean deleted;

	protected User() {
	}

	private User(String username, String password, UserStatus userStatus) {
		this.username = username;
		this.password = password;
		this.userStatus = userStatus;
	}

	public static User signup(String username, String encodedPassword) {
		return new User(username, encodedPassword, UserStatus.USER);
	}

	public Long getId() {
		return id;
	}

	public long getPointBalance() {
		return pointBalance;
	}

	public String getUsername() {
		return username;
	}

	public String getPassword() {
		return password;
	}

	public UserStatus getUserStatus() {
		return userStatus;
	}

	public boolean isDeleted() {
		return deleted;
	}

	public boolean isWithdrawn() {
		return deleted;
	}

	public void withdraw() {
		deleted = true;
	}

	public void charge(long amount) {
		try {
			pointBalance = Math.addExact(pointBalance, amount);
		} catch (ArithmeticException exception) {
			throw new ApplicationException(ErrorCode.INVALID_REQUEST, "충전 후 포인트가 허용 범위를 초과합니다.");
		}
	}

	public void usePoint(long amount) {
		if (pointBalance < amount) {
			throw new ApplicationException(ErrorCode.INSUFFICIENT_POINTS);
		}

		pointBalance -= amount;
	}
}
