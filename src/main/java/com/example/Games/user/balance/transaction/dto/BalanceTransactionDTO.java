package com.example.Games.user.balance.transaction.dto;

import com.example.Games.user.balance.transaction.OperationType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BalanceTransactionDTO(
        Long id,
        OperationType type,
        BigDecimal amount,
        BigDecimal balanceBefore,
        BigDecimal balanceAfter,
        LocalDateTime timestamp
) {}
