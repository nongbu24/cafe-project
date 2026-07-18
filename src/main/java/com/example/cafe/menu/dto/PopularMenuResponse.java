package com.example.cafe.menu.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record PopularMenuResponse(
	OffsetDateTime from,
	OffsetDateTime to,
	List<PopularMenuItemResponse> menus
) {
}
