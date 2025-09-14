package com.example.Games.config.exception.cart;

public class
GameAlreadyOwnedException extends RuntimeException {
    
    public GameAlreadyOwnedException(String message) {
        super(message);
    }
    
    public GameAlreadyOwnedException(String message, Throwable cause) {
        super(message, cause);
    }

    public static GameAlreadyOwnedException forGame(String gameTitle) {
        return new GameAlreadyOwnedException("Game already exists in user library: " + gameTitle);
    }

    public static GameAlreadyOwnedException forGameId(Long gameId) {
        return new GameAlreadyOwnedException("Game already exists in user library with ID: " + gameId);
    }
}
