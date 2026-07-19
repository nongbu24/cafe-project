package com.example.cafe.menu.dto;

import com.example.cafe.menu.entity.Menu;
import com.example.cafe.menu.entity.MenuStatus;

public record AdminMenuResponse(
	long menuId,
	String name,
	long price,
	MenuStatus status,
	long orderCount
) {

	public static AdminMenuResponse of(Menu menu, long orderCount) {
		return new AdminMenuResponse(
			menu.getId(),
			menu.getName(),
			menu.getPrice(),
			menu.getStatus(),
			orderCount
		);
	}
}
