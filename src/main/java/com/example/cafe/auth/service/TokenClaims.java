package com.example.cafe.auth.service;

import java.time.LocalDateTime;

public record TokenClaims(long userId, String tokenId, LocalDateTime expiresAt) {
}
