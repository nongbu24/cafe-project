package com.example.cafe.cart.entity;

import com.example.cafe.common.entity.BaseEntity;
import com.example.cafe.menu.entity.Menu;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "cart_items")
public class CartItem extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "cart_id", nullable = false)
	private Cart cart;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "menu_id", nullable = false)
	private Menu menu;

	@Column(nullable = false)
	private int quantity;

	protected CartItem() {
	}

	private CartItem(Cart cart, Menu menu, int quantity) {
		this.cart = cart;
		this.menu = menu;
		this.quantity = quantity;
	}

	public static CartItem of(Cart cart, Menu menu, int quantity) {
		return new CartItem(cart, menu, quantity);
	}

	public Long getId() {
		return id;
	}

	public Cart getCart() {
		return cart;
	}

	public Menu getMenu() {
		return menu;
	}

	public int getQuantity() {
		return quantity;
	}

	void changeQuantity(int quantity) {
		this.quantity = quantity;
	}

	boolean hasMenuId(long menuId) {
		return menu.getId() == menuId;
	}
}
