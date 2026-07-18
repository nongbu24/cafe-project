package com.example.cafe.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.cafe.auth.store.TokenBlacklistStore;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Import(AuthControllerTest.TokenBlacklistStoreTestConfig.class)
class AuthControllerTest {

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
	}

	@Test
	void 회원가입하면_USER_상태와_암호화된_비밀번호를_저장한다() throws Exception {
		mockMvc.perform(post("/api/v1/auth/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content(credentials("new_user", "Cafe1234!")))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.code").value("SUCCESS"))
			.andExpect(jsonPath("$.data.username").value("new_user"))
			.andExpect(jsonPath("$.data.userStatus").value("USER"));

		String storedPassword = jdbcTemplate.queryForObject(
			"SELECT password FROM users WHERE username = 'new_user'",
			String.class
		);
		assertThat(storedPassword).startsWith("$2").isNotEqualTo("Cafe1234!");

		Long unchangedUsers = jdbcTemplate.queryForObject(
			"SELECT COUNT(*) FROM users WHERE username = 'new_user' AND updated_at IS NULL",
			Long.class
		);
		assertThat(unchangedUsers).isEqualTo(1);
	}

	@Test
	void 중복_username으로_가입할_수_없다() throws Exception {
		signup("same_user", "Cafe1234!");

		mockMvc.perform(post("/api/v1/auth/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content(credentials("same_user", "Cafe1234!")))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("DUPLICATE_USERNAME"))
			.andExpect(jsonPath("$.data").doesNotExist());
	}

	@Test
	void 비밀번호에는_한글과_공백을_사용할_수_없다() throws Exception {
		mockMvc.perform(post("/api/v1/auth/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content(credentials("korean_user", "비밀번호123!")))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

		mockMvc.perform(post("/api/v1/auth/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content(credentials("space_user", "Cafe 1234!")))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
	}

	@Test
	void 비밀번호는_최대_64자까지_사용할_수_있다() throws Exception {
		mockMvc.perform(post("/api/v1/auth/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content(credentials("password_64", "A".repeat(64))))
			.andExpect(status().isCreated());

		mockMvc.perform(post("/api/v1/auth/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content(credentials("password_65", "A".repeat(65))))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
	}

	@Test
	void 로그인하면_JWT를_발급한다() throws Exception {
		signup("login_user", "Cafe1234!");

		mockMvc.perform(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(credentials("login_user", "Cafe1234!")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.tokenType").value("Bearer"))
			.andExpect(jsonPath("$.data.accessToken").isNotEmpty())
			.andExpect(jsonPath("$.data.expiresAt").doesNotExist());
	}

	@Test
	void 로그아웃한_JWT는_블랙리스트에_등록되어_다시_사용할_수_없다() throws Exception {
		signup("logout_user", "Cafe1234!");
		String token = login("logout_user", "Cafe1234!");

		mockMvc.perform(post("/api/v1/auth/logout")
				.header("Authorization", "Bearer " + token))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("SUCCESS"))
			.andExpect(jsonPath("$.message").value("로그아웃이 완료되었습니다."))
			.andExpect(jsonPath("$.data").doesNotExist());

		mockMvc.perform(post("/api/v1/auth/logout")
				.header("Authorization", "Bearer " + token))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("BLACKLISTED_TOKEN"));
	}

	@Test
	void 회원탈퇴하면_계정과_현재_JWT를_사용할_수_없다() throws Exception {
		signup("withdraw_user", "Cafe1234!");
		String token = login("withdraw_user", "Cafe1234!");

		mockMvc.perform(patch("/api/v1/users/me")
				.header("Authorization", "Bearer " + token))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("SUCCESS"))
			.andExpect(jsonPath("$.message").value("회원 탈퇴가 완료되었습니다."))
			.andExpect(jsonPath("$.data").doesNotExist());

		Boolean deleted = jdbcTemplate.queryForObject(
			"SELECT is_deleted FROM users WHERE username = 'withdraw_user'",
			Boolean.class
		);
		assertThat(deleted).isTrue();

		mockMvc.perform(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(credentials("withdraw_user", "Cafe1234!")))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
	}

	private void signup(String username, String password) throws Exception {
		mockMvc.perform(post("/api/v1/auth/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content(credentials(username, password)))
			.andExpect(status().isCreated());
	}

	private String login(String username, String password) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(credentials(username, password)))
			.andExpect(status().isOk())
			.andReturn();

		return new ObjectMapper()
			.readTree(result.getResponse().getContentAsString())
			.path("data")
			.path("accessToken")
			.asText();
	}

	private String credentials(String username, String password) {
		return """
			{
			  "username": "%s",
			  "password": "%s"
			}
			""".formatted(username, password);
	}

	@TestConfiguration(proxyBeanMethods = false)
	static class TokenBlacklistStoreTestConfig {

		@Bean
		@Primary
		TokenBlacklistStore tokenBlacklistStore() {
			return new InMemoryTokenBlacklistStore();
		}
	}

	static class InMemoryTokenBlacklistStore implements TokenBlacklistStore {

		private final Map<String, LocalDateTime> tokens = new ConcurrentHashMap<>();

		@Override
		public void add(String tokenId, LocalDateTime expiresAt) {
			tokens.put(tokenId, expiresAt);
		}

		@Override
		public boolean contains(String tokenId) {
			LocalDateTime expiresAt = tokens.get(tokenId);
			return expiresAt != null && expiresAt.isAfter(LocalDateTime.now(ZoneOffset.UTC));
		}
	}
}
