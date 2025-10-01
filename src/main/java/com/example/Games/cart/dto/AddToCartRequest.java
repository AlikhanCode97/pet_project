package com.example.Games.cart.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AddToCartRequest(
        @NotNull(message = "Game ID is required")
        @Positive(message = "Game ID must be positive")
        Long gameId
) {
    public static AddToCartRequest of(Long gameId) {
        return new AddToCartRequest(gameId);
    }
}