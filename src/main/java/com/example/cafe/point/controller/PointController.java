package com.example.cafe.point.controller;

import com.example.cafe.common.response.ApiResponse;
import com.example.cafe.point.dto.PointChargeRequest;
import com.example.cafe.point.dto.PointChargeResponse;
import com.example.cafe.point.service.PointService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/{userId}/point-charges")
public class PointController {

	private final PointService pointService;

	public PointController(PointService pointService) {
		this.pointService = pointService;
	}

	@PostMapping
	public ApiResponse<PointChargeResponse> charge(
		@PathVariable long userId,
		@RequestBody PointChargeRequest request
	) {
		return ApiResponse.success(pointService.charge(userId, request.amount()));
	}
}
