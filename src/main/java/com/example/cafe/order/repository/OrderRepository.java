package com.example.cafe.order.repository;

import com.example.cafe.menu.dto.PopularMenuOrderCount;
import com.example.cafe.order.entity.Order;
import com.example.cafe.order.entity.OrderStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OrderRepository extends JpaRepository<Order, Long> {

	@Query("""
		SELECT new com.example.cafe.menu.dto.PopularMenuOrderCount(
			menu.id,
			menu.name,
			menu.price,
			COUNT(o)
		)
		FROM Order o
		JOIN o.menu menu
		WHERE o.status = :status
			AND o.paidAt >= :from
			AND o.paidAt < :to
		GROUP BY menu.id, menu.name, menu.price
		ORDER BY COUNT(o) DESC, menu.id ASC
		""")
	List<PopularMenuOrderCount> findPopularMenus(
		OrderStatus status,
		LocalDateTime from,
		LocalDateTime to,
		Pageable pageable
	);
}
