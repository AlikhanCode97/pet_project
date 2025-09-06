package com.example.Games.user.dto;

import jakarta.validation.constraints.NotBlank;

public record LogoutRequest(
        @NotBlank(message = "Access token is required")
        String accessToken,
        
        String refreshToken // Optional - can be null
) {
    public LogoutRequest(String accessToken, String refreshToken) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }
}
