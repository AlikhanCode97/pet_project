package com.example.Games.config.exception.balance;

public class InvalidAmountException extends RuntimeException {
    public InvalidAmountException(String operation) {
        super(operation + " amount must be positive");
    }
}