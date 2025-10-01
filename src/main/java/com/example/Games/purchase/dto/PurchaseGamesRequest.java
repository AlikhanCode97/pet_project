package com.example.Games.purchase.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record PurchaseGamesRequest(
        @NotEmpty(message = "At least one gameId must be provided")
        List<Long> gameIds
) {}

