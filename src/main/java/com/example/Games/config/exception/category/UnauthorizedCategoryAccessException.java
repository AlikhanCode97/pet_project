package com.example.Games.config.exception.category;

public class UnauthorizedCategoryAccessException extends RuntimeException {
    
    public UnauthorizedCategoryAccessException(String message) {
        super(message);
    }

    public static UnauthorizedCategoryAccessException notOwner(Long categoryId, String username) {
        return new UnauthorizedCategoryAccessException(
            String.format("User '%s' is not the creator of category with ID: %d and cannot modify it", 
                    username, categoryId)
        );
    }

    public static UnauthorizedCategoryAccessException cannotDelete(String categoryName, String username) {
        return new UnauthorizedCategoryAccessException(
            String.format("User '%s' is not authorized to delete category '%s'", 
                    username, categoryName)
        );
    }
}
