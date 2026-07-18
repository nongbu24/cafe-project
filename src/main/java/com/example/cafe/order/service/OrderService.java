package com.example.cafe.order.service;

import com.example.cafe.common.exception.ApplicationException;
import com.example.cafe.common.exception.ErrorCode;
import com.example.cafe.menu.entity.Menu;
import com.example.cafe.menu.facade.MenuFacade;
import com.example.cafe.order.dto.OrderMenuResponse;
import com.example.cafe.order.dto.OrderResponse;
import com.example.cafe.order.entity.Order;
import com.example.cafe.order.entity.OrderEventOutbox;
import com.example.cafe.order.repository.OrderEventOutboxRepository;
import com.example.cafe.order.repository.OrderRepository;
import com.example.cafe.point.facade.PointFacade;
import com.example.cafe.user.entity.User;
import com.example.cafe.user.facade.UserFacade;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

	private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");

	private final UserFacade userFacade;
	private final MenuFacade menuFacade;
	private final OrderRepository orderRepository;
	private final PointFacade pointFacade;
	private final OrderEventOutboxRepository orderEventOutboxRepository;
	private final ApplicationEventPublisher applicationEventPublisher;

	public OrderService(
		UserFacade userFacade,
		MenuFacade menuFacade,
		OrderRepository orderRepository,
		PointFacade pointFacade,
		OrderEventOutboxRepository orderEventOutboxRepository,
		ApplicationEventPublisher applicationEventPublisher
	) {
		this.userFacade = userFacade;
		this.menuFacade = menuFacade;
		this.orderRepository = orderRepository;
		this.pointFacade = pointFacade;
		this.orderEventOutboxRepository = orderEventOutboxRepository;
		this.applicationEventPublisher = applicationEventPublisher;
	}

	@Transactional
	public OrderResponse order(long userId, long menuId) {
		validate(userId, menuId);

		User user = userFacade.getUserForUpdate(userId);
		Menu menu = menuFacade.getMenu(menuId);

		LocalDateTime paidAt = LocalDateTime.now();
		user.usePoint(menu.getPrice());

		Order order = orderRepository.save(Order.paid(user, menu, paidAt));
		pointFacade.savePaymentTransaction(
			user,
			order.getId(),
			order.getPaymentAmount(),
			user.getPointBalance(),
			paidAt
		);

		OrderEventOutbox event = orderEventOutboxRepository.save(OrderEventOutbox.orderPaid(
			order,
			orderPaidPayload(order, user, menu)
		));
		applicationEventPublisher.publishEvent(event);

		return new OrderResponse(
			order.getId(),
			user.getId(),
			new OrderMenuResponse(menu.getId(), order.getMenuName()),
			order.getPaymentAmount(),
			user.getPointBalance(),
			order.getStatus().name(),
			toOffsetDateTime(paidAt)
		);
	}

	private void validate(long userId, long menuId) {
		if (userId < 1) {
			throw new ApplicationException(ErrorCode.INVALID_REQUEST, "userId는 1 이상이어야 합니다.");
		}

		if (menuId < 1) {
			throw new ApplicationException(ErrorCode.INVALID_REQUEST, "menuId는 1 이상이어야 합니다.");
		}
	}

	private String orderPaidPayload(Order order, User user, Menu menu) {
		return """
			{"eventType":"ORDER_PAID","userId":%d,"menuId":%d,"paymentAmount":%d}
			""".formatted(user.getId(), menu.getId(), order.getPaymentAmount()).trim();
	}

	private OffsetDateTime toOffsetDateTime(LocalDateTime dateTime) {
		return dateTime.atZone(KOREA_ZONE).toOffsetDateTime();
	}
}
