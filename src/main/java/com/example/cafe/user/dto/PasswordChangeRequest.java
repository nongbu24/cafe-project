package com.example.cafe.user.dto;

public record PasswordChangeRequest(String currentPassword, String newPassword) {
}
