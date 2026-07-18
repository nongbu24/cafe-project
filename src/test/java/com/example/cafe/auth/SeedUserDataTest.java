package com.example.cafe.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class SeedUserDataTest {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void 초기_회원은_관리자_3명과_일반회원_7명이다() {
		assertThat(countByStatus("ADMIN")).isEqualTo(3);
		assertThat(countByStatus("USER")).isEqualTo(7);
		assertThat(countAll()).isEqualTo(10);
		assertThat(countUnchangedUsers()).isEqualTo(10);
	}

	private Long countByStatus(String status) {
		return jdbcTemplate.queryForObject(
			"SELECT COUNT(*) FROM users WHERE user_status = ? AND is_deleted = FALSE",
			Long.class,
			status
		);
	}

	private Long countAll() {
		return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Long.class);
	}

	private Long countUnchangedUsers() {
		return jdbcTemplate.queryForObject(
			"SELECT COUNT(*) FROM users WHERE updated_at IS NULL",
			Long.class
		);
	}
}
