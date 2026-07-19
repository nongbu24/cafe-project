package com.example.cafe.point.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.cafe.auth.service.JwtTokenProvider;
import com.example.cafe.auth.store.TokenBlacklistStore;
import com.example.cafe.point.service.PortOnePayment;
import com.example.cafe.point.service.PortOnePaymentClient;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
	"app.portone.store-id=store-test",
	"app.portone.channel-key=channel-test",
	"app.portone.api-secret=secret-test"
})
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

	@MockitoBean
	private PortOnePaymentClient portOnePaymentClient;

	@BeforeEach
	void setUp() {
		reset(portOnePaymentClient);
		jdbcTemplate.update("DELETE FROM order_event_outbox");
		jdbcTemplate.update("DELETE FROM point_charge_payment");
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
	void 입력한_금액으로_포인트_충전_결제를_준비한다() throws Exception {
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
			.andExpect(jsonPath("$.data.paymentId").isNotEmpty())
			.andExpect(jsonPath("$.data.storeId").value("store-test"))
			.andExpect(jsonPath("$.data.channelKey").value("channel-test"))
			.andExpect(jsonPath("$.data.orderName").value("포인트 10000원 충전"))
			.andExpect(jsonPath("$.data.amount").value(10000))
			.andExpect(jsonPath("$.data.currency").value("KRW"));

		assertThat(findPointBalance()).isEqualTo(3500);
		assertThat(countChargePayments()).isEqualTo(1);
		assertThat(countChargeTransactions()).isZero();
		verifyNoInteractions(portOnePaymentClient);
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
		assertThat(countChargePayments()).isZero();
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
		assertThat(countChargePayments()).isZero();
		assertThat(countChargeTransactions()).isZero();
	}

	@Test
	void 요청에_다른_userId가_있어도_로그인한_회원의_충전_결제를_준비한다() throws Exception {
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
			.andExpect(jsonPath("$.data.amount").value(1000));

		assertThat(findPointBalance()).isEqualTo(3500);
		assertThat(findPointBalance(OTHER_USER_ID)).isEqualTo(9000);
		assertThat(countChargePayments()).isEqualTo(1);
		assertThat(countChargePaymentUsers()).containsExactly(USER_ID);
		assertThat(countChargeTransactions()).isZero();
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
		assertThat(countChargePayments()).isZero();
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
		assertThat(countChargePayments()).isZero();
		assertThat(countChargeTransactions()).isZero();
	}

	@Test
	void 포트원_결제완료_웹훅으로_포인트를_충전한다() throws Exception {
		String paymentId = "point-charge-test";
		insertReadyChargePayment(paymentId, 10000);
		when(portOnePaymentClient.getPayment(paymentId))
			.thenReturn(new PortOnePayment(paymentId, "store-test", "PAID", 10000, "tx-1"));

		mockMvc.perform(post("/api/v1/portone/webhooks/payments")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "type": "Transaction.Paid",
					  "timestamp": "2026-07-19T10:00:00.000Z",
					  "data": {
					    "paymentId": "%s",
					    "storeId": "store-test",
					    "transactionId": "tx-1"
					  }
					}
					""".formatted(paymentId)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.paymentId").value(paymentId))
			.andExpect(jsonPath("$.data.status").value("PAID"))
			.andExpect(jsonPath("$.data.charged").value(true));

		assertThat(findPointBalance()).isEqualTo(13500);
		assertThat(findChargePaymentStatus(paymentId)).isEqualTo("PAID");
		assertThat(countChargeTransactions()).isEqualTo(1);
		assertThat(findTransactionAmount()).isEqualTo(10000);
		assertThat(findTransactionBalanceAfter()).isEqualTo(13500);
	}

	@Test
	void 이미_처리된_웹훅은_중복_충전하지_않는다() throws Exception {
		String paymentId = "point-charge-paid";
		insertPaidChargePayment(paymentId, 10000);

		mockMvc.perform(post("/api/v1/portone/webhooks/payments")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "type": "Transaction.Paid",
					  "timestamp": "2026-07-19T10:00:00.000Z",
					  "data": {
					    "paymentId": "%s",
					    "storeId": "store-test",
					    "transactionId": "tx-1"
					  }
					}
					""".formatted(paymentId)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.paymentId").value(paymentId))
			.andExpect(jsonPath("$.data.status").value("PAID"))
			.andExpect(jsonPath("$.data.charged").value(false));

		assertThat(findPointBalance()).isEqualTo(3500);
		assertThat(countChargeTransactions()).isZero();
		verifyNoInteractions(portOnePaymentClient);
	}

	@Test
	void 포트원_결제금액이_충전요청과_다르면_충전하지_않는다() throws Exception {
		String paymentId = "point-charge-mismatch";
		insertReadyChargePayment(paymentId, 10000);
		when(portOnePaymentClient.getPayment(paymentId))
			.thenReturn(new PortOnePayment(paymentId, "store-test", "PAID", 9000, "tx-1"));

		mockMvc.perform(post("/api/v1/portone/webhooks/payments")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "type": "Transaction.Paid",
					  "timestamp": "2026-07-19T10:00:00.000Z",
					  "data": {
					    "paymentId": "%s",
					    "storeId": "store-test",
					    "transactionId": "tx-1"
					  }
					}
					""".formatted(paymentId)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

		assertThat(findPointBalance()).isEqualTo(3500);
		assertThat(findChargePaymentStatus(paymentId)).isEqualTo("READY");
		assertThat(countChargeTransactions()).isZero();
	}

	@Test
	void 결제완료가_아닌_웹훅은_무시한다() throws Exception {
		mockMvc.perform(post("/api/v1/portone/webhooks/payments")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "type": "Transaction.Ready",
					  "timestamp": "2026-07-19T10:00:00.000Z",
					  "data": {
					    "paymentId": "point-charge-ready",
					    "storeId": "store-test",
					    "transactionId": "tx-1"
					  }
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.status").value("IGNORED"))
			.andExpect(jsonPath("$.data.charged").value(false));

		assertThat(findPointBalance()).isEqualTo(3500);
		assertThat(countChargeTransactions()).isZero();
		verifyNoInteractions(portOnePaymentClient);
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

	private Long countChargePayments() {
		return jdbcTemplate.queryForObject(
			"""
			SELECT COUNT(*)
			FROM point_charge_payment
			WHERE user_id = ?
			""",
			Long.class,
			USER_ID
		);
	}

	private java.util.List<Long> countChargePaymentUsers() {
		return jdbcTemplate.queryForList(
			"SELECT user_id FROM point_charge_payment ORDER BY id",
			Long.class
		);
	}

	private String findChargePaymentStatus(String paymentId) {
		return jdbcTemplate.queryForObject(
			"SELECT status FROM point_charge_payment WHERE payment_id = ?",
			String.class,
			paymentId
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

	private void insertReadyChargePayment(String paymentId, long amount) {
		jdbcTemplate.update(
			"""
			INSERT INTO point_charge_payment (
			    user_id, payment_id, amount, status, portone_transaction_id, paid_at, created_at, updated_at
			)
			VALUES (?, ?, ?, 'READY', NULL, NULL, CURRENT_TIMESTAMP, NULL)
			""",
			USER_ID,
			paymentId,
			amount
		);
	}

	private void insertPaidChargePayment(String paymentId, long amount) {
		jdbcTemplate.update(
			"""
			INSERT INTO point_charge_payment (
			    user_id, payment_id, amount, status, portone_transaction_id, paid_at, created_at, updated_at
			)
			VALUES (?, ?, ?, 'PAID', 'tx-1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL)
			""",
			USER_ID,
			paymentId,
			amount
		);
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
