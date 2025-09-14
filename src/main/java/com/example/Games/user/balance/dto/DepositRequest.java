package com.example.Games.user.balance.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record DepositRequest(
        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Minimum deposit is $0.01")
        @DecimalMax(value = "999999.99", message = "Maximum deposit is $999,999.99")
        @Digits(integer = 6, fraction = 2, message = "Amount must have maximum 6 digits before decimal and 2 decimal places")
        BigDecimal amount
) {}
