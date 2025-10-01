package com.example.Games.config.exception.cart;

import com.example.Games.config.common.dto.ApiResponse;
import com.example.Games.config.common.mappers.ResponseMapStruct;
import com.example.Games.config.exception.purchase.GameAlreadyOwnedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class CartExceptionHandler {

    private final ResponseMapStruct responseMapper;

    @ExceptionHandler(GameAlreadyInCartException.class)
    public ResponseEntity<ApiResponse<Object>> handleGameAlreadyInCart(GameAlreadyInCartException ex) {
        log.warn("Game already in cart: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(responseMapper.toErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(GameAlreadyOwnedException.class)
    public ResponseEntity<ApiResponse<Object>> handleGameAlreadyOwned(GameAlreadyOwnedException ex) {
        log.warn("Game already owned: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(responseMapper.toErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(CartOperationException.class)
    public ResponseEntity<ApiResponse<Object>> handleCartOperation(CartOperationException ex) {
        log.warn("Cart operation failed: {}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body(responseMapper.toErrorResponse(ex.getMessage()));
    }
}