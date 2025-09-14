package com.example.Games.game.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateRequest(
        @NotBlank(message = "Title is required")
        @Size(min = 1, max = 100, message = "Title must be between 1 and 100 characters")
        String title,

        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.01", message = "Price must be at least $0.01")
        BigDecimal price,

        @NotNull(message = "Category ID is required")
        Long categoryId
) {}
