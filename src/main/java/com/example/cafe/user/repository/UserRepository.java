package com.example.cafe.user.repository;

import com.example.cafe.user.entity.User;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT user FROM User user WHERE user.id = :userId")
	Optional<User> findByIdForUpdate(@Param("userId") long userId);
}
