package com.example.cafe.order.facade;

import com.example.cafe.menu.dto.MenuOrderCount;
import com.example.cafe.order.repository.OrderRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class OrderFacade {

	private final OrderRepository orderRepository;

	public OrderFacade(OrderRepository orderRepository) {
		this.orderRepository = orderRepository;
	}

	public List<MenuOrderCount> countOrdersByMenuIds(List<Long> menuIds) {
		return orderRepository.countOrdersByMenuIds(menuIds);
	}
}
