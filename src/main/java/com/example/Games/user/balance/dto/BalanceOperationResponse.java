package com.example.Games.user.balance.dto;

import com.example.Games.user.balance.transaction.OperationType;

import java.math.BigDecimal;

public record BalanceOperationResponse(
        BigDecimal balance,
        Long userId,
        BigDecimal amount,
        OperationType operation
) {}