package com.example.Games.purchase.dto;

import com.example.Games.purchase.PurchaseHistory;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record PurchaseResponse(
        Long purchaseId,
        Long gameId,
        String gameTitle,
        String gameAuthor,
        BigDecimal purchasePrice,
        BigDecimal currentGamePrice,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime purchasedAt,
        BigDecimal priceDifference
) {}
