package com.example.cafe.order.repository;

import static com.example.cafe.menu.entity.QMenu.menu;
import static com.example.cafe.order.entity.QOrder.order;

import com.example.cafe.menu.dto.MenuOrderCount;
import com.example.cafe.menu.dto.PopularMenuOrderCount;
import com.example.cafe.order.entity.OrderStatus;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;

public class OrderRepositoryImpl implements OrderRepositoryCustom {

	private final JPAQueryFactory queryFactory;

	public OrderRepositoryImpl(JPAQueryFactory queryFactory) {
		this.queryFactory = queryFactory;
	}

	@Override
	public List<PopularMenuOrderCount> findPopularMenus(
		OrderStatus status,
		LocalDateTime from,
		LocalDateTime to,
		Pageable pageable
	) {
		return queryFactory
			.select(Projections.constructor(
				PopularMenuOrderCount.class,
				menu.id,
				menu.name,
				menu.price,
				order.count()
			))
			.from(order)
			.join(order.menu, menu)
			.where(
				order.status.eq(status),
				order.paidAt.goe(from),
				order.paidAt.lt(to)
			)
			.groupBy(menu.id, menu.name, menu.price)
			.orderBy(order.count().desc(), menu.id.asc())
			.offset(pageable.getOffset())
			.limit(pageable.getPageSize())
			.fetch();
	}

	@Override
	public List<MenuOrderCount> countOrdersByMenuIds(List<Long> menuIds) {
		if (menuIds.isEmpty()) {
			return List.of();
		}

		return queryFactory
			.select(Projections.constructor(
				MenuOrderCount.class,
				order.menu.id,
				order.count()
			))
			.from(order)
			.where(order.menu.id.in(menuIds))
			.groupBy(order.menu.id)
			.fetch();
	}
}
