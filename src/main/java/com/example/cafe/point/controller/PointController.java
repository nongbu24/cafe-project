package com.example.cafe.point.controller;

import com.example.cafe.auth.service.AuthenticationInterceptor;
import com.example.cafe.common.response.ApiResponse;
import com.example.cafe.point.dto.PointChargeRequest;
import com.example.cafe.point.dto.PointChargeResponse;
import com.example.cafe.point.service.PointService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/me/point-charges")
public class PointController {

	private final PointService pointService;

	public PointController(PointService pointService) {
		this.pointService = pointService;
	}

	@PostMapping
	public ApiResponse<PointChargeResponse> charge(
		@RequestBody @Valid PointChargeRequest request,
		HttpServletRequest httpServletRequest
	) {
		long userId = (long) httpServletRequest.getAttribute(AuthenticationInterceptor.AUTHENTICATED_USER_ID);

		return ApiResponse.success(pointService.charge(userId, request.amount()));
	}
}
