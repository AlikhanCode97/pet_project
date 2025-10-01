package com.example.Games.cart.dto;

import com.example.Games.game.dto.Response;

import java.math.BigDecimal;

public record CartItemResponse(Response game) {

    public BigDecimal gamePrice() {
        return game != null ? game.price() : BigDecimal.ZERO;
    }
    public String gameTitle() {
        return game != null ? game.title() : "";
    }
    public Long gameId() {
        return game != null ? game.id() : null;
    }
}