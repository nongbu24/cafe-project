package com.example.cafe.cart.service;

import com.example.cafe.cart.dto.CartResponse;
import com.example.cafe.cart.dto.CartItemResponse;
import com.example.cafe.cart.entity.Cart;
import com.example.cafe.cart.entity.CartItem;
import com.example.cafe.cart.repository.CartRepository;
import com.example.cafe.common.exception.ApplicationException;
import com.example.cafe.common.exception.ErrorCode;
import com.example.cafe.menu.entity.Menu;
import com.example.cafe.menu.facade.MenuFacade;
import com.example.cafe.user.entity.User;
import com.example.cafe.user.facade.UserFacade;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CartService {

	private final CartRepository cartRepository;
	private final UserFacade userFacade;
	private final MenuFacade menuFacade;

	public CartService(CartRepository cartRepository, UserFacade userFacade, MenuFacade menuFacade) {
		this.cartRepository = cartRepository;
		this.userFacade = userFacade;
		this.menuFacade = menuFacade;
	}

	@Transactional
	public CartResponse getCart(long userId) {
		Cart cart = getOrCreateCart(userId);
		var items = cart.getItems().stream()
			.map(this::toResponse)
			.toList();
		long totalAmount = items.stream()
			.mapToLong(CartItemResponse::lineAmount)
			.sum();

		return new CartResponse(cart.getUser().getId(), items, totalAmount);
	}

	@Transactional
	public CartItemResponse updateItemQuantity(long userId, long menuId, Integer quantity) {
		validateMenuId(menuId);
		validateQuantity(quantity);

		Cart cart = getOrCreateCart(userId);

		if (quantity == 0) {
			cart.removeItem(menuId);

			return null;
		}

		Menu menu = menuFacade.getMenu(menuId);
		CartItem item = cart.updateItemQuantity(menu, quantity);

		return toResponse(item);
	}

	@Transactional
	public void clear(long userId) {
		Cart cart = getOrCreateCart(userId);
		cart.clear();
	}

	private Cart getOrCreateCart(long userId) {
		User user = userFacade.getActiveUser(userId);

		return cartRepository.findByUserId(user.getId())
			.orElseGet(() -> cartRepository.save(Cart.create(user)));
	}

	private void validateMenuId(long menuId) {
		if (menuId < 1) {
			throw new ApplicationException(ErrorCode.MENU_NOT_FOUND);
		}
	}

	private void validateQuantity(Integer quantity) {
		if (quantity == null) {
			throw ApplicationException.invalidRequest("수량은 필수로 입력해야 합니다.");
		}

		if (quantity < 0) {
			throw ApplicationException.invalidRequest("수량은 0 이상이어야 합니다.");
		}
	}

	private CartItemResponse toResponse(CartItem item) {
		long lineAmount = Math.multiplyExact(item.getMenu().getPrice(), item.getQuantity());

		return new CartItemResponse(
			item.getMenu().getId(),
			item.getMenu().getName(),
			item.getMenu().getPrice(),
			item.getQuantity(),
			lineAmount
		);
	}
}
