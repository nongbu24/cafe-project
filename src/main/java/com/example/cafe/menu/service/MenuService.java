package com.example.cafe.menu.service;

import com.example.cafe.common.exception.ApplicationException;
import com.example.cafe.common.exception.ErrorCode;
import com.example.cafe.common.response.PageResponse;
import com.example.cafe.common.util.DateTimeUtils;
import com.example.cafe.menu.dto.AdminMenuResponse;
import com.example.cafe.menu.dto.MenuCreateRequest;
import com.example.cafe.menu.dto.MenuOrderCount;
import com.example.cafe.menu.dto.MenuResponse;
import com.example.cafe.menu.dto.MenuStatusUpdateRequest;
import com.example.cafe.menu.dto.PopularMenuItemResponse;
import com.example.cafe.menu.dto.PopularMenuOrderCount;
import com.example.cafe.menu.dto.PopularMenuResponse;
import com.example.cafe.menu.entity.Menu;
import com.example.cafe.menu.entity.MenuStatus;
import com.example.cafe.menu.repository.MenuRepository;
import com.example.cafe.order.entity.OrderStatus;
import com.example.cafe.order.facade.OrderFacade;
import com.example.cafe.user.entity.User;
import com.example.cafe.user.entity.UserStatus;
import com.example.cafe.user.facade.UserFacade;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MenuService {

	private static final int POPULAR_MENU_LIMIT = 3;
	private static final int POPULAR_MENU_PERIOD_DAYS = 7;
	private static final EnumSet<MenuStatus> VISIBLE_STATUSES =
		EnumSet.of(MenuStatus.AVAILABLE, MenuStatus.SOLD_OUT);

	private final MenuRepository menuRepository;
	private final OrderFacade orderFacade;
	private final UserFacade userFacade;

	public MenuService(MenuRepository menuRepository, OrderFacade orderFacade, UserFacade userFacade) {
		this.menuRepository = menuRepository;
		this.orderFacade = orderFacade;
		this.userFacade = userFacade;
	}

	@Transactional(readOnly = true)
	public PageResponse<MenuResponse> getMenus(int page, int size) {
		PageRequest pageRequest = PageRequest.of(page, size, Sort.by("id").ascending());
		Page<MenuResponse> menuPage = menuRepository.findAllByStatusIn(VISIBLE_STATUSES, pageRequest)
			.map(MenuResponse::from);

		return PageResponse.from(menuPage);
	}

	@Transactional
	public MenuResponse createMenuForAdmin(long adminUserId, MenuCreateRequest request) {
		validateAdmin(adminUserId);
		if (request == null) {
			throw ApplicationException.invalidRequest();
		}

		Menu menu = menuRepository.save(Menu.create(request.name(), request.price()));

		return MenuResponse.from(menu);
	}

	@Transactional
	public MenuResponse updateMenuStatusForAdmin(
		long adminUserId,
		long menuId,
		MenuStatusUpdateRequest request
	) {
		validateAdmin(adminUserId);
		if (request == null) {
			throw ApplicationException.invalidRequest();
		}

		Menu menu = menuRepository.findById(menuId)
			.orElseThrow(() -> new ApplicationException(ErrorCode.MENU_NOT_FOUND));
		menu.changeStatus(request.status());

		return MenuResponse.from(menu);
	}

	@Transactional(readOnly = true)
	public PageResponse<AdminMenuResponse> getMenusForAdmin(
		long adminUserId,
		int page,
		int size,
		MenuStatus status
	) {
		validateAdmin(adminUserId);
		PageRequest pageRequest = PageRequest.of(page, size, Sort.by("id").ascending());
		Page<Menu> menuPage;

		if (status != null) {
			menuPage = menuRepository.findAllByStatus(status, pageRequest);
		} else {
			menuPage = menuRepository.findAll(pageRequest);
		}

		return PageResponse.from(toAdminMenuResponsePage(menuPage, pageRequest));
	}

	@Transactional(readOnly = true)
	public PopularMenuResponse getPopularMenus() {
		LocalDateTime to = DateTimeUtils.utcNow();
		LocalDateTime from = to.minusDays(POPULAR_MENU_PERIOD_DAYS);
		List<PopularMenuOrderCount> orderCounts = orderFacade.findPopularMenus(
			OrderStatus.PAID,
			from,
			to,
			PageRequest.of(0, POPULAR_MENU_LIMIT)
		);

		return new PopularMenuResponse(
			DateTimeUtils.toKoreaOffsetDateTime(from),
			DateTimeUtils.toKoreaOffsetDateTime(to),
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
				orderCount.price()
			));
		}

		return items;
	}

	private Page<AdminMenuResponse> toAdminMenuResponsePage(Page<Menu> menuPage, PageRequest pageRequest) {
		List<Menu> menus = menuPage.getContent();
		List<Long> menuIds = menus.stream()
			.map(Menu::getId)
			.toList();
		Map<Long, Long> orderCounts = orderFacade.countOrdersByMenuIds(menuIds).stream()
			.collect(Collectors.toMap(MenuOrderCount::menuId, MenuOrderCount::orderCount));
		List<AdminMenuResponse> responses = menus.stream()
			.map(menu -> AdminMenuResponse.of(menu, orderCounts.getOrDefault(menu.getId(), 0L)))
			.toList();

		return new PageImpl<>(responses, pageRequest, menuPage.getTotalElements());
	}

	private void validateAdmin(long userId) {
		User user = userFacade.getActiveUser(userId);
		if (user.getUserStatus() != UserStatus.ADMIN) {
			throw new ApplicationException(ErrorCode.FORBIDDEN);
		}
	}
}
