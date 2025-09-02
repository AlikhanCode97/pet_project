// AddToCartRequest.java
package com.example.Games.cart.dto;

import jakarta.validation.constraints.NotNull;

public record AddToCartRequest(@NotNull Long gameId) {}