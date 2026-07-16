package com.example.cafe.menu;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class MenuControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void 기본값으로_첫_페이지의_메뉴_10개를_조회한다() throws Exception {
		mockMvc.perform(get("/api/v1/menus"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("SUCCESS"))
			.andExpect(jsonPath("$.message").value("요청이 성공적으로 처리되었습니다."))
			.andExpect(jsonPath("$.data.content", hasSize(10)))
			.andExpect(jsonPath("$.data.content[0].menuId").value(1))
			.andExpect(jsonPath("$.data.content[0].name").value("아메리카노"))
			.andExpect(jsonPath("$.data.content[0].price").value(4500))
			.andExpect(jsonPath("$.data.content[0].status").value("AVAILABLE"))
			.andExpect(jsonPath("$.data.content[9].menuId").value(10))
			.andExpect(jsonPath("$.data.page").value(0))
			.andExpect(jsonPath("$.data.size").value(10))
			.andExpect(jsonPath("$.data.totalElements").value(90))
			.andExpect(jsonPath("$.data.totalPages").value(9))
			.andExpect(jsonPath("$.data.first").value(true))
			.andExpect(jsonPath("$.data.last").value(false));
	}

	@Test
	void 마지막_페이지를_조회한다() throws Exception {
		mockMvc.perform(get("/api/v1/menus")
				.param("page", "8")
				.param("size", "10"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.content", hasSize(10)))
			.andExpect(jsonPath("$.data.content[0].menuId").value(81))
			.andExpect(jsonPath("$.data.content[0].status").value("SOLD_OUT"))
			.andExpect(jsonPath("$.data.content[9].menuId").value(90))
			.andExpect(jsonPath("$.data.content[9].status").value("SOLD_OUT"))
			.andExpect(jsonPath("$.data.first").value(false))
			.andExpect(jsonPath("$.data.last").value(true));
	}

	@Test
	void 페이지_크기는_최대_100개까지_조회할_수_있다() throws Exception {
		mockMvc.perform(get("/api/v1/menus").param("size", "100"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.content", hasSize(90)))
			.andExpect(jsonPath("$.data.content[89].menuId").value(90))
			.andExpect(jsonPath("$.data.totalPages").value(1));
	}

	@Test
	void 잘못된_페이지_크기는_400을_반환한다() throws Exception {
		mockMvc.perform(get("/api/v1/menus").param("size", "101"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
			.andExpect(jsonPath("$.message").value("size는 1 이상 100 이하여야 합니다."))
			.andExpect(jsonPath("$.data").isEmpty());
	}

	@Test
	void 페이지_크기가_0이면_400을_반환한다() throws Exception {
		mockMvc.perform(get("/api/v1/menus").param("size", "0"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
	}

	@Test
	void 페이지가_음수이면_400을_반환한다() throws Exception {
		mockMvc.perform(get("/api/v1/menus").param("page", "-1"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
			.andExpect(jsonPath("$.message").value("page는 0 이상이어야 합니다."))
			.andExpect(jsonPath("$.data").isEmpty());
	}

	@Test
	void 페이지_값의_타입이_잘못되면_공통_오류_응답을_반환한다() throws Exception {
		mockMvc.perform(get("/api/v1/menus").param("page", "잘못된 값"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
			.andExpect(jsonPath("$.message").value("요청 값이 올바르지 않습니다."))
			.andExpect(jsonPath("$.data").isEmpty());
	}

	@Test
	void 초기_메뉴의_수정_시각은_비어_있다() {
		Long count = jdbcTemplate.queryForObject(
			"SELECT COUNT(*) FROM menus WHERE updated_at IS NULL",
			Long.class
		);

		assertThat(count).isEqualTo(100);
	}

	@Test
	void 초기_메뉴는_상태별로_저장된다() {
		assertThat(countMenusByStatus("AVAILABLE")).isEqualTo(80);
		assertThat(countMenusByStatus("SOLD_OUT")).isEqualTo(10);
		assertThat(countMenusByStatus("DISCONTINUED")).isEqualTo(10);
	}

	private Long countMenusByStatus(String status) {
		return jdbcTemplate.queryForObject(
			"SELECT COUNT(*) FROM menus WHERE status = ?",
			Long.class,
			status
		);
	}
}
