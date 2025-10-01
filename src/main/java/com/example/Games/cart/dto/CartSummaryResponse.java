package com.example.Games.cart.dto;

import java.math.BigDecimal;
import java.util.List;


public record CartSummaryResponse(
        List<CartItemResponse> items,
        int totalItems,
        BigDecimal totalPrice,
        String formattedTotalPrice
) {
    public static CartSummaryResponse from(List<CartItemResponse> items) {
        BigDecimal totalPrice = items.stream()
                .map(CartItemResponse::gamePrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
                
        return new CartSummaryResponse(
                items,
                items.size(),
                totalPrice,
                String.format("$%.2f", totalPrice)
        );
    }
}
