package com.example.cafe.auth.service;

import com.example.cafe.common.exception.ApplicationException;
import com.example.cafe.common.exception.ErrorCode;
import com.example.cafe.user.entity.User;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

@Component
public class JwtTokenProvider {

	private static final String HMAC_ALGORITHM = "HmacSHA256";
	private static final Base64.Encoder BASE64_URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
	private static final Base64.Decoder BASE64_URL_DECODER = Base64.getUrlDecoder();

	private final ObjectMapper objectMapper;
	private final SecretKeySpec secretKey;
	private final long validitySeconds;

	public JwtTokenProvider(
		ObjectMapper objectMapper,
		@Value("${app.jwt.secret:}") String configuredSecret,
		@Value("${app.jwt.access-token-validity-seconds:3600}") long validitySeconds
	) {
		this.objectMapper = objectMapper;
		this.secretKey = new SecretKeySpec(resolveSecret(configuredSecret), HMAC_ALGORITHM);
		this.validitySeconds = validitySeconds;
	}

	public IssuedToken issue(User user) {
		Instant now = Instant.now();
		Instant expiresAt = now.plusSeconds(validitySeconds);
		String tokenId = UUID.randomUUID().toString();

		ObjectNode header = objectMapper.createObjectNode();
		header.put("alg", "HS256");
		header.put("typ", "JWT");

		ObjectNode payload = objectMapper.createObjectNode();
		payload.put("sub", user.getId().toString());
		payload.put("status", user.getUserStatus().name());
		payload.put("jti", tokenId);
		payload.put("iat", now.getEpochSecond());
		payload.put("exp", expiresAt.getEpochSecond());

		try {
			String encodedHeader = encode(objectMapper.writeValueAsBytes(header));
			String encodedPayload = encode(objectMapper.writeValueAsBytes(payload));
			String content = encodedHeader + "." + encodedPayload;
			String token = content + "." + encode(sign(content));
			return new IssuedToken(token);
		} catch (Exception exception) {
			throw invalidToken();
		}
	}

	public TokenClaims parse(String token) {
		try {
			String[] parts = token.split("\\.");
			if (parts.length != 3) {
				throw invalidToken();
			}

			String content = parts[0] + "." + parts[1];
			if (!MessageDigest.isEqual(sign(content), BASE64_URL_DECODER.decode(parts[2]))) {
				throw invalidToken();
			}

			JsonNode header = objectMapper.readTree(BASE64_URL_DECODER.decode(parts[0]));
			JsonNode payload = objectMapper.readTree(BASE64_URL_DECODER.decode(parts[1]));
			if (!"HS256".equals(header.path("alg").asText())) {
				throw invalidToken();
			}

			long userId = Long.parseLong(payload.path("sub").asText());
			String tokenId = payload.path("jti").asText();
			long expiresAtEpoch = payload.path("exp").asLong();
			if (tokenId.isBlank() || expiresAtEpoch <= Instant.now().getEpochSecond()) {
				throw invalidToken();
			}

			return new TokenClaims(
				userId,
				tokenId,
				LocalDateTime.ofInstant(Instant.ofEpochSecond(expiresAtEpoch), ZoneOffset.UTC)
			);
		} catch (ApplicationException exception) {
			throw exception;
		} catch (Exception exception) {
			throw invalidToken();
		}
	}

	private byte[] sign(String content) throws Exception {
		Mac mac = Mac.getInstance(HMAC_ALGORITHM);
		mac.init(secretKey);

		return mac.doFinal(content.getBytes(StandardCharsets.UTF_8));
	}

	private String encode(byte[] value) {
		return BASE64_URL_ENCODER.encodeToString(value);
	}

	private byte[] resolveSecret(String configuredSecret) {
		if (!configuredSecret.isBlank()) {
			byte[] decoded = Base64.getDecoder().decode(configuredSecret);

			if (decoded.length < 32) {
				throw new IllegalStateException("JWT_SECRET는 Base64 디코딩 기준 32바이트 이상이어야 합니다.");
			}

			return decoded;
		}

		byte[] generated = new byte[32];
		new SecureRandom().nextBytes(generated);
		return generated;
	}

	private ApplicationException invalidToken() {
		return new ApplicationException(ErrorCode.INVALID_TOKEN);
	}
}
