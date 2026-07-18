package com.example.cafe.common.util;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

public final class DateTimeUtils {

	private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");

	private DateTimeUtils() {
	}

	public static OffsetDateTime toKoreaOffsetDateTime(LocalDateTime dateTime) {
		return dateTime.atZone(KOREA_ZONE).toOffsetDateTime();
	}
}
