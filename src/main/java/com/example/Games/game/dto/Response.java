package com.example.Games.game.dto;

import com.example.Games.category.dto.CategoryResponse;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record Response(
        Long id,
        String title,
        String author,
        BigDecimal price,
        CategoryResponse category,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
