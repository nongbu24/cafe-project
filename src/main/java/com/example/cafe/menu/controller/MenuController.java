package com.example.cafe.menu.controller;

import com.example.cafe.common.exception.ApplicationException;
import com.example.cafe.common.exception.ErrorCode;
import com.example.cafe.common.response.ApiResponse;
import com.example.cafe.common.response.PageResponse;
import com.example.cafe.menu.dto.MenuResponse;
import com.example.cafe.menu.service.MenuService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/menus")
public class MenuController {

	private final MenuService menuService;

	public MenuController(MenuService menuService) {
		this.menuService = menuService;
	}

	@GetMapping
	public ApiResponse<PageResponse<MenuResponse>> getMenus(
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "10") int size
	) {
		validatePagination(page, size);

		return ApiResponse.success(menuService.getMenus(page, size));
	}

	private void validatePagination(int page, int size) {
		if (page < 0) {
			throw new ApplicationException(ErrorCode.INVALID_REQUEST, "page는 0 이상이어야 합니다.");
		}

		if (size < 1 || size > 100) {
			throw new ApplicationException(ErrorCode.INVALID_REQUEST, "size는 1 이상 100 이하여야 합니다.");
		}
	}
}
