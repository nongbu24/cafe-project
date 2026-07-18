package com.example.cafe.order.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.cafe.auth.service.JwtTokenProvider;
import com.example.cafe.auth.store.TokenBlacklistStore;
import com.example.cafe.order.dto.OrderPaidEventPayload;
import com.example.cafe.order.service.MockOrderDataCollector;
import com.example.cafe.user.entity.User;
import com.example.cafe.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
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
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@Import(OrderControllerTest.TokenBlacklistStoreTestConfig.class)
class OrderControllerTest {

	private static final long USER_ID = 1L;
	private static final long OTHER_USER_ID = 2L;
	private static final long MENU_ID = 10L;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private JwtTokenProvider jwtTokenProvider;

	@Autowired
	private UserRepository userRepository;

	@MockitoBean
	private MockOrderDataCollector dataCollector;

	@BeforeEach
	void setUp() {
		reset(dataCollector);
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
			INSERT INTO users (id, username, password, user_status, point_balance, is_deleted, created_at, updated_at)
			VALUES (?, 'other_order_test', 'encoded', 'USER', ?, FALSE, CURRENT_TIMESTAMP, NULL)
			""",
			OTHER_USER_ID,
			9000
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
		MvcResult result = mockMvc.perform(post("/api/v1/orders")
				.header("Authorization", bearerToken())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "menuId": %d
					}
					""".formatted(MENU_ID)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.code").value("SUCCESS"))
			.andExpect(jsonPath("$.message").value("주문이 완료되었습니다."))
			.andExpect(jsonPath("$.data.userId").value(USER_ID))
			.andExpect(jsonPath("$.data.menu.menuId").value(MENU_ID))
			.andExpect(jsonPath("$.data.menu.name").value("카페라테"))
			.andExpect(jsonPath("$.data.paymentAmount").value(5000))
			.andExpect(jsonPath("$.data.pointBalance").value(7000))
			.andExpect(jsonPath("$.data.status").value("PAID"))
			.andExpect(jsonPath("$.data.paidAt").isNotEmpty())
			.andReturn();

		Long orderId = findOnlyOrderId();
		Long eventId = findOnlyOutboxId();
		assertThat(result.getResponse().getHeader("Location")).isEqualTo("/api/v1/orders/" + orderId);
		assertThat(result.getResponse().getContentAsString()).contains("\"orderId\":" + orderId);
		assertThat(findPointBalance()).isEqualTo(7000);
		assertThat(countOrders()).isEqualTo(1);
		assertThat(countPaymentTransactions()).isEqualTo(1);
		assertThat(findPaymentTransactionAmount()).isEqualTo(5000);
		assertThat(findOutboxPayload())
			.contains("\"eventId\":" + eventId)
			.contains("\"eventType\":\"ORDER_PAID\"")
			.contains("\"occurredAt\"")
			.contains("\"userId\":1")
			.contains("\"menuId\":10")
			.contains("\"paymentAmount\":5000");

		verify(dataCollector, timeout(1000)).send(argThat(payload ->
			payload.eventId().equals(eventId)
				&& payload.occurredAt() != null
				&& payload.userId() == USER_ID
				&& payload.menuId() == MENU_ID
				&& payload.paymentAmount() == 5000
				&& "ORDER_PAID".equals(payload.eventType())
		));
		waitForOutboxStatus("SENT");
	}

	@Test
	void Outbox_전송에_실패해도_주문_응답과_결제는_성공하고_전송_대기로_남긴다() throws Exception {
		doThrow(new RuntimeException("collector down")).when(dataCollector).send(any(OrderPaidEventPayload.class));

		mockMvc.perform(post("/api/v1/orders")
				.header("Authorization", bearerToken())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "menuId": %d
					}
					""".formatted(MENU_ID)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.data.userId").value(USER_ID))
			.andExpect(jsonPath("$.data.pointBalance").value(7000));

		Long eventId = findOnlyOutboxId();
		verify(dataCollector, timeout(1000)).send(argThat(payload ->
			payload.eventId().equals(eventId)
				&& payload.userId() == USER_ID
				&& payload.menuId() == MENU_ID
				&& payload.paymentAmount() == 5000
		));
		assertThat(findPointBalance()).isEqualTo(7000);
		assertThat(countOrders()).isEqualTo(1);
		assertThat(countPaymentTransactions()).isEqualTo(1);
		assertThat(findOutboxStatus()).isEqualTo("PENDING");
	}

	@Test
	void 같은_회원의_동시_결제는_잔액보다_많이_사용할_수_없다() throws Exception {
		jdbcTemplate.update("UPDATE users SET point_balance = 5000 WHERE id = ?", USER_ID);
		String token = bearerToken();

		ExecutorService executorService = Executors.newFixedThreadPool(2);
		CountDownLatch ready = new CountDownLatch(2);
		CountDownLatch start = new CountDownLatch(1);

		try {
			Future<Integer> first = executorService.submit(() -> orderStatusAfterWaiting(token, ready, start));
			Future<Integer> second = executorService.submit(() -> orderStatusAfterWaiting(token, ready, start));

			assertThat(ready.await(1, TimeUnit.SECONDS)).isTrue();
			start.countDown();

			List<Integer> statuses = List.of(first.get(3, TimeUnit.SECONDS), second.get(3, TimeUnit.SECONDS));

			assertThat(statuses).containsExactlyInAnyOrder(201, 409);
			assertThat(findPointBalance()).isZero();
			assertThat(countOrders()).isEqualTo(1);
			assertThat(countPaymentTransactions()).isEqualTo(1);
			waitForSentOutboxCount(1);
		} finally {
			executorService.shutdownNow();
		}
	}

	@Test
	void 요청에_다른_userId가_있어도_로그인한_회원으로_주문한다() throws Exception {
		mockMvc.perform(post("/api/v1/orders")
				.header("Authorization", bearerToken())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "userId": %d,
					  "menuId": %d
					}
					""".formatted(OTHER_USER_ID, MENU_ID)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.data.userId").value(USER_ID))
			.andExpect(jsonPath("$.data.pointBalance").value(7000));

		assertThat(findPointBalance()).isEqualTo(7000);
		assertThat(findPointBalance(OTHER_USER_ID)).isEqualTo(9000);
		assertThat(countOrders()).isEqualTo(1);
		waitForOutboxStatus("SENT");
	}

	@Test
	void 탈퇴한_사용자의_토큰으로는_주문할_수_없다() throws Exception {
		String token = bearerToken();
		jdbcTemplate.update("UPDATE users SET is_deleted = TRUE WHERE id = ?", USER_ID);

		mockMvc.perform(post("/api/v1/orders")
				.header("Authorization", token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "menuId": %d
					}
					""".formatted(MENU_ID)))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("INVALID_TOKEN"))
			.andExpect(jsonPath("$.data").doesNotExist());

		assertThat(findPointBalance()).isEqualTo(12000);
		assertThat(countOrders()).isZero();
		assertThat(countPaymentTransactions()).isZero();
		verifyNoInteractions(dataCollector);
	}

	@Test
	void 포인트가_부족하면_409를_반환하고_주문하지_않는다() throws Exception {
		jdbcTemplate.update("UPDATE users SET point_balance = 1000 WHERE id = ?", USER_ID);

		mockMvc.perform(post("/api/v1/orders")
				.header("Authorization", bearerToken())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "menuId": %d
					}
					""".formatted(MENU_ID)))
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
	void 품절_메뉴는_주문할_수_없다() throws Exception {
		jdbcTemplate.update("UPDATE menus SET status = 'SOLD_OUT' WHERE id = ?", MENU_ID);

		mockMvc.perform(post("/api/v1/orders")
				.header("Authorization", bearerToken())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "menuId": %d
					}
					""".formatted(MENU_ID)))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("MENU_NOT_AVAILABLE"))
			.andExpect(jsonPath("$.message").value("주문할 수 없는 메뉴입니다."))
			.andExpect(jsonPath("$.data").doesNotExist());

		assertThat(findPointBalance()).isEqualTo(12000);
		assertThat(countOrders()).isZero();
		assertThat(countPaymentTransactions()).isZero();
		verifyNoInteractions(dataCollector);
	}

	@Test
	void 단종_메뉴는_주문할_수_없다() throws Exception {
		jdbcTemplate.update("UPDATE menus SET status = 'DISCONTINUED' WHERE id = ?", MENU_ID);

		mockMvc.perform(post("/api/v1/orders")
				.header("Authorization", bearerToken())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "menuId": %d
					}
					""".formatted(MENU_ID)))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("MENU_NOT_AVAILABLE"))
			.andExpect(jsonPath("$.data").doesNotExist());

		assertThat(findPointBalance()).isEqualTo(12000);
		assertThat(countOrders()).isZero();
		assertThat(countPaymentTransactions()).isZero();
		verifyNoInteractions(dataCollector);
	}

	@Test
	void 인증하지_않으면_주문할_수_없다() throws Exception {
		mockMvc.perform(post("/api/v1/orders")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "menuId": %d
					}
					""".formatted(MENU_ID)))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"))
			.andExpect(jsonPath("$.data").doesNotExist());

		assertThat(countOrders()).isZero();
		verifyNoInteractions(dataCollector);
	}

	@Test
	void 메뉴가_존재하지_않으면_404를_반환한다() throws Exception {
		mockMvc.perform(post("/api/v1/orders")
				.header("Authorization", bearerToken())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "menuId": 9999
					}
					"""))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("MENU_NOT_FOUND"))
			.andExpect(jsonPath("$.data").doesNotExist());

		assertThat(countOrders()).isZero();
		verifyNoInteractions(dataCollector);
	}

	@Test
	void 메뉴_식별값이_0이면_400을_반환한다() throws Exception {
		mockMvc.perform(post("/api/v1/orders")
				.header("Authorization", bearerToken())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "menuId": 0
					}
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
			.andExpect(jsonPath("$.message").value("menuId는 1 이상이어야 합니다."))
			.andExpect(jsonPath("$.data").doesNotExist());
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

	private Long findOnlyOrderId() {
		return jdbcTemplate.queryForObject(
			"SELECT id FROM orders",
			Long.class
		);
	}

	private Long findOnlyOutboxId() {
		return jdbcTemplate.queryForObject(
			"SELECT id FROM order_event_outbox",
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

	private Long countSentOutbox() {
		return jdbcTemplate.queryForObject(
			"SELECT COUNT(*) FROM order_event_outbox WHERE status = 'SENT'",
			Long.class
		);
	}

	private String findOutboxPayload() {
		return jdbcTemplate.queryForObject(
			"SELECT payload FROM order_event_outbox",
			String.class
		);
	}

	private String bearerToken() {
		User user = userRepository.findById(USER_ID).orElseThrow();
		return "Bearer " + jwtTokenProvider.issue(user).value();
	}

	private int orderStatusAfterWaiting(
		String token,
		CountDownLatch ready,
		CountDownLatch start
	) throws Exception {
		ready.countDown();
		assertThat(start.await(1, TimeUnit.SECONDS)).isTrue();

		return mockMvc.perform(post("/api/v1/orders")
				.header("Authorization", token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "menuId": %d
					}
					""".formatted(MENU_ID)))
			.andReturn()
			.getResponse()
			.getStatus();
	}

	private void waitForOutboxStatus(String expectedStatus) throws InterruptedException {
		for (int attempt = 0; attempt < 20; attempt++) {
			if (expectedStatus.equals(findOutboxStatus())) {
				return;
			}

			Thread.sleep(50);
		}

		assertThat(findOutboxStatus()).isEqualTo(expectedStatus);
	}

	private void waitForSentOutboxCount(long expectedCount) throws InterruptedException {
		for (int attempt = 0; attempt < 20; attempt++) {
			if (countSentOutbox() == expectedCount) {
				return;
			}

			Thread.sleep(50);
		}

		assertThat(countSentOutbox()).isEqualTo(expectedCount);
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
