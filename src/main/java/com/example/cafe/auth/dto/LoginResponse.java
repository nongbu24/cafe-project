package com.example.cafe.auth.dto;

public record LoginResponse(String tokenType, String accessToken) {
}
