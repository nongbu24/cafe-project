package com.example.cafe.user.facade;

import com.example.cafe.common.exception.ApplicationException;
import com.example.cafe.common.exception.ErrorCode;
import com.example.cafe.user.entity.User;
import com.example.cafe.user.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class UserFacade {

	private final UserRepository userRepository;

	public UserFacade(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	public boolean existsByUsername(String username) {
		return userRepository.existsByUsername(username);
	}

	public User save(User user) {
		return userRepository.save(user);
	}

	public User getActiveUserByUsername(String username) {
		return userRepository.findByUsername(username)
			.filter(found -> !found.isWithdrawn())
			.orElseThrow(() -> new ApplicationException(ErrorCode.INVALID_CREDENTIALS));
	}

	public User getUser(long userId) {
		return userRepository.findById(userId)
			.orElseThrow(() -> new ApplicationException(ErrorCode.USER_NOT_FOUND));
	}

	public User getActiveUser(long userId) {
		return userRepository.findById(userId)
			.filter(found -> !found.isWithdrawn())
			.orElseThrow(() -> new ApplicationException(ErrorCode.INVALID_TOKEN));
	}

	public User getUserForUpdate(long userId) {
		return userRepository.findByIdForUpdate(userId)
			.orElseThrow(() -> new ApplicationException(ErrorCode.USER_NOT_FOUND));
	}
}
