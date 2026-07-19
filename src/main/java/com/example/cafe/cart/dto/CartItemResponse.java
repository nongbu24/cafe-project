package com.example.cafe.cart.dto;

public record CartItemResponse(long menuId, String name, long unitPrice, int quantity, long lineAmount) {
}
