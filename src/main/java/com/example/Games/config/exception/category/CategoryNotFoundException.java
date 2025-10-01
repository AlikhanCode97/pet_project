package com.example.Games.config.exception.category;

public class CategoryNotFoundException extends RuntimeException {
    
    public CategoryNotFoundException(String message) {
        super(message);
    }
    

    public static CategoryNotFoundException byId(Long categoryId) {
        return new CategoryNotFoundException("Category not found with ID: " + categoryId);
    }

    public static CategoryNotFoundException byName(String categoryName) {
        return new CategoryNotFoundException("Category not found with name: " + categoryName);
    }
}
