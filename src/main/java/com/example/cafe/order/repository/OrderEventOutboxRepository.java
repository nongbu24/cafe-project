package com.example.cafe.order.repository;

import com.example.cafe.order.entity.OrderEventOutbox;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderEventOutboxRepository extends JpaRepository<OrderEventOutbox, Long> {
}
