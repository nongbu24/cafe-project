package com.example.cafe.user.service;

import com.example.cafe.common.exception.ApplicationException;
import com.example.cafe.common.exception.ErrorCode;
import com.example.cafe.common.response.PageResponse;
import com.example.cafe.user.dto.AdminUserDetailResponse;
import com.example.cafe.user.dto.AdminUserSummaryResponse;
import com.example.cafe.user.entity.User;
import com.example.cafe.user.entity.UserStatus;
import com.example.cafe.user.facade.UserFacade;
import com.example.cafe.user.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

	private final UserRepository userRepository;
	private final UserFacade userFacade;

	public UserService(UserRepository userRepository, UserFacade userFacade) {
		this.userRepository = userRepository;
		this.userFacade = userFacade;
	}

	@Transactional(readOnly = true)
	public PageResponse<AdminUserSummaryResponse> getUsersForAdmin(long adminUserId, int page, int size) {
		validateAdmin(adminUserId);

		return PageResponse.from(userRepository.findAll(
			PageRequest.of(page, size, Sort.by("id").ascending())
		).map(AdminUserSummaryResponse::from));
	}

	@Transactional(readOnly = true)
	public AdminUserDetailResponse getUserForAdmin(long adminUserId, long userId) {
		validateAdmin(adminUserId);

		return AdminUserDetailResponse.from(userFacade.getUser(userId));
	}

	private void validateAdmin(long userId) {
		User user = userFacade.getActiveUser(userId);
		if (user.getUserStatus() != UserStatus.ADMIN) {
			throw new ApplicationException(ErrorCode.FORBIDDEN);
		}
	}
}
