package com.example.Games.game.dto;

import java.math.BigDecimal;

public record UpdateRequest(
        String title,
        BigDecimal price,
        Long categoryId
) {
}
