package com.example.Games.config.exception.game;

public class UnauthorizedGameAccessException extends RuntimeException {
    
    public UnauthorizedGameAccessException(String message) {
        super(message);
    }

    public static UnauthorizedGameAccessException forModification(Long gameId, String username) {
        return new UnauthorizedGameAccessException(
            String.format("User '%s' is not authorized to modify game with ID: %d", username, gameId)
        );
    }

    public static UnauthorizedGameAccessException forDeletion(Long gameId, String username) {
        return new UnauthorizedGameAccessException(
            String.format("User '%s' is not authorized to delete game with ID: %d", username, gameId)
        );
    }
}
