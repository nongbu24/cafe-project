package com.example.cafe.menu.controller;

import com.example.cafe.auth.service.AuthenticationInterceptor;
import com.example.cafe.common.exception.ApplicationException;
import com.example.cafe.common.response.ApiResponse;
import com.example.cafe.common.response.PageResponse;
import com.example.cafe.menu.dto.AdminMenuResponse;
import com.example.cafe.menu.dto.MenuCreateRequest;
import com.example.cafe.menu.dto.MenuResponse;
import com.example.cafe.menu.dto.MenuStatusUpdateRequest;
import com.example.cafe.menu.entity.MenuStatus;
import com.example.cafe.menu.service.MenuService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/menus")
public class AdminMenuController {

	private final MenuService menuService;

	public AdminMenuController(MenuService menuService) {
		this.menuService = menuService;
	}

	@PostMapping
	public ResponseEntity<ApiResponse<MenuResponse>> createMenu(
		HttpServletRequest httpServletRequest,
		@RequestBody @Valid MenuCreateRequest request
	) {
		MenuResponse response = menuService.createMenuForAdmin(
			getAuthenticatedUserId(httpServletRequest),
			request
		);

		return ResponseEntity.created(URI.create("/api/v1/admin/menus/" + response.menuId()))
			.body(ApiResponse.success("SUCCESS", "메뉴 생성이 완료되었습니다.", response));
	}

	@PatchMapping("/{menuId}/status")
	public ApiResponse<MenuResponse> updateMenuStatus(
		HttpServletRequest httpServletRequest,
		@PathVariable long menuId,
		@RequestBody @Valid MenuStatusUpdateRequest request
	) {
		return ApiResponse.success(
			"SUCCESS",
			"메뉴 상태 변경이 완료되었습니다.",
			menuService.updateMenuStatusForAdmin(
				getAuthenticatedUserId(httpServletRequest),
				menuId,
				request
			)
		);
	}

	@GetMapping
	public ApiResponse<PageResponse<AdminMenuResponse>> getMenus(
		HttpServletRequest httpServletRequest,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "10") int size,
		@RequestParam(required = false) MenuStatus status
	) {
		validatePagination(page, size);

		return ApiResponse.success(
			"SUCCESS",
			"관리자 메뉴 목록 조회가 완료되었습니다.",
			menuService.getMenusForAdmin(getAuthenticatedUserId(httpServletRequest), page, size, status)
		);
	}

	private long getAuthenticatedUserId(HttpServletRequest request) {
		return (long) request.getAttribute(AuthenticationInterceptor.AUTHENTICATED_USER_ID);
	}

	private void validatePagination(int page, int size) {
		if (page < 0) {
			throw ApplicationException.invalidRequest("page는 0 이상이어야 합니다.");
		}

		if (size < 1 || size > 100) {
			throw ApplicationException.invalidRequest("size는 1 이상 100 이하여야 합니다.");
		}
	}
}
