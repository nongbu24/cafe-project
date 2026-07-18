package com.example.cafe.order.service;

import com.example.cafe.order.entity.OrderEventOutbox;

public interface OrderEventPublisher {

	void publish(OrderEventOutbox event);
}
