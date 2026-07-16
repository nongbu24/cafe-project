package com.example.cafe.menu;

public record MenuResponse(long menuId, String name, long price, MenuStatus status) {

	public static MenuResponse from(Menu menu) {
		return new MenuResponse(menu.getId(), menu.getName(), menu.getPrice(), menu.getStatus());
	}
}
