package com.example.cafe.menu.facade;

import com.example.cafe.common.exception.ApplicationException;
import com.example.cafe.common.exception.ErrorCode;
import com.example.cafe.menu.entity.Menu;
import com.example.cafe.menu.entity.MenuStatus;
import com.example.cafe.menu.repository.MenuRepository;
import org.springframework.stereotype.Service;

@Service
public class MenuFacade {

	private final MenuRepository menuRepository;

	public MenuFacade(MenuRepository menuRepository) {
		this.menuRepository = menuRepository;
	}

	public Menu getMenu(long menuId) {
		return menuRepository.findById(menuId)
			.orElseThrow(() -> new ApplicationException(ErrorCode.MENU_NOT_FOUND));
	}

	public Menu getOrderableMenu(long menuId) {
		Menu menu = getMenu(menuId);

		if (menu.getStatus() != MenuStatus.AVAILABLE) {
			throw new ApplicationException(ErrorCode.MENU_NOT_AVAILABLE);
		}

		return menu;
	}
}
