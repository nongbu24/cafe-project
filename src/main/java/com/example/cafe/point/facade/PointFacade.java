package com.example.cafe.point.facade;

import com.example.cafe.point.entity.PointTransaction;
import com.example.cafe.point.repository.PointTransactionRepository;
import com.example.cafe.user.entity.User;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

@Service
public class PointFacade {

	private final PointTransactionRepository pointTransactionRepository;

	public PointFacade(PointTransactionRepository pointTransactionRepository) {
		this.pointTransactionRepository = pointTransactionRepository;
	}

	public void saveChargeTransaction(
		User user,
		long amount,
		long balanceAfterTransaction,
		LocalDateTime transactedAt
	) {
		pointTransactionRepository.save(PointTransaction.charge(
			user,
			amount,
			balanceAfterTransaction,
			transactedAt
		));
	}

	public void savePaymentTransaction(
		User user,
		long orderId,
		long amount,
		long balanceAfterTransaction,
		LocalDateTime transactedAt
	) {
		pointTransactionRepository.save(PointTransaction.payment(
			user,
			orderId,
			amount,
			balanceAfterTransaction,
			transactedAt
		));
	}
}
