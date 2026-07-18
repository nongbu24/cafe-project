package com.example.cafe.order.service;

import com.example.cafe.common.exception.ApplicationException;
import com.example.cafe.common.exception.ErrorCode;
import com.example.cafe.menu.entity.Menu;
import com.example.cafe.menu.repository.MenuRepository;
import com.example.cafe.order.dto.OrderMenuResponse;
import com.example.cafe.order.dto.OrderResponse;
import com.example.cafe.order.entity.Order;
import com.example.cafe.order.entity.OrderEventOutbox;
import com.example.cafe.order.repository.OrderEventOutboxRepository;
import com.example.cafe.order.repository.OrderRepository;
import com.example.cafe.point.entity.PointTransaction;
import com.example.cafe.point.repository.PointTransactionRepository;
import com.example.cafe.user.entity.User;
import com.example.cafe.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

	private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");

	private final UserRepository userRepository;
	private final MenuRepository menuRepository;
	private final OrderRepository orderRepository;
	private final PointTransactionRepository pointTransactionRepository;
	private final OrderEventOutboxRepository orderEventOutboxRepository;
	private final ApplicationEventPublisher applicationEventPublisher;

	public OrderService(
		UserRepository userRepository,
		MenuRepository menuRepository,
		OrderRepository orderRepository,
		PointTransactionRepository pointTransactionRepository,
		OrderEventOutboxRepository orderEventOutboxRepository,
		ApplicationEventPublisher applicationEventPublisher
	) {
		this.userRepository = userRepository;
		this.menuRepository = menuRepository;
		this.orderRepository = orderRepository;
		this.pointTransactionRepository = pointTransactionRepository;
		this.orderEventOutboxRepository = orderEventOutboxRepository;
		this.applicationEventPublisher = applicationEventPublisher;
	}

	@Transactional
	public OrderResponse order(long userId, long menuId) {
		validate(userId, menuId);

		User user = userRepository.findByIdForUpdate(userId)
			.orElseThrow(() -> new ApplicationException(ErrorCode.USER_NOT_FOUND));
		Menu menu = menuRepository.findById(menuId)
			.orElseThrow(() -> new ApplicationException(ErrorCode.MENU_NOT_FOUND));

		LocalDateTime paidAt = LocalDateTime.now();
		user.usePoint(menu.getPrice());

		Order order = orderRepository.save(Order.paid(user, menu, paidAt));
		pointTransactionRepository.save(PointTransaction.payment(
			user,
			order.getId(),
			order.getPaymentAmount(),
			user.getPointBalance(),
			paidAt
		));

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
