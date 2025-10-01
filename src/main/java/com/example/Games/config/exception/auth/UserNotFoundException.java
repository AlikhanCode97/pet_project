package com.example.Games.config.exception.auth;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(String username) {
        super("User not found: " + username);
    }
    
    public UserNotFoundException(Long userId) {
        super("User not found with ID: " + userId);
    }
}
