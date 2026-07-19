package com.example.cafe.cart.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
@Import(CartControllerTest.TokenBlacklistStoreTestConfig.class)
class CartControllerTest {

	private static final long USER_ID = 1L;
	private static final long MENU_ID = 10L;
	private static final long OTHER_MENU_ID = 11L;

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
		jdbcTemplate.update("DELETE FROM point_charge_payment");
		jdbcTemplate.update("DELETE FROM point_transaction");
		jdbcTemplate.update("DELETE FROM order_items");
		jdbcTemplate.update("DELETE FROM orders");
		jdbcTemplate.update("DELETE FROM cart_items");
		jdbcTemplate.update("DELETE FROM carts");
		jdbcTemplate.update("DELETE FROM users");
		jdbcTemplate.update("DELETE FROM menus");
		jdbcTemplate.update(
			"""
			INSERT INTO users (id, username, password, user_status, point_balance, is_deleted, created_at, updated_at)
			VALUES (?, 'cart_test', 'encoded', 'USER', 0, FALSE, CURRENT_TIMESTAMP, NULL)
			""",
			USER_ID
		);
		jdbcTemplate.update(
			"""
			INSERT INTO carts (id, user_id, created_at, updated_at)
			VALUES (100, ?, CURRENT_TIMESTAMP, NULL)
			""",
			USER_ID
		);
		insertMenu(MENU_ID, "카페라테", 5000);
		insertMenu(OTHER_MENU_ID, "아메리카노", 4500);
	}

	@Test
	void 회원은_장바구니를_조회할_수_있다() throws Exception {
		insertCartItem(MENU_ID, 2);
		insertCartItem(OTHER_MENU_ID, 1);

		mockMvc.perform(get("/api/v1/carts/me")
				.header("Authorization", bearerToken()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("SUCCESS"))
			.andExpect(jsonPath("$.message").value("장바구니 조회가 완료되었습니다."))
			.andExpect(jsonPath("$.data.userId").value(USER_ID))
			.andExpect(jsonPath("$.data.items[0].menuId").value(MENU_ID))
			.andExpect(jsonPath("$.data.items[0].name").value("카페라테"))
			.andExpect(jsonPath("$.data.items[0].unitPrice").value(5000))
			.andExpect(jsonPath("$.data.items[0].quantity").value(2))
			.andExpect(jsonPath("$.data.items[0].lineAmount").value(10000))
			.andExpect(jsonPath("$.data.items[1].menuId").value(OTHER_MENU_ID))
			.andExpect(jsonPath("$.data.items[1].name").value("아메리카노"))
			.andExpect(jsonPath("$.data.items[1].unitPrice").value(4500))
			.andExpect(jsonPath("$.data.items[1].quantity").value(1))
			.andExpect(jsonPath("$.data.items[1].lineAmount").value(4500))
			.andExpect(jsonPath("$.data.totalAmount").value(14500));
	}

	@Test
	void 빈_장바구니를_조회하면_빈_목록과_총액_0을_반환한다() throws Exception {
		mockMvc.perform(get("/api/v1/carts/me")
				.header("Authorization", bearerToken()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("장바구니 조회가 완료되었습니다."))
			.andExpect(jsonPath("$.data.userId").value(USER_ID))
			.andExpect(jsonPath("$.data.items").isArray())
			.andExpect(jsonPath("$.data.items").isEmpty())
			.andExpect(jsonPath("$.data.totalAmount").value(0));
	}

	@Test
	void 수량을_1_이상으로_변경하면_장바구니에_담거나_수량을_변경한다() throws Exception {
		updateQuantity(MENU_ID, 3)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("SUCCESS"))
			.andExpect(jsonPath("$.message").value("장바구니 수량이 변경되었습니다."))
			.andExpect(jsonPath("$.data.menuId").value(MENU_ID))
			.andExpect(jsonPath("$.data.name").value("카페라테"))
			.andExpect(jsonPath("$.data.unitPrice").value(5000))
			.andExpect(jsonPath("$.data.quantity").value(3))
			.andExpect(jsonPath("$.data.lineAmount").value(15000));

		assertThat(findCartItemQuantity(MENU_ID)).isEqualTo(3);

		updateQuantity(MENU_ID, 2)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.quantity").value(2))
			.andExpect(jsonPath("$.data.lineAmount").value(10000));

		assertThat(findCartItemQuantity(MENU_ID)).isEqualTo(2);
		assertThat(countCartItems()).isEqualTo(1);
	}

	@Test
	void 수량을_0으로_변경하면_장바구니_항목을_삭제한다() throws Exception {
		insertCartItem(MENU_ID, 2);

		updateQuantity(MENU_ID, 0)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("SUCCESS"))
			.andExpect(jsonPath("$.message").value("장바구니 항목이 삭제되었습니다."))
			.andExpect(jsonPath("$.data").doesNotExist());

		assertThat(countCartItems()).isZero();
	}

	@Test
	void 회원은_장바구니를_전체_비울_수_있다() throws Exception {
		insertCartItem(MENU_ID, 2);
		insertCartItem(OTHER_MENU_ID, 1);

		mockMvc.perform(delete("/api/v1/carts/me/items")
				.header("Authorization", bearerToken()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("SUCCESS"))
			.andExpect(jsonPath("$.message").value("장바구니가 비워졌습니다."))
			.andExpect(jsonPath("$.data").doesNotExist());

		assertThat(countCartItems()).isZero();
	}

	@Test
	void 수량이_음수이면_400을_반환한다() throws Exception {
		updateQuantity(MENU_ID, -1)
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
			.andExpect(jsonPath("$.message").value("수량은 0 이상이어야 합니다."))
			.andExpect(jsonPath("$.data").doesNotExist());
	}

	@Test
	void 메뉴_식별값이_0이면_존재하지_않는_메뉴_오류를_반환한다() throws Exception {
		updateQuantity(0, 1)
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("MENU_NOT_FOUND"))
			.andExpect(jsonPath("$.message").value("메뉴를 찾을 수 없습니다."))
			.andExpect(jsonPath("$.data").doesNotExist());
	}

	@Test
	void 메뉴가_존재하지_않으면_404를_반환한다() throws Exception {
		updateQuantity(9999, 1)
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("MENU_NOT_FOUND"))
			.andExpect(jsonPath("$.data").doesNotExist());
	}

	@Test
	void 인증하지_않으면_장바구니를_조회할_수_없다() throws Exception {
		mockMvc.perform(get("/api/v1/carts/me"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
	}

	@Test
	void 인증하지_않으면_장바구니를_변경할_수_없다() throws Exception {
		mockMvc.perform(patch("/api/v1/carts/me/items/{menuId}", MENU_ID)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "quantity": 1
					}
					"""))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
	}

	private org.springframework.test.web.servlet.ResultActions updateQuantity(long menuId, int quantity) throws Exception {
		return mockMvc.perform(patch("/api/v1/carts/me/items/{menuId}", menuId)
			.header("Authorization", bearerToken())
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
				{
				  "quantity": %d
				}
				""".formatted(quantity)));
	}

	private void insertMenu(long menuId, String name, long price) {
		jdbcTemplate.update(
			"""
			INSERT INTO menus (id, name, price, status, created_at, updated_at)
			VALUES (?, ?, ?, 'AVAILABLE', CURRENT_TIMESTAMP, NULL)
			""",
			menuId,
			name,
			price
		);
	}

	private void insertCartItem(long menuId, int quantity) {
		jdbcTemplate.update(
			"""
			INSERT INTO cart_items (cart_id, menu_id, quantity, created_at, updated_at)
			VALUES (100, ?, ?, CURRENT_TIMESTAMP, NULL)
			""",
			menuId,
			quantity
		);
	}

	private Integer findCartItemQuantity(long menuId) {
		return jdbcTemplate.queryForObject(
			"SELECT quantity FROM cart_items WHERE cart_id = 100 AND menu_id = ?",
			Integer.class,
			menuId
		);
	}

	private Long countCartItems() {
		return jdbcTemplate.queryForObject(
			"SELECT COUNT(*) FROM cart_items WHERE cart_id = 100",
			Long.class
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
