package com.example.cafe.order.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.cafe.menu.dto.PopularMenuOrderCount;
import com.example.cafe.order.entity.OrderStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
class OrderRepositoryImplTest {

	private static final long USER_ID = 101L;
	private static final long MENU_ID = 201L;
	private static final long EXCLUDED_MENU_ID = 202L;

	@Autowired
	private OrderRepository orderRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void setUp() {
		jdbcTemplate.update("DELETE FROM order_event_outbox");
		jdbcTemplate.update("DELETE FROM point_transaction");
		jdbcTemplate.update("DELETE FROM orders");
		jdbcTemplate.update("DELETE FROM menus");
		jdbcTemplate.update("DELETE FROM users");
		jdbcTemplate.update(
			"""
			INSERT INTO users (id, username, password, user_status, point_balance, is_deleted, created_at, updated_at)
			VALUES (?, 'popular_boundary_user', 'encoded', 'USER', 0, FALSE, CURRENT_TIMESTAMP, NULL)
			""",
			USER_ID
		);
		insertMenu(MENU_ID, "경계 테스트 메뉴");
		insertMenu(EXCLUDED_MENU_ID, "제외 테스트 메뉴");
	}

	@Test
	void 인기_메뉴_집계는_시작_시각을_포함하고_종료_시각은_제외한다() {
		LocalDateTime from = LocalDateTime.of(2026, 7, 10, 0, 0);
		LocalDateTime to = LocalDateTime.of(2026, 7, 17, 0, 0);

		insertOrder(MENU_ID, from);
		insertOrder(MENU_ID, to.minusSeconds(1));
		insertOrder(EXCLUDED_MENU_ID, from.minusSeconds(1));
		insertOrder(EXCLUDED_MENU_ID, to);

		List<PopularMenuOrderCount> result = orderRepository.findPopularMenus(
			OrderStatus.PAID,
			from,
			to,
			PageRequest.of(0, 3)
		);

		assertThat(result).hasSize(1);
		assertThat(result.getFirst().menuId()).isEqualTo(MENU_ID);
		assertThat(result.getFirst().orderCount()).isEqualTo(2);
	}

	private void insertMenu(long menuId, String name) {
		jdbcTemplate.update(
			"""
			INSERT INTO menus (id, name, price, status, created_at, updated_at)
			VALUES (?, ?, 5000, 'AVAILABLE', CURRENT_TIMESTAMP, NULL)
			""",
			menuId,
			name
		);
	}

	private void insertOrder(long menuId, LocalDateTime paidAt) {
		jdbcTemplate.update(
			"""
			INSERT INTO orders (user_id, menu_id, menu_name, payment_amount, status, paid_at, created_at)
			SELECT ?, id, name, price, 'PAID', ?, CURRENT_TIMESTAMP
			FROM menus
			WHERE id = ?
			""",
			USER_ID,
			paidAt,
			menuId
		);
	}
}
