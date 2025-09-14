package com.example.Games.user.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public record TokenResponse(
        @JsonProperty("access_token")
        String accessToken,

        @JsonProperty("refresh_token")
        String refreshToken,

        @JsonProperty("expires_in")
        long expiresIn,

        @JsonProperty("issued_at")
        Instant issuedAt,

        @JsonProperty("user")
        UserInfo user
) {
    public record UserInfo(
        Long id,
        String username,
        String email,
        String role
    ) {}
}
