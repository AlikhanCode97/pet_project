package com.example.Games.config.exception.game;

import com.example.Games.config.exception.ResourceNotFoundException;

public class GameNotFoundException extends ResourceNotFoundException {
    
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
