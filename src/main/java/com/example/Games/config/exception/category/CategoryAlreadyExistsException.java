package com.example.Games.config.exception.category;

public class CategoryAlreadyExistsException extends RuntimeException {
    
    public CategoryAlreadyExistsException(String message) {
        super(message);
    }

    public static CategoryAlreadyExistsException withName(String categoryName) {
        return new CategoryAlreadyExistsException(
            String.format("Category already exists with name: '%s'", categoryName)
        );
    }
}
