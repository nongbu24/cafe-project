package com.example.cafe.point.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class PointControllerTest {

	private static final long USER_ID = 1L;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void setUp() {
		jdbcTemplate.update("DELETE FROM order_event_outbox");
		jdbcTemplate.update("DELETE FROM point_transaction");
		jdbcTemplate.update("DELETE FROM orders");
		jdbcTemplate.update("DELETE FROM users");
		jdbcTemplate.update(
			"""
			INSERT INTO users (id, username, password, user_status, point_balance, is_deleted, created_at, updated_at)
			VALUES (?, 'point_test', 'encoded', 'USER', ?, FALSE, CURRENT_TIMESTAMP, NULL)
			""",
			USER_ID,
			3500
		);
	}

	@Test
	void 입력한_금액만큼_포인트를_충전한다() throws Exception {
		mockMvc.perform(post("/api/v1/users/{userId}/point-charges", USER_ID)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "amount": 10000
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("SUCCESS"))
			.andExpect(jsonPath("$.message").value("요청이 성공적으로 처리되었습니다."))
			.andExpect(jsonPath("$.data.userId").value(USER_ID))
			.andExpect(jsonPath("$.data.chargedAmount").value(10000))
			.andExpect(jsonPath("$.data.pointBalance").value(13500))
			.andExpect(jsonPath("$.data.chargedAt").isNotEmpty());

		assertThat(findPointBalance()).isEqualTo(13500);
		assertThat(countChargeTransactions()).isEqualTo(1);
		assertThat(findTransactionAmount()).isEqualTo(10000);
		assertThat(findTransactionBalanceAfter()).isEqualTo(13500);
	}

	@Test
	void 충전을_여러_번_요청하면_잔액이_누적된다() throws Exception {
		charge(2000);
		charge(3000);

		assertThat(findPointBalance()).isEqualTo(8500);
		assertThat(countChargeTransactions()).isEqualTo(2);
	}

	@Test
	void 충전금액이_0이면_400을_반환한다() throws Exception {
		mockMvc.perform(post("/api/v1/users/{userId}/point-charges", USER_ID)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "amount": 0
					}
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
			.andExpect(jsonPath("$.message").value("amount는 1 이상이어야 합니다."))
			.andExpect(jsonPath("$.data").doesNotExist());

		assertThat(findPointBalance()).isEqualTo(3500);
		assertThat(countChargeTransactions()).isZero();
	}

	@Test
	void 사용자_식별값이_0이면_400을_반환한다() throws Exception {
		mockMvc.perform(post("/api/v1/users/{userId}/point-charges", 0)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "amount": 1000
					}
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
			.andExpect(jsonPath("$.message").value("userId는 1 이상이어야 합니다."))
			.andExpect(jsonPath("$.data").doesNotExist());
	}

	@Test
	void 사용자가_존재하지_않으면_404를_반환한다() throws Exception {
		mockMvc.perform(post("/api/v1/users/{userId}/point-charges", 9999)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "amount": 1000
					}
					"""))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("USER_NOT_FOUND"))
			.andExpect(jsonPath("$.message").value("사용자를 찾을 수 없습니다."))
			.andExpect(jsonPath("$.data").doesNotExist());
	}

	@Test
	void 충전금액의_타입이_잘못되면_400을_반환한다() throws Exception {
		mockMvc.perform(post("/api/v1/users/{userId}/point-charges", USER_ID)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "amount": "잘못된 값"
					}
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
			.andExpect(jsonPath("$.message").value("요청 값이 올바르지 않습니다."))
			.andExpect(jsonPath("$.data").doesNotExist());
	}

	private void charge(long amount) throws Exception {
		mockMvc.perform(post("/api/v1/users/{userId}/point-charges", USER_ID)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "amount": %d
					}
					""".formatted(amount)))
			.andExpect(status().isOk());
	}

	private Long findPointBalance() {
		return jdbcTemplate.queryForObject(
			"SELECT point_balance FROM users WHERE id = ?",
			Long.class,
			USER_ID
		);
	}

	private Long countChargeTransactions() {
		return jdbcTemplate.queryForObject(
			"""
			SELECT COUNT(*)
			FROM point_transaction
			WHERE user_id = ? AND type = 'CHARGE'
			""",
			Long.class,
			USER_ID
		);
	}

	private Long findTransactionAmount() {
		return jdbcTemplate.queryForObject(
			"SELECT amount FROM point_transaction WHERE user_id = ?",
			Long.class,
			USER_ID
		);
	}

	private Long findTransactionBalanceAfter() {
		return jdbcTemplate.queryForObject(
			"SELECT balance_after FROM point_transaction WHERE user_id = ?",
			Long.class,
			USER_ID
		);
	}
}
