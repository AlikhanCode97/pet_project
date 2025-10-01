package com.example.Games.config.exception.balance;

public class BalanceAlreadyExistsException extends RuntimeException {
    public BalanceAlreadyExistsException(String username) {
        super("Balance already exists for user: " + username);
    }
}
