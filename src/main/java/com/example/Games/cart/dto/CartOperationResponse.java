package com.example.Games.cart.dto;

import java.math.BigDecimal;

public record CartOperationResponse(
        String operation,
        String message,
        Long gameId,
        String gameTitle,
        int cartSize,
        int itemsProcessed,
        BigDecimal totalAmount,
        String formattedTotal
) {

    // Constructor for simple operations (add/remove)
    public CartOperationResponse(String operation, String message, Long gameId, String gameTitle, int cartSize) {
        this(operation, message, gameId, gameTitle, cartSize, 0, null, null);
    }

    public static CartOperationResponse addedToCart(Long gameId, String gameTitle, int cartSize) {
        return new CartOperationResponse(
                "ADD",
                "Game added to cart successfully",
                gameId,
                gameTitle,
                cartSize
        );
    }

    public static CartOperationResponse removedFromCart(Long gameId, String gameTitle, int cartSize) {
        return new CartOperationResponse(
                "REMOVE",
                "Game removed from cart successfully",
                gameId,
                gameTitle,
                cartSize
        );
    }

    public static CartOperationResponse checkedOut(int itemsProcessed, BigDecimal totalAmount) {
        String formattedTotal = String.format("$%.2f", totalAmount);
        return new CartOperationResponse(
                "CHECKOUT",
                String.format("Checkout completed successfully. %d games purchased for %s and added to your library.",
                        itemsProcessed, formattedTotal),
                null,
                null,
                0, // Cart is empty after checkout
                itemsProcessed,
                totalAmount,
                formattedTotal
        );
    }

    public static CartOperationResponse cartCleared(int itemsRemoved) {
        return new CartOperationResponse(
                "CLEAR",
                String.format("Cart cleared successfully. %d items removed.", itemsRemoved),
                null,
                null,
                0, // Cart is empty after clearing
                itemsRemoved,
                null,
                null
        );
    }
}