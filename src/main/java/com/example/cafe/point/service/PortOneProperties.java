package com.example.cafe.point.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class PortOneProperties {

	private final String storeId;
	private final String channelKey;
	private final String apiSecret;

	public PortOneProperties(
		@Value("${app.portone.store-id:}") String storeId,
		@Value("${app.portone.channel-key:}") String channelKey,
		@Value("${app.portone.api-secret:}") String apiSecret
	) {
		this.storeId = storeId;
		this.channelKey = channelKey;
		this.apiSecret = apiSecret;
	}

	public String getStoreId() {
		return storeId;
	}

	public String getChannelKey() {
		return channelKey;
	}

	public String getApiSecret() {
		return apiSecret;
	}

	public void validatePaymentRequestConfig() {
		if (!StringUtils.hasText(storeId) || !StringUtils.hasText(channelKey)) {
			throw new IllegalStateException("포트원 상점 아이디와 채널 키 설정이 필요합니다.");
		}
	}

	public void validateApiConfig() {
		if (!StringUtils.hasText(apiSecret)) {
			throw new IllegalStateException("포트원 API Secret 설정이 필요합니다.");
		}
	}
}
