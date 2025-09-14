package com.example.Games.config.exception.category;

import com.example.Games.config.exception.ResourceNotFoundException;

public class CategoryNotFoundException extends ResourceNotFoundException {
    
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
