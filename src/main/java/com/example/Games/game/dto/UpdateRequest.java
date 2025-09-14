package com.example.Games.game.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record UpdateRequest(
        @Size(min = 1, max = 100, message = "Title must be between 1 and 100 characters")
        String title,
        
        @DecimalMin(value = "0.01", message = "Price must be at least $0.01")
        BigDecimal price,
        
        Long categoryId
) {}
