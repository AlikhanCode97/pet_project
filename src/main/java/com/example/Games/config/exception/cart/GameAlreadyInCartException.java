package com.example.Games.config.exception.cart;

public class GameAlreadyInCartException extends RuntimeException {
    
    public GameAlreadyInCartException(String message) {
        super(message);
    }

    public static GameAlreadyInCartException forGame(Long gameId, String gameTitle) {
        return new GameAlreadyInCartException(
            String.format("Game '%s' (ID: %d) is already in your cart", gameTitle, gameId)
        );
    }

    public static GameAlreadyInCartException forGameId(Long gameId) {
        return new GameAlreadyInCartException(
            String.format("Game with ID %d is already in your cart", gameId)
        );
    }
}
