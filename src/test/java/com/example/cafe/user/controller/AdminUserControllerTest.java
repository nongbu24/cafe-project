package com.example.cafe.user.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(AdminUserControllerTest.TokenBlacklistStoreTestConfig.class)
class AdminUserControllerTest {

	private static final long ADMIN_ID = 1L;
	private static final long USER_ID = 2L;
	private static final long WITHDRAWN_USER_ID = 3L;

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
			VALUES (?, 'admin_test', 'encoded', 'ADMIN', 0, FALSE, CURRENT_TIMESTAMP, NULL)
			""",
			ADMIN_ID
		);
		jdbcTemplate.update(
			"""
			INSERT INTO users (id, username, password, user_status, point_balance, is_deleted, created_at, updated_at)
			VALUES (?, 'user_test', 'encoded', 'USER', 3500, FALSE, CURRENT_TIMESTAMP, NULL)
			""",
			USER_ID
		);
		jdbcTemplate.update(
			"""
			INSERT INTO users (id, username, password, user_status, point_balance, is_deleted, created_at, updated_at)
			VALUES (?, 'withdrawn_test', 'encoded', 'USER', 1000, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
			""",
			WITHDRAWN_USER_ID
		);
	}

	@Test
	void 관리자는_회원_목록을_조회할_수_있다() throws Exception {
		mockMvc.perform(get("/api/v1/admin/users")
				.header("Authorization", bearerToken(ADMIN_ID)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("SUCCESS"))
			.andExpect(jsonPath("$.message").value("회원 목록 조회가 완료되었습니다."))
			.andExpect(jsonPath("$.data.content", hasSize(3)))
			.andExpect(jsonPath("$.data.content[0].userId").value(ADMIN_ID))
			.andExpect(jsonPath("$.data.content[0].username").value("admin_test"))
			.andExpect(jsonPath("$.data.content[0].userStatus").value("ADMIN"))
			.andExpect(jsonPath("$.data.content[0].pointBalance").doesNotExist())
			.andExpect(jsonPath("$.data.content[0].deleted").doesNotExist())
			.andExpect(jsonPath("$.data.content[1].userId").value(USER_ID))
			.andExpect(jsonPath("$.data.content[1].username").value("user_test"))
			.andExpect(jsonPath("$.data.content[1].userStatus").value("USER"))
			.andExpect(jsonPath("$.data.page").value(0))
			.andExpect(jsonPath("$.data.size").value(10))
			.andExpect(jsonPath("$.data.totalElements").value(3))
			.andExpect(jsonPath("$.data.totalPages").value(1))
			.andExpect(jsonPath("$.data.first").value(true))
			.andExpect(jsonPath("$.data.last").value(true));
	}

	@Test
	void 관리자는_회원_단건을_조회할_수_있다() throws Exception {
		mockMvc.perform(get("/api/v1/admin/users/{userId}", USER_ID)
				.header("Authorization", bearerToken(ADMIN_ID)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("회원 조회가 완료되었습니다."))
			.andExpect(jsonPath("$.data.userId").value(USER_ID))
			.andExpect(jsonPath("$.data.username").value("user_test"))
			.andExpect(jsonPath("$.data.userStatus").value("USER"))
			.andExpect(jsonPath("$.data.pointBalance").value(3500))
			.andExpect(jsonPath("$.data.deleted").value(false))
			.andExpect(jsonPath("$.data.createdAt").isNotEmpty());
	}

	@Test
	void 관리자는_탈퇴한_회원도_단건_조회할_수_있다() throws Exception {
		mockMvc.perform(get("/api/v1/admin/users/{userId}", WITHDRAWN_USER_ID)
				.header("Authorization", bearerToken(ADMIN_ID)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.userId").value(WITHDRAWN_USER_ID))
			.andExpect(jsonPath("$.data.username").value("withdrawn_test"))
			.andExpect(jsonPath("$.data.deleted").value(true));
	}

	@Test
	void 일반_회원은_관리자_회원_목록을_조회할_수_없다() throws Exception {
		mockMvc.perform(get("/api/v1/admin/users")
				.header("Authorization", bearerToken(USER_ID)))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("FORBIDDEN"))
			.andExpect(jsonPath("$.message").value("접근 권한이 없습니다."))
			.andExpect(jsonPath("$.data").doesNotExist());
	}

	@Test
	void 인증하지_않으면_관리자_회원_목록을_조회할_수_없다() throws Exception {
		mockMvc.perform(get("/api/v1/admin/users"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"))
			.andExpect(jsonPath("$.data").doesNotExist());
	}

	@Test
	void 존재하지_않는_회원_단건_조회는_404를_반환한다() throws Exception {
		mockMvc.perform(get("/api/v1/admin/users/{userId}", 999L)
				.header("Authorization", bearerToken(ADMIN_ID)))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("USER_NOT_FOUND"))
			.andExpect(jsonPath("$.data").doesNotExist());
	}

	@Test
	void 잘못된_페이지_크기는_400을_반환한다() throws Exception {
		mockMvc.perform(get("/api/v1/admin/users")
				.header("Authorization", bearerToken(ADMIN_ID))
				.param("size", "101"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
			.andExpect(jsonPath("$.message").value("size는 1 이상 100 이하여야 합니다."));
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
