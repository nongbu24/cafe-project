package com.example.cafe.menu.dto;

import com.example.cafe.menu.entity.Menu;
import com.example.cafe.menu.entity.MenuStatus;

public record MenuResponse(long menuId, String name, long price, MenuStatus status) {

	public static MenuResponse from(Menu menu) {
		return new MenuResponse(menu.getId(), menu.getName(), menu.getPrice(), menu.getStatus());
	}
}
