package com.example.Games.purchase.dto;

import java.math.BigDecimal;

/**
 * Response DTO for purchase eligibility checks
 */
public record PurchaseEligibilityResponse(
        boolean eligible,
        String message,
        Long gameId,
        String gameTitle,
        BigDecimal gamePrice,
        String formattedGamePrice,
        BigDecimal userBalance,
        String formattedUserBalance,
        boolean sufficientFunds
) {}
