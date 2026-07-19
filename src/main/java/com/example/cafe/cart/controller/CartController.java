package com.example.cafe.cart.controller;

import com.example.cafe.auth.service.AuthenticationInterceptor;
import com.example.cafe.cart.dto.CartResponse;
import com.example.cafe.cart.dto.CartItemQuantityUpdateRequest;
import com.example.cafe.cart.dto.CartItemResponse;
import com.example.cafe.cart.service.CartService;
import com.example.cafe.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/carts/me")
public class CartController {

	private final CartService cartService;

	public CartController(CartService cartService) {
		this.cartService = cartService;
	}

	@GetMapping
	public ApiResponse<CartResponse> getCart(HttpServletRequest httpServletRequest) {
		long userId = (long) httpServletRequest.getAttribute(AuthenticationInterceptor.AUTHENTICATED_USER_ID);

		return ApiResponse.success("SUCCESS", "장바구니 조회가 완료되었습니다.", cartService.getCart(userId));
	}

	@PatchMapping("/items/{menuId}")
	public ApiResponse<CartItemResponse> updateItemQuantity(
		@PathVariable long menuId,
		@RequestBody CartItemQuantityUpdateRequest request,
		HttpServletRequest httpServletRequest
	) {
		long userId = (long) httpServletRequest.getAttribute(AuthenticationInterceptor.AUTHENTICATED_USER_ID);
		CartItemResponse response = cartService.updateItemQuantity(
			userId,
			menuId,
			request == null ? null : request.quantity()
		);

		if (response == null) {
			return ApiResponse.success("SUCCESS", "장바구니 항목이 삭제되었습니다.", null);
		}

		return ApiResponse.success("SUCCESS", "장바구니 수량이 변경되었습니다.", response);
	}

	@DeleteMapping("/items")
	public ApiResponse<Void> clear(HttpServletRequest httpServletRequest) {
		long userId = (long) httpServletRequest.getAttribute(AuthenticationInterceptor.AUTHENTICATED_USER_ID);
		cartService.clear(userId);

		return ApiResponse.successMessage("장바구니가 비워졌습니다.");
	}
}
