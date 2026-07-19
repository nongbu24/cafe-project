package com.example.cafe.menu.entity;

import com.example.cafe.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "menus")
public class Menu extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 100)
	private String name;

	@Column(nullable = false)
	private long price;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private MenuStatus status;

	protected Menu() {
	}

	private Menu(String name, long price, MenuStatus status) {
		this.name = name;
		this.price = price;
		this.status = status;
	}

	public static Menu create(String name, long price) {
		return new Menu(name, price, MenuStatus.AVAILABLE);
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public long getPrice() {
		return price;
	}

	public MenuStatus getStatus() {
		return status;
	}

	public void changeStatus(MenuStatus status) {
		this.status = status;
	}
}
