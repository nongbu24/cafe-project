package com.example.cafe.point.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.cafe.auth.service.JwtTokenProvider;
import com.example.cafe.auth.store.TokenBlacklistStore;
import com.example.cafe.user.entity.User;
import com.example.cafe.user.repository.UserRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(PointControllerTest.TokenBlacklistStoreTestConfig.class)
class PointControllerTest {

	private static final long USER_ID = 1L;
	private static final long OTHER_USER_ID = 2L;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private JwtTokenProvider jwtTokenProvider;

	@Autowired
	private UserRepository userRepository;

	@BeforeEach
	void setUp() {
		jdbcTemplate.update("DELETE FROM order_event_outbox");
		jdbcTemplate.update("DELETE FROM point_transaction");
		jdbcTemplate.update("DELETE FROM order_items");
		jdbcTemplate.update("DELETE FROM orders");
		jdbcTemplate.update("DELETE FROM cart_items");
		jdbcTemplate.update("DELETE FROM carts");
		jdbcTemplate.update("DELETE FROM users");
		jdbcTemplate.update(
			"""
			INSERT INTO users (id, username, password, user_status, point_balance, is_deleted, created_at, updated_at)
			VALUES (?, 'point_test', 'encoded', 'USER', ?, FALSE, CURRENT_TIMESTAMP, NULL)
			""",
			USER_ID,
			3500
		);
		jdbcTemplate.update(
			"""
			INSERT INTO users (id, username, password, user_status, point_balance, is_deleted, created_at, updated_at)
			VALUES (?, 'other_point_test', 'encoded', 'USER', ?, FALSE, CURRENT_TIMESTAMP, NULL)
			""",
			OTHER_USER_ID,
			9000
		);
	}

	@Test
	void 입력한_금액만큼_포인트를_충전한다() throws Exception {
		mockMvc.perform(post("/api/v1/users/me/point-charges")
				.header("Authorization", bearerToken())
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
	void 충전금액이_0이면_400을_반환한다() throws Exception {
		mockMvc.perform(post("/api/v1/users/me/point-charges")
				.header("Authorization", bearerToken())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "amount": 0
					}
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
			.andExpect(jsonPath("$.message").value("충전할 포인트는 1 이상이어야 합니다."))
			.andExpect(jsonPath("$.data").doesNotExist());

		assertThat(findPointBalance()).isEqualTo(3500);
		assertThat(countChargeTransactions()).isZero();
	}

	@Test
	void 충전금액이_null이면_400을_반환한다() throws Exception {
		mockMvc.perform(post("/api/v1/users/me/point-charges")
				.header("Authorization", bearerToken())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "amount": null
					}
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
			.andExpect(jsonPath("$.message").value("충전할 포인트는 필수로 입력해야 합니다."))
			.andExpect(jsonPath("$.data").doesNotExist());

		assertThat(findPointBalance()).isEqualTo(3500);
		assertThat(countChargeTransactions()).isZero();
	}

	@Test
	void 요청에_다른_userId가_있어도_로그인한_회원에게_충전한다() throws Exception {
		mockMvc.perform(post("/api/v1/users/me/point-charges")
				.header("Authorization", bearerToken())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "userId": %d,
					  "amount": 1000
					}
					""".formatted(OTHER_USER_ID)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.userId").value(USER_ID))
			.andExpect(jsonPath("$.data.pointBalance").value(4500));

		assertThat(findPointBalance()).isEqualTo(4500);
		assertThat(findPointBalance(OTHER_USER_ID)).isEqualTo(9000);
		assertThat(countChargeTransactions()).isEqualTo(1);
	}

	@Test
	void 인증하지_않으면_충전할_수_없다() throws Exception {
		mockMvc.perform(post("/api/v1/users/me/point-charges")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "amount": 1000
					}
					"""))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"))
			.andExpect(jsonPath("$.data").doesNotExist());

		assertThat(findPointBalance()).isEqualTo(3500);
		assertThat(countChargeTransactions()).isZero();
	}

	@Test
	void 탈퇴한_사용자의_토큰으로는_충전할_수_없다() throws Exception {
		String token = bearerToken();
		jdbcTemplate.update("UPDATE users SET is_deleted = TRUE WHERE id = ?", USER_ID);

		mockMvc.perform(post("/api/v1/users/me/point-charges")
				.header("Authorization", token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "amount": 1000
					}
					"""))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("INVALID_TOKEN"))
			.andExpect(jsonPath("$.data").doesNotExist());

		assertThat(findPointBalance()).isEqualTo(3500);
		assertThat(countChargeTransactions()).isZero();
	}

	private Long findPointBalance() {
		return findPointBalance(USER_ID);
	}

	private Long findPointBalance(long userId) {
		return jdbcTemplate.queryForObject(
			"SELECT point_balance FROM users WHERE id = ?",
			Long.class,
			userId
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

	private String bearerToken() {
		User user = userRepository.findById(USER_ID).orElseThrow();
		return "Bearer " + jwtTokenProvider.issue(user).value();
	}

	@TestConfiguration(proxyBeanMethods = false)
	static class TokenBlacklistStoreTestConfig {

		@Bean
		@Primary
		TokenBlacklistStore tokenBlacklistStore() {
			return new TokenBlacklistStore() {

				@Override
				public void add(String tokenId, LocalDateTime expiresAt) {
				}

				@Override
				public boolean contains(String tokenId) {
					return false;
				}
			};
		}
	}
}
