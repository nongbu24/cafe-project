package com.example.cafe.order.dto;

import java.util.List;

public record OrderCreateRequest(List<OrderItemCreateRequest> items) {
}
