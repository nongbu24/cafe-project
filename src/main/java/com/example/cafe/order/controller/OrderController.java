package com.example.cafe.order.controller;

import com.example.cafe.common.response.ApiResponse;
import com.example.cafe.order.dto.OrderCreateRequest;
import com.example.cafe.order.dto.OrderResponse;
import com.example.cafe.order.service.OrderService;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

	private final OrderService orderService;

	public OrderController(OrderService orderService) {
		this.orderService = orderService;
	}

	@PostMapping
	public ResponseEntity<ApiResponse<OrderResponse>> order(
		@RequestBody OrderCreateRequest request
	) {
		OrderResponse response = orderService.order(request.userId(), request.menuId());

		return ResponseEntity.created(URI.create("/api/v1/orders/" + response.orderId()))
			.body(ApiResponse.success(response));
	}
}
