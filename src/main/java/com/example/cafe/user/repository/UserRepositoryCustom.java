package com.example.cafe.user.repository;

import com.example.cafe.user.entity.User;
import java.util.Optional;

public interface UserRepositoryCustom {

	Optional<User> findByIdForUpdate(long userId);
}
