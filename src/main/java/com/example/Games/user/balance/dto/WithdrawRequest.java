package com.example.Games.user.balance.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record WithdrawRequest(
        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Minimum withdrawal is $0.01")
        @DecimalMax(value = "999999.99", message = "Maximum withdrawal is $999,999.99") 
        @Digits(integer = 6, fraction = 2, message = "Amount must have maximum 6 digits before decimal and 2 decimal places")
        BigDecimal amount
) {}
