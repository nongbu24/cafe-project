package com.example.cafe.common.entity;

import com.example.cafe.common.util.DateTimeUtils;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.time.LocalDateTime;

@MappedSuperclass
public abstract class BaseEntity {

	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column
	private LocalDateTime updatedAt;

	@PrePersist
	protected void onCreate() {
		createdAt = DateTimeUtils.utcNow();
	}

	@PreUpdate
	protected void onUpdate() {
		updatedAt = DateTimeUtils.utcNow();
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}
}
