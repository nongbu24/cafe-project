package com.example.cafe.point.service;

import com.example.cafe.common.exception.ApplicationException;
import com.example.cafe.common.exception.ErrorCode;
import com.example.cafe.point.dto.PointChargeResponse;
import com.example.cafe.point.facade.PointFacade;
import com.example.cafe.user.entity.User;
import com.example.cafe.user.facade.UserFacade;
import java.time.OffsetDateTime;
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

		User user = userFacade.getUserForUpdate(userId);

		user.charge(amount);
		OffsetDateTime chargedAt = OffsetDateTime.now();
		pointFacade.saveChargeTransaction(
			user,
			amount,
			user.getPointBalance(),
			chargedAt.toLocalDateTime()
		);

		return new PointChargeResponse(
			user.getId(),
			amount,
			user.getPointBalance(),
			chargedAt
		);
	}

	private void validate(long userId, long amount) {
		if (userId < 1) {
			throw new ApplicationException(ErrorCode.INVALID_REQUEST, "userId는 1 이상이어야 합니다.");
		}

		if (amount < 1) {
			throw new ApplicationException(ErrorCode.INVALID_REQUEST, "amount는 1 이상이어야 합니다.");
		}
	}
}
