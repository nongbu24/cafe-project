package com.example.cafe.menu.dto;

public record PopularMenuItemResponse(
	int rank,
	long menuId,
	String name,
	long price
) {
}
