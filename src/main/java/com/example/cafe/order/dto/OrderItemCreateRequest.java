package com.example.cafe.order.dto;

public record OrderItemCreateRequest(long menuId, int quantity) {
}
