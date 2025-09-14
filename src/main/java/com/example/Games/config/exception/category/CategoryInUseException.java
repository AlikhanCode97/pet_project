package com.example.Games.config.exception.category;

public class CategoryInUseException extends RuntimeException {
    
    public CategoryInUseException(String message) {
        super(message);
    }

    public static CategoryInUseException withGames(String categoryName, int gameCount) {
        return new CategoryInUseException(
            String.format("Cannot delete category '%s' as it has %d associated games",
                categoryName, gameCount)
        );
    }

    public static CategoryInUseException withGamesById(Long categoryId, int gameCount) {
        return new CategoryInUseException(
            String.format("Cannot delete category with ID %d as it has %d associated games", 
                categoryId, gameCount)
        );
    }
}
