package com.example.Games.game.dto;

public record FilterRequest(
        String title,       // for search
        String categoryName,
        String sortBy,      // "price", "title", etc.
        String direction    // "asc", "desc"
) {}
