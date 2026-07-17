package com.example.cafe.auth.store;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class RedisTokenBlacklistStoreTest {

	@Test
	void JWT의_남은_유효시간을_TTL로_저장한다() {
		StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
		@SuppressWarnings("unchecked")
		ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		RedisTokenBlacklistStore tokenBlacklistStore = new RedisTokenBlacklistStore(redisTemplate);
		LocalDateTime expiresAt = LocalDateTime.now(ZoneOffset.UTC).plusMinutes(10);

		tokenBlacklistStore.add("token-id", expiresAt);

		ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);
		verify(valueOperations).set(
			eq("auth:blacklist:token-id"),
			eq("blacklisted"),
			ttlCaptor.capture()
		);
		assertThat(ttlCaptor.getValue()).isPositive().isLessThanOrEqualTo(Duration.ofMinutes(10));
	}

	@Test
	void Redis에_키가_있으면_블랙리스트_토큰이다() {
		StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
		when(redisTemplate.hasKey(any(String.class))).thenReturn(true);
		RedisTokenBlacklistStore tokenBlacklistStore = new RedisTokenBlacklistStore(redisTemplate);

		assertThat(tokenBlacklistStore.contains("token-id")).isTrue();
		verify(redisTemplate).hasKey("auth:blacklist:token-id");
	}
}
