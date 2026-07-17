package com.example.cafe.auth.store;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisTokenBlacklistStore implements TokenBlacklistStore {

	private static final String KEY_PREFIX = "auth:blacklist:";
	private static final String BLACKLISTED_VALUE = "blacklisted";

	private final StringRedisTemplate redisTemplate;

	public RedisTokenBlacklistStore(StringRedisTemplate redisTemplate) {
		this.redisTemplate = redisTemplate;
	}

	@Override
	public void add(String tokenId, LocalDateTime expiresAt) {
		Duration ttl = Duration.between(LocalDateTime.now(ZoneOffset.UTC), expiresAt);
		if (ttl.isPositive()) {
			redisTemplate.opsForValue().set(KEY_PREFIX + tokenId, BLACKLISTED_VALUE, ttl);
		}
	}

	@Override
	public boolean contains(String tokenId) {
		return Boolean.TRUE.equals(redisTemplate.hasKey(KEY_PREFIX + tokenId));
	}
}
