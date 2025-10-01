package com.example.Games.config.exception.game;

public class GameNotFoundException extends RuntimeException {
    
    public GameNotFoundException(String message) {
        super(message);
    }

    public static GameNotFoundException byId(Long gameId) {
        return new GameNotFoundException("Game not found with ID: " + gameId);
    }

    public static GameNotFoundException byTitle(String title) {
        return new GameNotFoundException("Game not found with title: " + title);
    }
}
