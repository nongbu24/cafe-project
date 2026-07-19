package com.example.cafe.menu.store;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.cafe.menu.dto.PopularMenuCount;
import com.example.cafe.menu.dto.PopularMenuOrderItem;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashSet;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

class RedisPopularMenuStoreTest {

	@Test
	void 결제_주문_항목을_주문시각_score로_Redis_ZSET에_저장한다() {
		StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
		@SuppressWarnings("unchecked")
		ZSetOperations<String, String> zSetOperations = mock(ZSetOperations.class);
		when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
		RedisPopularMenuStore popularMenuStore = new RedisPopularMenuStore(redisTemplate);
		LocalDateTime paidAt = LocalDateTime.of(2026, 7, 19, 10, 30);

		popularMenuStore.addPaidOrder(10, paidAt, List.of(
			new PopularMenuOrderItem(1, 2),
			new PopularMenuOrderItem(2, 1)
		));

		double score = paidAt.toInstant(ZoneOffset.UTC).toEpochMilli();
		verify(zSetOperations).add("menu:popular:paid-items", "10:0:1:2", score);
		verify(zSetOperations).add("menu:popular:paid-items", "10:1:2:1", score);
	}

	@Test
	void 최근_기간의_주문_항목을_메뉴별_수량으로_합산하고_상위_메뉴를_반환한다() {
		StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
		@SuppressWarnings("unchecked")
		ZSetOperations<String, String> zSetOperations = mock(ZSetOperations.class);
		when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
		when(zSetOperations.rangeByScore(eq("menu:popular:paid-items"), eq(1000.0), eq(Math.nextDown(2000.0))))
			.thenReturn(new LinkedHashSet<>(List.of(
				"1:0:2:2",
				"2:0:1:3",
				"3:0:2:2",
				"4:0:3:4",
				"잘못된값"
			)));
		RedisPopularMenuStore popularMenuStore = new RedisPopularMenuStore(redisTemplate);

		List<PopularMenuCount> result = popularMenuStore.findPopularMenus(
			LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(1000), ZoneOffset.UTC),
			LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(2000), ZoneOffset.UTC),
			2
		);

		assertThat(result).containsExactly(
			new PopularMenuCount(2, 4),
			new PopularMenuCount(3, 4)
		);
		verify(zSetOperations).removeRangeByScore("menu:popular:paid-items", 0, Math.nextDown(1000.0));
	}
}
