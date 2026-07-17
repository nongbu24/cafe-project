package com.example.cafe.menu.service;

import com.example.cafe.common.response.PageResponse;
import com.example.cafe.menu.dto.MenuResponse;
import com.example.cafe.menu.entity.MenuStatus;
import com.example.cafe.menu.repository.MenuRepository;
import java.util.EnumSet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MenuService {

	private static final EnumSet<MenuStatus> VISIBLE_STATUSES =
		EnumSet.of(MenuStatus.AVAILABLE, MenuStatus.SOLD_OUT);

	private final MenuRepository menuRepository;

	public MenuService(MenuRepository menuRepository) {
		this.menuRepository = menuRepository;
	}

	@Transactional(readOnly = true)
	public PageResponse<MenuResponse> getMenus(int page, int size) {
		PageRequest pageRequest = PageRequest.of(page, size, Sort.by("id").ascending());
		Page<MenuResponse> menuPage = menuRepository.findAllByStatusIn(VISIBLE_STATUSES, pageRequest)
			.map(MenuResponse::from);

		return PageResponse.from(menuPage);
	}
}
