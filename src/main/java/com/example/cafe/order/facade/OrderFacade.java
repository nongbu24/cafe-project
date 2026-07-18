package com.example.cafe.order.facade;

import com.example.cafe.menu.dto.PopularMenuOrderCount;
import com.example.cafe.order.entity.OrderStatus;
import com.example.cafe.order.repository.OrderRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class OrderFacade {

	private final OrderRepository orderRepository;

	public OrderFacade(OrderRepository orderRepository) {
		this.orderRepository = orderRepository;
	}

	public List<PopularMenuOrderCount> findPopularMenus(
		OrderStatus status,
		LocalDateTime from,
		LocalDateTime to,
		Pageable pageable
	) {
		return orderRepository.findPopularMenus(status, from, to, pageable);
	}
}
