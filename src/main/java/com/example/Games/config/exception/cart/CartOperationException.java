package com.example.Games.config.exception.cart;

public class CartOperationException extends RuntimeException {
    
    public CartOperationException(String message) {
        super(message);
    }

    public static CartOperationException emptyCart() {
        return new CartOperationException("Cannot perform operation on empty cart");
    }

    public static CartOperationException gameNotInCart(Long gameId) {
        return new CartOperationException(
            String.format("Game with ID %d is not in your cart", gameId)
        );
    }
}
