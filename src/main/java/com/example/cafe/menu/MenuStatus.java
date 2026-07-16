package com.example.cafe.menu;

public enum MenuStatus {
	AVAILABLE("판매중"),
	SOLD_OUT("품절"),
	DISCONTINUED("단종");

	private final String description;

	MenuStatus(String description) {
		this.description = description;
	}

	public String getDescription() {
		return description;
	}
}
