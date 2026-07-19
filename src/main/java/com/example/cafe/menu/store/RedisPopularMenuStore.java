package com.example.cafe.menu.store;

import com.example.cafe.menu.dto.PopularMenuCount;
import com.example.cafe.menu.dto.PopularMenuOrderItem;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;

@Component
public class RedisPopularMenuStore implements PopularMenuStore {

	private static final String KEY = "menu:popular:paid-items";
	private static final String SEPARATOR = ":";

	private final StringRedisTemplate redisTemplate;

	public RedisPopularMenuStore(StringRedisTemplate redisTemplate) {
		this.redisTemplate = redisTemplate;
	}

	@Override
	public void addPaidOrder(long orderId, LocalDateTime paidAt, List<PopularMenuOrderItem> items) {
		if (items.isEmpty()) {
			return;
		}

		ZSetOperations<String, String> zSetOperations = redisTemplate.opsForZSet();
		double score = toEpochMillis(paidAt);
		for (int index = 0; index < items.size(); index++) {
			PopularMenuOrderItem item = items.get(index);
			zSetOperations.add(KEY, toMember(orderId, index, item), score);
		}
	}

	@Override
	public List<PopularMenuCount> findPopularMenus(LocalDateTime from, LocalDateTime to, int limit) {
		if (limit < 1 || !from.isBefore(to)) {
			return List.of();
		}

		ZSetOperations<String, String> zSetOperations = redisTemplate.opsForZSet();
		double fromScore = toEpochMillis(from);
		double toScore = Math.nextDown(toEpochMillis(to));
		zSetOperations.removeRangeByScore(KEY, 0, Math.nextDown(fromScore));
		Set<String> members = zSetOperations.rangeByScore(KEY, fromScore, toScore);

		if (members == null || members.isEmpty()) {
			return List.of();
		}

		Map<Long, Long> orderCounts = new HashMap<>();
		for (String member : members) {
			toOrderItem(member).ifPresent(item ->
				orderCounts.merge(item.menuId(), (long) item.quantity(), Long::sum)
			);
		}

		return orderCounts.entrySet().stream()
			.map(entry -> new PopularMenuCount(entry.getKey(), entry.getValue()))
			.sorted(Comparator
				.comparingLong(PopularMenuCount::orderCount).reversed()
				.thenComparingLong(PopularMenuCount::menuId))
			.limit(limit)
			.toList();
	}

	private String toMember(long orderId, int index, PopularMenuOrderItem item) {
		return orderId + SEPARATOR + index + SEPARATOR + item.menuId() + SEPARATOR + item.quantity();
	}

	private Optional<PopularMenuOrderItem> toOrderItem(String member) {
		String[] values = member.split(SEPARATOR);
		if (values.length != 4) {
			return Optional.empty();
		}

		try {
			return Optional.of(new PopularMenuOrderItem(
				Long.parseLong(values[2]),
				Integer.parseInt(values[3])
			));
		} catch (NumberFormatException exception) {
			return Optional.empty();
		}
	}

	private double toEpochMillis(LocalDateTime dateTime) {
		return dateTime.toInstant(ZoneOffset.UTC).toEpochMilli();
	}
}
