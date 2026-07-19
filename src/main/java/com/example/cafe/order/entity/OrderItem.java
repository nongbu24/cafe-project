package com.example.cafe.order.entity;

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
@Table(name = "order_items")
public class OrderItem {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "order_id", nullable = false)
	private Order order;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "menu_id", nullable = false)
	private Menu menu;

	@Column(nullable = false, length = 100)
	private String menuName;

	@Column(nullable = false)
	private long unitPrice;

	@Column(nullable = false)
	private int quantity;

	protected OrderItem() {
	}

	private OrderItem(Menu menu, int quantity) {
		this.menu = menu;
		this.menuName = menu.getName();
		this.unitPrice = menu.getPrice();
		this.quantity = quantity;
	}

	public static OrderItem of(Menu menu, int quantity) {
		return new OrderItem(menu, quantity);
	}

	void assignOrder(Order order) {
		this.order = order;
	}

	public Long getId() {
		return id;
	}

	public Menu getMenu() {
		return menu;
	}

	public String getMenuName() {
		return menuName;
	}

	public long getUnitPrice() {
		return unitPrice;
	}

	public int getQuantity() {
		return quantity;
	}

	public long getLineAmount() {
		return Math.multiplyExact(unitPrice, quantity);
	}
}
