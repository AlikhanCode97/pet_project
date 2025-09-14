package com.example.Games.purchase.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record PurchaseStatsResponse(
        int totalGamesOwned,
        BigDecimal totalMoneySpent,
        BigDecimal averagePrice,
        LocalDateTime firstPurchaseDate,
        LocalDateTime lastPurchaseDate
) {}
