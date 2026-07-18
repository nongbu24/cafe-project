package com.example.cafe.point.service;

import com.example.cafe.common.exception.ApplicationException;
import com.example.cafe.common.util.DateTimeUtils;
import com.example.cafe.point.dto.PointChargeResponse;
import com.example.cafe.point.facade.PointFacade;
import com.example.cafe.user.entity.User;
import com.example.cafe.user.facade.UserFacade;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PointService {

	private final UserFacade userFacade;
	private final PointFacade pointFacade;

	public PointService(
		UserFacade userFacade,
		PointFacade pointFacade
	) {
		this.userFacade = userFacade;
		this.pointFacade = pointFacade;
	}

	@Transactional
	public PointChargeResponse charge(long userId, long amount) {
		validate(userId, amount);

		User user = userFacade.getActiveUserForUpdate(userId);

		user.charge(amount);
		LocalDateTime chargedAt = DateTimeUtils.utcNow();
		pointFacade.saveChargeTransaction(
			user,
			amount,
			user.getPointBalance(),
			chargedAt
		);

		return new PointChargeResponse(
			user.getId(),
			amount,
			user.getPointBalance(),
			DateTimeUtils.toKoreaOffsetDateTime(chargedAt)
		);
	}

	private void validate(long userId, long amount) {
		if (userId < 1) {
			throw ApplicationException.invalidRequest("userId는 1 이상이어야 합니다.");
		}

		if (amount < 1) {
			throw ApplicationException.invalidRequest("amount는 1 이상이어야 합니다.");
		}
	}
}
