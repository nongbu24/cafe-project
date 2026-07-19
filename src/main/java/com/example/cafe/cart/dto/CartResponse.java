package com.example.cafe.cart.dto;

import java.util.List;

public record CartResponse(long userId, List<CartItemResponse> items, long totalAmount) {
}
