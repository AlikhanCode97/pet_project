package com.example.Games.config.exception.game;

public class InvalidGameDataException extends RuntimeException {
    public InvalidGameDataException(String message) {
        super(message);
    }
    
    public static InvalidGameDataException invalidTitle() {
        return new InvalidGameDataException("Title cannot be null or empty");
    }
    
    public static InvalidGameDataException invalidPrice() {
        return new InvalidGameDataException("Price amount must be positive");
    }
    
    public static InvalidGameDataException invalidCategory() {
        return new InvalidGameDataException("Category cannot be null");
    }
    
    public static InvalidGameDataException invalidPriceRange() {
        return new InvalidGameDataException("Minimum price cannot be greater than maximum price");
    }
}
