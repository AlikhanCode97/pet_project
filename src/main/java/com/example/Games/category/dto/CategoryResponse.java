package com.example.Games.category.dto;

import java.time.LocalDateTime;

public record CategoryResponse(
        Long id,
        String name,
        int gameCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
