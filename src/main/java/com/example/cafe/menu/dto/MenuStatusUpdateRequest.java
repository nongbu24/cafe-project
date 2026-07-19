package com.example.cafe.menu.dto;

import com.example.cafe.menu.entity.MenuStatus;
import jakarta.validation.constraints.NotNull;

public record MenuStatusUpdateRequest(
	@NotNull(message = "메뉴 상태는 필수로 입력해야 합니다.")
	MenuStatus status
) {
}
