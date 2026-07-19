package com.example.cafe.order.repository;

import com.example.cafe.order.entity.OrderEventOutbox;
import com.example.cafe.order.entity.OrderEventStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderEventOutboxRepository extends JpaRepository<OrderEventOutbox, Long> {

	List<OrderEventOutbox> findTop20ByStatusAndNextRetryAtLessThanEqualOrderByNextRetryAtAscIdAsc(
		OrderEventStatus status,
		LocalDateTime now
	);
}
