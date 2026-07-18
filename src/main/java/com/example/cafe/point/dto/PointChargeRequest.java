package com.example.cafe.point.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PointChargeRequest(
	@NotNull(message = "충전할 포인트는 필수로 입력해야 합니다.")
	@Positive(message = "충전할 포인트는 1 이상이어야 합니다.")
	Long amount
) {
}
