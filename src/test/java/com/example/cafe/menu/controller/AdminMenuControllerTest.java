package com.example.cafe.menu.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
@Import(AdminMenuControllerTest.TokenBlacklistStoreTestConfig.class)
class AdminMenuControllerTest {

	private static final long ADMIN_ID = 1L;
	private static final long USER_ID = 2L;

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
		jdbcTemplate.update("DELETE FROM menus WHERE id > 100");
		jdbcTemplate.update("UPDATE menus SET status = 'AVAILABLE', updated_at = NULL WHERE id <= 80");
		jdbcTemplate.update("UPDATE menus SET status = 'SOLD_OUT', updated_at = NULL WHERE id BETWEEN 81 AND 90");
		jdbcTemplate.update("UPDATE menus SET status = 'DISCONTINUED', updated_at = NULL WHERE id BETWEEN 91 AND 100");
		jdbcTemplate.update(
			"""
			INSERT INTO users (id, username, password, user_status, point_balance, is_deleted, created_at, updated_at)
			VALUES (?, 'admin_menu_test', 'encoded', 'ADMIN', 0, FALSE, CURRENT_TIMESTAMP, NULL)
			""",
			ADMIN_ID
		);
		jdbcTemplate.update(
			"""
			INSERT INTO users (id, username, password, user_status, point_balance, is_deleted, created_at, updated_at)
			VALUES (?, 'user_menu_test', 'encoded', 'USER', 0, FALSE, CURRENT_TIMESTAMP, NULL)
			""",
			USER_ID
		);
	}

	@Test
	void 관리자는_메뉴를_생성할_수_있다() throws Exception {
		createMenu(ADMIN_ID, "바닐라 콜드브루", 5800)
			.andExpect(status().isCreated())
			.andExpect(header().exists("Location"))
			.andExpect(jsonPath("$.code").value("SUCCESS"))
			.andExpect(jsonPath("$.message").value("메뉴 생성이 완료되었습니다."))
			.andExpect(jsonPath("$.data.menuId").isNumber())
			.andExpect(jsonPath("$.data.name").value("바닐라 콜드브루"))
			.andExpect(jsonPath("$.data.price").value(5800))
			.andExpect(jsonPath("$.data.status").value("AVAILABLE"));
	}

	@Test
	void 관리자는_메뉴를_단종_상태로_변경할_수_있다() throws Exception {
		updateMenuStatus(ADMIN_ID, 1L, "DISCONTINUED")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("SUCCESS"))
			.andExpect(jsonPath("$.message").value("메뉴 상태 변경이 완료되었습니다."))
			.andExpect(jsonPath("$.data.menuId").value(1))
			.andExpect(jsonPath("$.data.status").value("DISCONTINUED"));
	}

	@Test
	void 관리자는_메뉴를_품절_상태로_변경할_수_있다() throws Exception {
		updateMenuStatus(ADMIN_ID, 1L, "SOLD_OUT")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.menuId").value(1))
			.andExpect(jsonPath("$.data.status").value("SOLD_OUT"));
	}

	@Test
	void 단종_처리한_메뉴는_회원_메뉴_목록에서_제외된다() throws Exception {
		updateMenuStatus(ADMIN_ID, 1L, "DISCONTINUED")
			.andExpect(status().isOk());

		mockMvc.perform(get("/api/v1/menus").param("size", "100"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.content", hasSize(89)))
			.andExpect(jsonPath("$.data.content[0].menuId").value(2))
			.andExpect(jsonPath("$.data.totalElements").value(89));
	}

	@Test
	void 관리자는_단종을_포함한_메뉴_목록을_조회할_수_있다() throws Exception {
		getAdminMenus(ADMIN_ID, "100", null)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("SUCCESS"))
			.andExpect(jsonPath("$.message").value("관리자 메뉴 목록 조회가 완료되었습니다."))
			.andExpect(jsonPath("$.data.content", hasSize(100)))
			.andExpect(jsonPath("$.data.content[0].menuId").value(1))
			.andExpect(jsonPath("$.data.content[0].orderCount").value(0))
			.andExpect(jsonPath("$.data.content[89].menuId").value(90))
			.andExpect(jsonPath("$.data.content[89].status").value("SOLD_OUT"))
			.andExpect(jsonPath("$.data.content[99].menuId").value(100))
			.andExpect(jsonPath("$.data.content[99].status").value("DISCONTINUED"))
			.andExpect(jsonPath("$.data.content[99].orderCount").value(0))
			.andExpect(jsonPath("$.data.totalElements").value(100))
			.andExpect(jsonPath("$.data.totalPages").value(1));
	}

	@Test
	void 관리자는_상태를_선택해서_메뉴_목록을_조회할_수_있다() throws Exception {
		insertPaidOrders(91L, 2);
		insertPaidOrders(100L, 1);

		getAdminMenus(ADMIN_ID, "20", "DISCONTINUED")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.content", hasSize(10)))
			.andExpect(jsonPath("$.data.content[0].menuId").value(91))
			.andExpect(jsonPath("$.data.content[0].status").value("DISCONTINUED"))
			.andExpect(jsonPath("$.data.content[0].orderCount").value(2))
			.andExpect(jsonPath("$.data.content[9].menuId").value(100))
			.andExpect(jsonPath("$.data.content[9].status").value("DISCONTINUED"))
			.andExpect(jsonPath("$.data.content[9].orderCount").value(1))
			.andExpect(jsonPath("$.data.totalElements").value(10))
			.andExpect(jsonPath("$.data.totalPages").value(1));
	}

	@Test
	void 일반_회원은_관리자_메뉴_목록을_조회할_수_없다() throws Exception {
		getAdminMenus(USER_ID, null, null)
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("FORBIDDEN"))
			.andExpect(jsonPath("$.message").value("접근 권한이 없습니다."))
			.andExpect(jsonPath("$.data").doesNotExist());
	}

	@Test
	void 인증하지_않으면_관리자_메뉴_목록을_조회할_수_없다() throws Exception {
		mockMvc.perform(get("/api/v1/admin/menus"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"))
			.andExpect(jsonPath("$.data").doesNotExist());
	}

	@Test
	void 존재하지_않는_메뉴_상태_변경은_404를_반환한다() throws Exception {
		updateMenuStatus(ADMIN_ID, 999L, "SOLD_OUT")
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("MENU_NOT_FOUND"))
			.andExpect(jsonPath("$.data").doesNotExist());
	}

	@Test
	void 메뉴_가격이_0이면_400을_반환한다() throws Exception {
		createMenu(ADMIN_ID, "잘못된 메뉴", 0)
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
			.andExpect(jsonPath("$.message").value("메뉴 가격은 1 이상이어야 합니다."));
	}

	@Test
	void 메뉴_이름이_비어_있으면_400을_반환한다() throws Exception {
		createMenu(ADMIN_ID, " ", 5800)
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
			.andExpect(jsonPath("$.message").value("메뉴 이름은 필수로 입력해야 합니다."));
	}

	@Test
	void 잘못된_메뉴_상태는_400을_반환한다() throws Exception {
		updateMenuStatus(ADMIN_ID, 1L, "UNKNOWN")
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
			.andExpect(jsonPath("$.message").value("요청 값이 올바르지 않습니다."));
	}

	@Test
	void 잘못된_목록_조회_상태는_400을_반환한다() throws Exception {
		getAdminMenus(ADMIN_ID, null, "UNKNOWN")
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
			.andExpect(jsonPath("$.message").value("요청 값이 올바르지 않습니다."));
	}

	private ResultActions createMenu(long userId, String name, long price) throws Exception {
		return mockMvc.perform(post("/api/v1/admin/menus")
			.header("Authorization", bearerToken(userId))
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
				{
				  "name": "%s",
				  "price": %d
				}
				""".formatted(name, price)));
	}

	private ResultActions updateMenuStatus(long userId, long menuId, String status) throws Exception {
		return mockMvc.perform(patch("/api/v1/admin/menus/{menuId}/status", menuId)
			.header("Authorization", bearerToken(userId))
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
				{
				  "status": "%s"
				}
				""".formatted(status)));
	}

	private ResultActions getAdminMenus(long userId, String size, String status) throws Exception {
		var requestBuilder = get("/api/v1/admin/menus")
			.header("Authorization", bearerToken(userId));

		if (size != null) {
			requestBuilder.param("size", size);
		}

		if (status != null) {
			requestBuilder.param("status", status);
		}

		return mockMvc.perform(requestBuilder);
	}

	private void insertPaidOrders(long menuId, int count) {
		for (int i = 0; i < count; i++) {
			Long orderId = nextOrderId();
			jdbcTemplate.update(
				"""
				INSERT INTO orders (id, user_id, payment_amount, status, paid_at, created_at)
				SELECT ?, ?, price, 'PAID', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
				FROM menus
				WHERE id = ?
				""",
				orderId,
				USER_ID,
				menuId
			);
			jdbcTemplate.update(
				"""
				INSERT INTO order_items (order_id, menu_id, menu_name, unit_price, quantity)
				SELECT ?, id, name, price, 1
				FROM menus
				WHERE id = ?
				""",
				orderId,
				menuId
			);
		}
	}

	private Long nextOrderId() {
		return jdbcTemplate.queryForObject("SELECT COALESCE(MAX(id), 0) + 1 FROM orders", Long.class);
	}

	private String bearerToken(long userId) {
		User user = userRepository.findById(userId).orElseThrow();
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
