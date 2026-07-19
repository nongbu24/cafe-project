package com.example.cafe.order.repository;

import com.example.cafe.menu.dto.PopularMenuOrderCount;
import com.example.cafe.menu.dto.MenuOrderCount;
import com.example.cafe.order.entity.OrderStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface OrderRepositoryCustom {

	List<PopularMenuOrderCount> findPopularMenus(
		OrderStatus status,
		LocalDateTime from,
		LocalDateTime to,
		Pageable pageable
	);

	List<MenuOrderCount> countOrdersByMenuIds(List<Long> menuIds);
}
