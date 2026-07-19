package com.example.cafe.order.repository;

import static com.example.cafe.order.entity.QOrderItem.orderItem;

import com.example.cafe.menu.dto.MenuOrderCount;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;

public class OrderRepositoryImpl implements OrderRepositoryCustom {

	private final JPAQueryFactory queryFactory;

	public OrderRepositoryImpl(JPAQueryFactory queryFactory) {
		this.queryFactory = queryFactory;
	}

	@Override
	public List<MenuOrderCount> countOrdersByMenuIds(List<Long> menuIds) {
		if (menuIds.isEmpty()) {
			return List.of();
		}

		return queryFactory
			.select(Projections.constructor(
				MenuOrderCount.class,
				orderItem.menu.id,
				orderItem.quantity.sum().longValue()
			))
			.from(orderItem)
			.where(orderItem.menu.id.in(menuIds))
			.groupBy(orderItem.menu.id)
			.fetch();
	}
}
