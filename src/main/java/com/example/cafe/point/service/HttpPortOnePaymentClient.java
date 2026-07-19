package com.example.cafe.point.service;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

@Component
public class HttpPortOnePaymentClient implements PortOnePaymentClient {

	private final PortOneProperties portOneProperties;
	private final RestClient restClient;

	public HttpPortOnePaymentClient(PortOneProperties portOneProperties) {
		this.portOneProperties = portOneProperties;
		this.restClient = RestClient.builder()
			.baseUrl("https://api.portone.io")
			.build();
	}

	@Override
	public PortOnePayment getPayment(String paymentId) {
		portOneProperties.validateApiConfig();

		try {
			PortOnePaymentResponse response = restClient.get()
				.uri("/payments/{paymentId}", paymentId)
				.header(HttpHeaders.AUTHORIZATION, "PortOne " + portOneProperties.getApiSecret())
				.retrieve()
				.body(PortOnePaymentResponse.class);

			return parse(response);
		} catch (HttpClientErrorException exception) {
			if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
				return null;
			}
			throw exception;
		}
	}

	private PortOnePayment parse(PortOnePaymentResponse response) {
		if (response == null || response.amount() == null) {
			throw new IllegalStateException("포트원 결제 조회 응답을 해석할 수 없습니다.");
		}

		return new PortOnePayment(
			response.id(),
			response.storeId(),
			response.status(),
			response.amount().total(),
			response.transactionId()
		);
	}

	private record PortOnePaymentResponse(
		String id,
		String storeId,
		String status,
		Amount amount,
		String transactionId
	) {
	}

	private record Amount(
		long total
	) {
	}
}
