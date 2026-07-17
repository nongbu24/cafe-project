package com.example.cafe.point.dto;

import java.time.OffsetDateTime;

public record PointChargeResponse(
	long userId,
	long chargedAmount,
	long pointBalance,
	OffsetDateTime chargedAt
) {
}
