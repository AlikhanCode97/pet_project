package com.example.Games.config.exception.balance;

import com.example.Games.config.common.dto.ApiResponse;
import com.example.Games.config.common.mappers.ResponseMapStruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class BalanceExceptionHandler {

    private final ResponseMapStruct responseMapper;

    @ExceptionHandler(InvalidAmountException.class)
    public ResponseEntity<ApiResponse<Object>> handleInvalidAmount(InvalidAmountException ex) {
        log.warn("Invalid amount: {}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body(responseMapper.toErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(BalanceNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleBalanceNotFound(BalanceNotFoundException ex) {
        log.warn("Balance not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(responseMapper.toErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(BalanceAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Object>> handleBalanceAlreadyExists(BalanceAlreadyExistsException ex) {
        log.warn("Balance already exists: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(responseMapper.toErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(InsufficientFundsException.class)
    public ResponseEntity<ApiResponse<Object>> handleInsufficientFunds(InsufficientFundsException ex) {
        log.warn("Insufficient funds: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
                .body(responseMapper.toErrorResponse(ex.getMessage()));
    }
}
