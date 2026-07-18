package com.example.cafe.order.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.cafe.order.service.MockOrderDataCollector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class OrderControllerTest {

	private static final long USER_ID = 1L;
	private static final long MENU_ID = 10L;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@MockitoBean
	private MockOrderDataCollector dataCollector;

	@BeforeEach
	void setUp() {
		jdbcTemplate.update("DELETE FROM order_event_outbox");
		jdbcTemplate.update("DELETE FROM point_transaction");
		jdbcTemplate.update("DELETE FROM orders");
		jdbcTemplate.update("DELETE FROM menus");
		jdbcTemplate.update("DELETE FROM users");
		jdbcTemplate.update(
			"""
			INSERT INTO users (id, username, password, user_status, point_balance, is_deleted, created_at, updated_at)
			VALUES (?, 'order_test', 'encoded', 'USER', ?, FALSE, CURRENT_TIMESTAMP, NULL)
			""",
			USER_ID,
			12000
		);
		jdbcTemplate.update(
			"""
			INSERT INTO menus (id, name, price, status, created_at, updated_at)
			VALUES (?, '카페라테', ?, 'AVAILABLE', CURRENT_TIMESTAMP, NULL)
			""",
			MENU_ID,
			5000
		);
	}

	@Test
	void 사용자와_메뉴로_주문하고_포인트로_결제한다() throws Exception {
		mockMvc.perform(post("/api/v1/orders")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "userId": %d,
					  "menuId": %d
					}
					""".formatted(USER_ID, MENU_ID)))
			.andExpect(status().isCreated())
			.andExpect(header().string("Location", "/api/v1/orders/1"))
			.andExpect(jsonPath("$.code").value("SUCCESS"))
			.andExpect(jsonPath("$.data.orderId").value(1))
			.andExpect(jsonPath("$.data.userId").value(USER_ID))
			.andExpect(jsonPath("$.data.menu.menuId").value(MENU_ID))
			.andExpect(jsonPath("$.data.menu.name").value("카페라테"))
			.andExpect(jsonPath("$.data.paymentAmount").value(5000))
			.andExpect(jsonPath("$.data.pointBalance").value(7000))
			.andExpect(jsonPath("$.data.status").value("PAID"))
			.andExpect(jsonPath("$.data.paidAt").isNotEmpty());

		assertThat(findPointBalance()).isEqualTo(7000);
		assertThat(countOrders()).isEqualTo(1);
		assertThat(countPaymentTransactions()).isEqualTo(1);
		assertThat(findPaymentTransactionAmount()).isEqualTo(5000);
		assertThat(findOutboxStatus()).isEqualTo("SENT");
		assertThat(findOutboxPayload())
			.contains("\"eventId\":1")
			.contains("\"eventType\":\"ORDER_PAID\"")
			.contains("\"occurredAt\"")
			.contains("\"userId\":1")
			.contains("\"menuId\":10")
			.contains("\"paymentAmount\":5000");

		verify(dataCollector, timeout(1000)).send(argThat(payload ->
			payload.eventId() == 1
				&& payload.occurredAt() != null
				&& payload.userId() == USER_ID
				&& payload.menuId() == MENU_ID
				&& payload.paymentAmount() == 5000
				&& "ORDER_PAID".equals(payload.eventType())
		));
	}

	@Test
	void 포인트가_부족하면_409를_반환하고_주문하지_않는다() throws Exception {
		jdbcTemplate.update("UPDATE users SET point_balance = 1000 WHERE id = ?", USER_ID);

		mockMvc.perform(post("/api/v1/orders")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "userId": %d,
					  "menuId": %d
					}
					""".formatted(USER_ID, MENU_ID)))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("INSUFFICIENT_POINTS"))
			.andExpect(jsonPath("$.message").value("포인트 잔액이 부족합니다."))
			.andExpect(jsonPath("$.data").doesNotExist());

		assertThat(findPointBalance()).isEqualTo(1000);
		assertThat(countOrders()).isZero();
		assertThat(countPaymentTransactions()).isZero();
		verifyNoInteractions(dataCollector);
	}

	@Test
	void 사용자가_존재하지_않으면_404를_반환한다() throws Exception {
		mockMvc.perform(post("/api/v1/orders")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "userId": 9999,
					  "menuId": %d
					}
					""".formatted(MENU_ID)))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("USER_NOT_FOUND"))
			.andExpect(jsonPath("$.data").doesNotExist());

		assertThat(countOrders()).isZero();
		verifyNoInteractions(dataCollector);
	}

	@Test
	void 메뉴가_존재하지_않으면_404를_반환한다() throws Exception {
		mockMvc.perform(post("/api/v1/orders")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "userId": %d,
					  "menuId": 9999
					}
					""".formatted(USER_ID)))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("MENU_NOT_FOUND"))
			.andExpect(jsonPath("$.data").doesNotExist());

		assertThat(countOrders()).isZero();
		verifyNoInteractions(dataCollector);
	}

	@Test
	void 사용자_식별값이_0이면_400을_반환한다() throws Exception {
		mockMvc.perform(post("/api/v1/orders")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "userId": 0,
					  "menuId": %d
					}
					""".formatted(MENU_ID)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
			.andExpect(jsonPath("$.message").value("userId는 1 이상이어야 합니다."))
			.andExpect(jsonPath("$.data").doesNotExist());
	}

	@Test
	void 메뉴_식별값이_0이면_400을_반환한다() throws Exception {
		mockMvc.perform(post("/api/v1/orders")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "userId": %d,
					  "menuId": 0
					}
					""".formatted(USER_ID)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
			.andExpect(jsonPath("$.message").value("menuId는 1 이상이어야 합니다."))
			.andExpect(jsonPath("$.data").doesNotExist());
	}

	private Long findPointBalance() {
		return jdbcTemplate.queryForObject(
			"SELECT point_balance FROM users WHERE id = ?",
			Long.class,
			USER_ID
		);
	}

	private Long countOrders() {
		return jdbcTemplate.queryForObject(
			"SELECT COUNT(*) FROM orders",
			Long.class
		);
	}

	private Long countPaymentTransactions() {
		return jdbcTemplate.queryForObject(
			"SELECT COUNT(*) FROM point_transaction WHERE type = 'PAYMENT'",
			Long.class
		);
	}

	private Long findPaymentTransactionAmount() {
		return jdbcTemplate.queryForObject(
			"SELECT amount FROM point_transaction WHERE type = 'PAYMENT'",
			Long.class
		);
	}

	private String findOutboxStatus() {
		return jdbcTemplate.queryForObject(
			"SELECT status FROM order_event_outbox",
			String.class
		);
	}

	private String findOutboxPayload() {
		return jdbcTemplate.queryForObject(
			"SELECT payload FROM order_event_outbox",
			String.class
		);
	}
}
