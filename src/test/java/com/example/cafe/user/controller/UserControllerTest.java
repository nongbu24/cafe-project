package com.example.cafe.user.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(UserControllerTest.TokenBlacklistStoreTestConfig.class)
class UserControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private JwtTokenProvider jwtTokenProvider;

	@Autowired
	private UserRepository userRepository;

	private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

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
	}

	@Test
	void 회원은_내_정보를_조회할_수_있다() throws Exception {
		User user = saveUser("my_user", "Cafe1234!");
		user.charge(2500);
		userRepository.saveAndFlush(user);

		mockMvc.perform(get("/api/v1/users/me")
				.header("Authorization", bearerToken(user)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("SUCCESS"))
			.andExpect(jsonPath("$.message").value("내 정보 조회가 완료되었습니다."))
			.andExpect(jsonPath("$.data.userId").value(user.getId()))
			.andExpect(jsonPath("$.data.username").value("my_user"))
			.andExpect(jsonPath("$.data.userStatus").value("USER"))
			.andExpect(jsonPath("$.data.pointBalance").value(2500))
			.andExpect(jsonPath("$.data.createdAt").isNotEmpty())
			.andExpect(jsonPath("$.data.updatedAt").isNotEmpty())
			.andExpect(jsonPath("$.data.password").doesNotExist())
			.andExpect(jsonPath("$.data.deleted").doesNotExist());
	}

	@Test
	void 인증하지_않으면_내_정보를_조회할_수_없다() throws Exception {
		mockMvc.perform(get("/api/v1/users/me"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
	}

	@Test
	void 회원은_현재_비밀번호를_확인한_뒤_비밀번호를_변경할_수_있다() throws Exception {
		User user = saveUser("password_user", "Cafe1234!");

		mockMvc.perform(patch("/api/v1/users/me/password")
				.header("Authorization", bearerToken(user))
				.contentType(MediaType.APPLICATION_JSON)
				.content(passwordChange("Cafe1234!", "NewCafe1234!")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("SUCCESS"))
			.andExpect(jsonPath("$.message").value("비밀번호 변경이 완료되었습니다."))
			.andExpect(jsonPath("$.data").doesNotExist());

		String changedPassword = jdbcTemplate.queryForObject(
			"SELECT password FROM users WHERE username = 'password_user'",
			String.class
		);
		assertThat(changedPassword).startsWith("$2").isNotEqualTo("NewCafe1234!");

		mockMvc.perform(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(credentials("password_user", "Cafe1234!")))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));

		mockMvc.perform(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(credentials("password_user", "NewCafe1234!")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.accessToken").isNotEmpty());
	}

	@Test
	void 현재_비밀번호가_틀리면_비밀번호를_변경할_수_없다() throws Exception {
		User user = saveUser("wrong_password_user", "Cafe1234!");

		mockMvc.perform(patch("/api/v1/users/me/password")
				.header("Authorization", bearerToken(user))
				.contentType(MediaType.APPLICATION_JSON)
				.content(passwordChange("Wrong1234!", "NewCafe1234!")))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("INVALID_PASSWORD"))
			.andExpect(jsonPath("$.message").value("비밀번호가 올바르지 않습니다."));

		String storedPassword = jdbcTemplate.queryForObject(
			"SELECT password FROM users WHERE username = 'wrong_password_user'",
			String.class
		);
		assertThat(passwordEncoder.matches("Cafe1234!", storedPassword)).isTrue();
	}

	@Test
	void 새_비밀번호가_정책에_맞지_않으면_400을_반환한다() throws Exception {
		User user = saveUser("invalid_new_password_user", "Cafe1234!");

		mockMvc.perform(patch("/api/v1/users/me/password")
				.header("Authorization", bearerToken(user))
				.contentType(MediaType.APPLICATION_JSON)
				.content(passwordChange("Cafe1234!", "short")))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
	}

	private User saveUser(String username, String password) {
		User user = User.signup(username, passwordEncoder.encode(password));
		return userRepository.saveAndFlush(user);
	}

	private String bearerToken(User user) {
		return "Bearer " + jwtTokenProvider.issue(user).value();
	}

	private String credentials(String username, String password) {
		return """
			{
			  "username": "%s",
			  "password": "%s"
			}
			""".formatted(username, password);
	}

	private String passwordChange(String currentPassword, String newPassword) {
		return """
			{
			  "currentPassword": "%s",
			  "newPassword": "%s"
			}
			""".formatted(currentPassword, newPassword);
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
