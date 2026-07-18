package com.example.cafe.user.repository;

import static com.example.cafe.user.entity.QUser.user;

import com.example.cafe.user.entity.User;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.LockModeType;
import java.util.Optional;

public class UserRepositoryImpl implements UserRepositoryCustom {

	private final JPAQueryFactory queryFactory;

	public UserRepositoryImpl(JPAQueryFactory queryFactory) {
		this.queryFactory = queryFactory;
	}

	@Override
	public Optional<User> findByIdForUpdate(long userId) {
		User foundUser = queryFactory
			.selectFrom(user)
			.where(user.id.eq(userId))
			.setLockMode(LockModeType.PESSIMISTIC_WRITE)
			.fetchOne();

		return Optional.ofNullable(foundUser);
	}
}
