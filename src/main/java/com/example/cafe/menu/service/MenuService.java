package com.example.cafe.menu.service;

import com.example.cafe.common.response.PageResponse;
import com.example.cafe.menu.dto.MenuResponse;
import com.example.cafe.menu.dto.PopularMenuItemResponse;
import com.example.cafe.menu.dto.PopularMenuOrderCount;
import com.example.cafe.menu.dto.PopularMenuResponse;
import com.example.cafe.menu.entity.MenuStatus;
import com.example.cafe.menu.repository.MenuRepository;
import com.example.cafe.order.entity.OrderStatus;
import com.example.cafe.order.repository.OrderRepository;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MenuService {

	private static final int POPULAR_MENU_LIMIT = 3;
	private static final int POPULAR_MENU_PERIOD_DAYS = 7;
	private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");
	private static final EnumSet<MenuStatus> VISIBLE_STATUSES =
		EnumSet.of(MenuStatus.AVAILABLE, MenuStatus.SOLD_OUT);

	private final MenuRepository menuRepository;
	private final OrderRepository orderRepository;

	public MenuService(MenuRepository menuRepository, OrderRepository orderRepository) {
		this.menuRepository = menuRepository;
		this.orderRepository = orderRepository;
	}

	@Transactional(readOnly = true)
	public PageResponse<MenuResponse> getMenus(int page, int size) {
		PageRequest pageRequest = PageRequest.of(page, size, Sort.by("id").ascending());
		Page<MenuResponse> menuPage = menuRepository.findAllByStatusIn(VISIBLE_STATUSES, pageRequest)
			.map(MenuResponse::from);

		return PageResponse.from(menuPage);
	}

	@Transactional(readOnly = true)
	public PopularMenuResponse getPopularMenus() {
		LocalDateTime to = LocalDateTime.now();
		LocalDateTime from = to.minusDays(POPULAR_MENU_PERIOD_DAYS);
		List<PopularMenuOrderCount> orderCounts = orderRepository.findPopularMenus(
			OrderStatus.PAID,
			from,
			to,
			PageRequest.of(0, POPULAR_MENU_LIMIT)
		);

		return new PopularMenuResponse(
			toOffsetDateTime(from),
			toOffsetDateTime(to),
			toPopularMenuItems(orderCounts)
		);
	}

	private List<PopularMenuItemResponse> toPopularMenuItems(List<PopularMenuOrderCount> orderCounts) {
		List<PopularMenuItemResponse> items = new ArrayList<>();

		for (int index = 0; index < orderCounts.size(); index++) {
			PopularMenuOrderCount orderCount = orderCounts.get(index);
			items.add(new PopularMenuItemResponse(
				index + 1,
				orderCount.menuId(),
				orderCount.name(),
				orderCount.price(),
				orderCount.orderCount()
			));
		}

		return items;
	}

	private OffsetDateTime toOffsetDateTime(LocalDateTime dateTime) {
		return dateTime.atZone(KOREA_ZONE).toOffsetDateTime();
	}
}
