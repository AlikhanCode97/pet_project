package com.example.Games.config.exception.balance;

public class BalanceNotFoundException extends RuntimeException {
    public BalanceNotFoundException(String username) {
        super("Balance not found for user: " + username);
    }
}
