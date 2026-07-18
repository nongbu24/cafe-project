package com.example.cafe.order.service;

import com.example.cafe.order.dto.OrderPaidEventPayload;
import org.springframework.stereotype.Component;

@Component
public class MockOrderDataCollector {

	public void send(OrderPaidEventPayload payload) {
		// 실제 데이터 수집 플랫폼 대신 테스트에서 검증 가능한 Mock 전송 지점으로 둔다.
	}
}
