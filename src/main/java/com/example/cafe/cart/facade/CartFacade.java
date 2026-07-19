package com.example.cafe.cart.facade;

import com.example.cafe.cart.entity.Cart;
import com.example.cafe.cart.entity.CartItem;
import com.example.cafe.cart.repository.CartRepository;
import com.example.cafe.user.entity.User;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CartFacade {

	private final CartRepository cartRepository;

	public CartFacade(CartRepository cartRepository) {
		this.cartRepository = cartRepository;
	}

	public void createFor(User user) {
		cartRepository.save(Cart.create(user));
	}

	public void clearFor(User user) {
		cartRepository.findByUserId(user.getId())
			.ifPresent(Cart::clear);
	}

	public List<CartItem> getItems(User user) {
		return cartRepository.findByUserId(user.getId())
			.map(Cart::getItems)
			.orElse(List.of());
	}
}
