package com.example.Games.game.dto;

import com.example.Games.category.CategoryResponse;

import java.math.BigDecimal;

public record Response(
        Long id,
        String title,
        String author,
        BigDecimal price,
        CategoryResponse category) {}
