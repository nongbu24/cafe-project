package com.example.cafe.order.service;

import com.example.cafe.common.exception.ApplicationException;
import com.example.cafe.common.util.DateTimeUtils;
import com.example.cafe.menu.entity.Menu;
import com.example.cafe.menu.facade.MenuFacade;
import com.example.cafe.order.dto.OrderCreateRequest;
import com.example.cafe.order.dto.OrderItemCreateRequest;
import com.example.cafe.order.dto.OrderItemResponse;
import com.example.cafe.order.dto.OrderResponse;
import com.example.cafe.order.entity.Order;
import com.example.cafe.order.entity.OrderEventOutbox;
import com.example.cafe.order.entity.OrderItem;
import com.example.cafe.order.repository.OrderEventOutboxRepository;
import com.example.cafe.order.repository.OrderRepository;
import com.example.cafe.point.facade.PointFacade;
import com.example.cafe.user.entity.User;
import com.example.cafe.user.facade.UserFacade;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

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
	public OrderResponse order(long userId, OrderCreateRequest request) {
		validate(userId, request);

		User user = userFacade.getActiveUserForUpdate(userId);
		List<OrderItem> items = createOrderItems(request);
		long paymentAmount = calculatePaymentAmount(items);

		LocalDateTime paidAt = DateTimeUtils.utcNow();
		user.usePoint(paymentAmount);

		Order order = orderRepository.save(Order.paid(user, items, paymentAmount, paidAt));
		pointFacade.savePaymentTransaction(
			user,
			order.getId(),
			order.getPaymentAmount(),
			user.getPointBalance(),
			paidAt
		);

		OrderEventOutbox event = orderEventOutboxRepository.saveAndFlush(OrderEventOutbox.orderPaid(order));
		event.updatePayload(orderPaidPayload(event, order, user));
		applicationEventPublisher.publishEvent(new OrderPaidOutboxEvent(event.getId()));

		return new OrderResponse(
			order.getId(),
			user.getId(),
			toItemResponses(order.getItems()),
			order.getPaymentAmount(),
			user.getPointBalance(),
			order.getStatus().name(),
			DateTimeUtils.toKoreaOffsetDateTime(paidAt)
		);
	}

	private void validate(long userId, OrderCreateRequest request) {
		if (userId < 1) {
			throw ApplicationException.invalidRequest("userId는 1 이상이어야 합니다.");
		}

		if (request == null || request.items() == null || request.items().isEmpty()) {
			throw ApplicationException.invalidRequest("주문 항목은 1개 이상이어야 합니다.");
		}

		for (OrderItemCreateRequest item : request.items()) {
			if (item == null) {
				throw ApplicationException.invalidRequest("주문 항목은 비어 있을 수 없습니다.");
			}

			if (item.menuId() < 1) {
				throw ApplicationException.invalidRequest("menuId는 1 이상이어야 합니다.");
			}

			if (item.quantity() < 1) {
				throw ApplicationException.invalidRequest("주문 항목은 1개 이상이어야 합니다.");
			}
		}
	}

	private List<OrderItem> createOrderItems(OrderCreateRequest request) {
		Map<Long, Integer> quantitiesByMenuId = new LinkedHashMap<>();
		for (OrderItemCreateRequest item : request.items()) {
			quantitiesByMenuId.merge(item.menuId(), item.quantity(), this::addQuantity);
		}

		List<OrderItem> items = new ArrayList<>();
		for (Map.Entry<Long, Integer> entry : quantitiesByMenuId.entrySet()) {
			Menu menu = menuFacade.getOrderableMenu(entry.getKey());
			items.add(OrderItem.of(menu, entry.getValue()));
		}

		return items;
	}

	private int addQuantity(int current, int added) {
		try {
			return Math.addExact(current, added);
		} catch (ArithmeticException exception) {
			throw ApplicationException.invalidRequest("주문 수량이 허용 범위를 초과합니다.");
		}
	}

	private long calculatePaymentAmount(List<OrderItem> items) {
		long paymentAmount = 0;
		try {
			for (OrderItem item : items) {
				paymentAmount = Math.addExact(paymentAmount, item.getLineAmount());
			}
			return paymentAmount;
		} catch (ArithmeticException exception) {
			throw ApplicationException.invalidRequest("주문 금액이 허용 범위를 초과합니다.");
		}
	}

	private List<OrderItemResponse> toItemResponses(List<OrderItem> items) {
		return items.stream()
			.map(item -> new OrderItemResponse(
				item.getMenu().getId(),
				item.getMenuName(),
				item.getUnitPrice(),
				item.getQuantity(),
				item.getLineAmount()
			))
			.toList();
	}

	private String orderPaidPayload(OrderEventOutbox event, Order order, User user) {
		return """
			{"eventId":%d,"eventType":"ORDER_PAID","occurredAt":"%s","userId":%d,"items":[%s],"paymentAmount":%d}
			""".formatted(
			event.getId(),
			DateTimeUtils.toKoreaOffsetDateTime(order.getPaidAt()),
			user.getId(),
			orderItemsPayload(order.getItems()),
			order.getPaymentAmount()
		).trim();
	}

	private String orderItemsPayload(List<OrderItem> items) {
		return items.stream()
			.map(item -> """
				{"menuId":%d,"quantity":%d}
				""".formatted(item.getMenu().getId(), item.getQuantity()).trim())
			.toList()
			.stream()
			.reduce((left, right) -> left + "," + right)
			.orElse("");
	}
}
