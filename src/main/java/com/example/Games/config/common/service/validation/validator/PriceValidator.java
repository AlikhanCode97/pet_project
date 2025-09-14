package com.example.Games.config.common.service.validation.validator;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class PriceValidator {

    private PriceValidator() {}

    public static BigDecimal validate(BigDecimal price) {
        validateNotNull(price);
        validateNotNegative(price);
        return scale(price);
    }

    public static void validateNotNull(BigDecimal price) {
        if (price == null) {
            throw new IllegalArgumentException("Price cannot be null");
        }
    }

    public static void validateNotNegative(BigDecimal price) {
        if (price.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Price cannot be negative");
        }
    }

    private static BigDecimal scale(BigDecimal price) {
        return price.setScale(2, RoundingMode.HALF_UP);
    }
}
