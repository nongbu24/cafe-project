package com.example.cafe.cart.entity;

import com.example.cafe.common.entity.BaseEntity;
import com.example.cafe.menu.entity.Menu;
import com.example.cafe.user.entity.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "carts")
public class Cart extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false, unique = true)
	private User user;

	@OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<CartItem> items = new ArrayList<>();

	protected Cart() {
	}

	private Cart(User user) {
		this.user = user;
	}

	public static Cart create(User user) {
		return new Cart(user);
	}

	public Long getId() {
		return id;
	}

	public User getUser() {
		return user;
	}

	public List<CartItem> getItems() {
		return List.copyOf(items);
	}

	public CartItem updateItemQuantity(Menu menu, int quantity) {
		CartItem item = findItem(menu.getId());

		if (item == null) {
			item = CartItem.of(this, menu, quantity);
			items.add(item);

			return item;
		}

		item.changeQuantity(quantity);
		return item;
	}

	public void removeItem(long menuId) {
		items.removeIf(item -> item.hasMenuId(menuId));
	}

	public void clear() {
		items.clear();
	}

	private CartItem findItem(long menuId) {
		return items.stream()
			.filter(item -> item.hasMenuId(menuId))
			.findFirst()
			.orElse(null);
	}
}
