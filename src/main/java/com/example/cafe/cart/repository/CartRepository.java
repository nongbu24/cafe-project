package com.example.cafe.cart.repository;

import com.example.cafe.cart.entity.Cart;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartRepository extends JpaRepository<Cart, Long> {

	Optional<Cart> findByUserId(long userId);
}
