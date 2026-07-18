package com.example.cafe.user.repository;

import com.example.cafe.user.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long>, UserRepositoryCustom {

	boolean existsByUsername(String username);

	Optional<User> findByUsername(String username);
}
