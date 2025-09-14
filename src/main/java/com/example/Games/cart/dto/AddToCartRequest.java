package com.example.Games.cart.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Request DTO for adding games to cart
 * Contains validation rules for cart operations
 */
public record AddToCartRequest(
        @NotNull(message = "Game ID is required")
        @Positive(message = "Game ID must be positive")
        Long gameId
) {
    /**
     * Factory method for creating add to cart requests
     * @param gameId the ID of the game to add
     * @return validated request object
     */
    public static AddToCartRequest of(Long gameId) {
        return new AddToCartRequest(gameId);
    }
}