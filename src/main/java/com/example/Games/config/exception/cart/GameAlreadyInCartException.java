package com.example.Games.config.exception.cart;

public class GameAlreadyInCartException extends RuntimeException {
    
    public GameAlreadyInCartException(String message) {
        super(message);
    }
}
