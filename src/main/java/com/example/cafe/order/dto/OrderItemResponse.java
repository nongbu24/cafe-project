package com.example.cafe.order.dto;

public record OrderItemResponse(long menuId, String name, long unitPrice, int quantity, long lineAmount) {
}
