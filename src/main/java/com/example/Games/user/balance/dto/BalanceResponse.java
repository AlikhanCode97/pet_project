package com.example.Games.user.balance.dto;

import java.math.BigDecimal;

public record BalanceResponse(
        Long userId,
        BigDecimal balance
) {}