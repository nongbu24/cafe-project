package com.example.cafe.point.controller;

import com.example.cafe.common.response.ApiResponse;
import com.example.cafe.point.dto.PortOnePaymentWebhookRequest;
import com.example.cafe.point.dto.PortOnePaymentWebhookResponse;
import com.example.cafe.point.service.PointService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/portone/webhooks/payments")
public class PortOneWebhookController {

	private final PointService pointService;

	public PortOneWebhookController(PointService pointService) {
		this.pointService = pointService;
	}

	@PostMapping
	public ApiResponse<PortOnePaymentWebhookResponse> handlePaymentWebhook(
		@RequestBody @Valid PortOnePaymentWebhookRequest request
	) {
		return ApiResponse.success(pointService.handlePortOnePaymentWebhook(request));
	}
}
