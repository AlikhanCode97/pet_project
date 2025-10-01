package com.example.Games.config.exception.game;

public class UnauthorizedGameAccessException extends RuntimeException {
    
    public UnauthorizedGameAccessException(String message) {
        super(message);
    }

    public static UnauthorizedGameAccessException notOwner(Long gameId, String username) {
        return new UnauthorizedGameAccessException(
            String.format("User '%s' is not the owner of game with ID: %d", username, gameId)
        );
    }
}
