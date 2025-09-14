package com.example.Games.user.balance.dto;

import java.math.BigDecimal;

public record BalanceResponse(
        BigDecimal balance,
        Long userId,
        BigDecimal amount,
        String operation
) {

    public static BalanceResponse forBalance(BigDecimal balance) {
        return new BalanceResponse(balance, null, null, null);
    }

    public static BalanceResponse forUserBalance(Long userId, BigDecimal balance) {
        return new BalanceResponse(balance, userId, null, null);
    }

    public static BalanceResponse forOperation(BigDecimal newBalance, BigDecimal operationAmount, String operation) {
        return new BalanceResponse(newBalance, null, operationAmount, operation);
    }
}
