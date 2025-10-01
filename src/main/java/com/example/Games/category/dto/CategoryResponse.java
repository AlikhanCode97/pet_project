package com.example.Games.category.dto;

import java.time.LocalDateTime;

public record CategoryResponse(
        Long id,
        String name,
        String createdByUsername,
        Long createdById,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
