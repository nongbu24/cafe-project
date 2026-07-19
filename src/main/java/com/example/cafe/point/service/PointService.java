package com.example.cafe.point.service;

import com.example.cafe.common.exception.ApplicationException;
import com.example.cafe.common.exception.ErrorCode;
import com.example.cafe.common.util.DateTimeUtils;
import com.example.cafe.point.dto.PointChargePaymentResponse;
import com.example.cafe.point.dto.PointChargeResponse;
import com.example.cafe.point.dto.PortOnePaymentWebhookRequest;
import com.example.cafe.point.dto.PortOnePaymentWebhookResponse;
import com.example.cafe.point.entity.PointChargePayment;
import com.example.cafe.point.repository.PointChargePaymentRepository;
import com.example.cafe.point.facade.PointFacade;
import com.example.cafe.user.entity.User;
import com.example.cafe.user.facade.UserFacade;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class PointService {

	private final UserFacade userFacade;
	private final PointFacade pointFacade;
	private final PointChargePaymentRepository pointChargePaymentRepository;
	private final PortOneProperties portOneProperties;
	private final PortOnePaymentClient portOnePaymentClient;

	public PointService(
		UserFacade userFacade,
		PointFacade pointFacade,
		PointChargePaymentRepository pointChargePaymentRepository,
		PortOneProperties portOneProperties,
		PortOnePaymentClient portOnePaymentClient
	) {
		this.userFacade = userFacade;
		this.pointFacade = pointFacade;
		this.pointChargePaymentRepository = pointChargePaymentRepository;
		this.portOneProperties = portOneProperties;
		this.portOnePaymentClient = portOnePaymentClient;
	}

	@Transactional
	public PointChargePaymentResponse prepareChargePayment(long userId, long amount) {
		validate(userId, amount);
		portOneProperties.validatePaymentRequestConfig();

		User user = userFacade.getActiveUser(userId);
		String paymentId = generatePaymentId();
		pointChargePaymentRepository.save(PointChargePayment.ready(user, paymentId, amount));

		return new PointChargePaymentResponse(
			paymentId,
			portOneProperties.getStoreId(),
			portOneProperties.getChannelKey(),
			"포인트 %d원 충전".formatted(amount),
			amount,
			"KRW"
		);
	}

	@Transactional
	public PortOnePaymentWebhookResponse handlePortOnePaymentWebhook(PortOnePaymentWebhookRequest request) {
		if (!"Transaction.Paid".equals(request.type())) {
			return new PortOnePaymentWebhookResponse(request.data().paymentId(), "IGNORED", false);
		}

		if (!StringUtils.hasText(request.data().paymentId())) {
			throw ApplicationException.invalidRequest("결제 식별값은 필수입니다.");
		}

		if (!StringUtils.hasText(request.data().storeId())) {
			throw ApplicationException.invalidRequest("상점 식별값은 필수입니다.");
		}

		if (!portOneProperties.getStoreId().equals(request.data().storeId())) {
			throw ApplicationException.invalidRequest("포트원 상점 식별값이 일치하지 않습니다.");
		}

		PointChargePayment chargePayment = pointChargePaymentRepository.findByPaymentId(request.data().paymentId())
			.orElse(null);

		if (chargePayment == null) {
			return new PortOnePaymentWebhookResponse(request.data().paymentId(), "NOT_FOUND", false);
		}

		if (chargePayment.isPaid()) {
			return new PortOnePaymentWebhookResponse(chargePayment.getPaymentId(), "PAID", false);
		}

		PortOnePayment portOnePayment = portOnePaymentClient.getPayment(chargePayment.getPaymentId());
		if (portOnePayment == null) {
			return new PortOnePaymentWebhookResponse(chargePayment.getPaymentId(), "NOT_FOUND", false);
		}

		validatePaidPayment(chargePayment, portOnePayment);

		User user = userFacade.getActiveUserForUpdate(chargePayment.getUser().getId());
		LocalDateTime chargedAt = DateTimeUtils.utcNow();
		user.charge(chargePayment.getAmount());
		pointFacade.saveChargeTransaction(
			user,
			chargePayment.getAmount(),
			user.getPointBalance(),
			chargedAt
		);
		chargePayment.complete(resolveTransactionId(request, portOnePayment), chargedAt);

		return new PortOnePaymentWebhookResponse(chargePayment.getPaymentId(), "PAID", true);
	}

	@Transactional
	public PointChargeResponse charge(long userId, long amount) {
		validate(userId, amount);

		User user = userFacade.getActiveUserForUpdate(userId);

		return chargeUser(user, amount);
	}

	private PointChargeResponse chargeUser(User user, long amount) {
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

	private void validatePaidPayment(PointChargePayment chargePayment, PortOnePayment portOnePayment) {
		if (!"PAID".equals(portOnePayment.status())) {
			throw ApplicationException.invalidRequest("포트원 결제 상태가 결제 완료가 아닙니다.");
		}

		if (!chargePayment.getPaymentId().equals(portOnePayment.paymentId())) {
			throw ApplicationException.invalidRequest("포트원 결제 식별값이 일치하지 않습니다.");
		}

		if (!portOneProperties.getStoreId().equals(portOnePayment.storeId())) {
			throw ApplicationException.invalidRequest("포트원 결제의 상점 식별값이 일치하지 않습니다.");
		}

		if (chargePayment.getAmount() != portOnePayment.totalAmount()) {
			throw ApplicationException.invalidRequest("포트원 결제 금액이 충전 요청 금액과 일치하지 않습니다.");
		}
	}

	private String resolveTransactionId(PortOnePaymentWebhookRequest request, PortOnePayment portOnePayment) {
		if (StringUtils.hasText(portOnePayment.transactionId())) {
			return portOnePayment.transactionId();
		}

		return request.data().transactionId();
	}

	private String generatePaymentId() {
		return "point-charge-" + UUID.randomUUID().toString().replace("-", "");
	}

	private void validate(long userId, long amount) {
		if (userId < 1) {
			throw new ApplicationException(ErrorCode.USER_NOT_FOUND);
		}

		if (amount < 1) {
			throw ApplicationException.invalidRequest("amount는 1 이상이어야 합니다.");
		}
	}
}
