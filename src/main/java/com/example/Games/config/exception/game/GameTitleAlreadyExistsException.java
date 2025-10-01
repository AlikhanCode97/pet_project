package com.example.Games.config.exception.game;

public class GameTitleAlreadyExistsException extends RuntimeException {
    
    public GameTitleAlreadyExistsException(String title) {
        super(String.format("Game with title '%s' already exists", title));
    }
    
    public static GameTitleAlreadyExistsException withTitle(String title) {
        return new GameTitleAlreadyExistsException(title);
    }
}
