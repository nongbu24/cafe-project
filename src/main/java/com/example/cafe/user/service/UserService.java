package com.example.cafe.user.service;

import com.example.cafe.common.exception.ApplicationException;
import com.example.cafe.common.exception.ErrorCode;
import com.example.cafe.common.response.PageResponse;
import com.example.cafe.user.dto.AdminUserDetailResponse;
import com.example.cafe.user.dto.AdminUserSummaryResponse;
import com.example.cafe.user.dto.MyUserResponse;
import com.example.cafe.user.dto.PasswordChangeRequest;
import com.example.cafe.user.entity.User;
import com.example.cafe.user.entity.UserStatus;
import com.example.cafe.user.facade.UserFacade;
import com.example.cafe.user.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

	private final UserRepository userRepository;
	private final UserFacade userFacade;
	private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

	public UserService(UserRepository userRepository, UserFacade userFacade) {
		this.userRepository = userRepository;
		this.userFacade = userFacade;
	}

	@Transactional(readOnly = true)
	public MyUserResponse getMe(long userId) {
		return MyUserResponse.from(userFacade.getActiveUser(userId));
	}

	@Transactional
	public void changePassword(long userId, PasswordChangeRequest request) {
		if (request == null) {
			throw ApplicationException.invalidRequest();
		}

		validatePassword(request.currentPassword(), "currentPassword");
		validatePassword(request.newPassword(), "newPassword");

		User user = userFacade.getActiveUserForUpdate(userId);
		if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
			throw new ApplicationException(ErrorCode.INVALID_PASSWORD);
		}

		user.changePassword(passwordEncoder.encode(request.newPassword()));
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

	private void validatePassword(String password, String fieldName) {
		if (password == null || !password.matches("[!-~]{8,64}")) {
			throw ApplicationException.invalidRequest(
				fieldName + "는 공백 없이 영문, 숫자, 일반 특수문자로 8자 이상 64자 이하로 입력해야 합니다."
			);
		}
	}
}
