package com.example.Games.cart.dto;

import com.example.Games.game.dto.Response;

import java.math.BigDecimal;

/**
 * Response DTO for individual cart items
 * Wraps game response with cart-specific functionality
 */
public record CartItemResponse(Response game) {
    
    /**
     * Helper method to get the game price for calculations
     * @return the price of the game in this cart item
     */
    public BigDecimal gamePrice() {
        return game != null ? game.price() : BigDecimal.ZERO;
    }
    
    /**
     * Helper method to get the game title for display
     * @return the title of the game in this cart item
     */
    public String gameTitle() {
        return game != null ? game.title() : "";
    }
    
    /**
     * Helper method to get the game ID
     * @return the ID of the game in this cart item
     */
    public Long gameId() {
        return game != null ? game.id() : null;
    }
}