package com.example.Games.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public record TokenResponse(
        @JsonProperty("access_token")
        String accessToken,

        @JsonProperty("refresh_token")
        String refreshToken,

        @JsonProperty("token_type")
        String tokenType,

        @JsonProperty("expires_in")
        long expiresIn,

        @JsonProperty("issued_at")
        Instant issuedAt,

        // User information
        @JsonProperty("user_id")
        Long userId,

        String username,
        String email,
        String role
) {
    public TokenResponse(String accessToken, String refreshToken, long expiresIn,
                         Long userId, String username, String email, String role) {
        this(accessToken, refreshToken, "Bearer", expiresIn, Instant.now(),
                userId, username, email, role);
    }
}