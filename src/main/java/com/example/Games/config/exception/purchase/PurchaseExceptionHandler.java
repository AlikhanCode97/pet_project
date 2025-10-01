package com.example.Games.config.exception.purchase;

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
@Order(1)
@RestControllerAdvice
@RequiredArgsConstructor
public class PurchaseExceptionHandler {

    private final ResponseMapStruct responseMapper;


    @ExceptionHandler(GameAlreadyOwnedException.class)
    public ResponseEntity<ApiResponse<Object>> handleGameAlreadyOwned(GameAlreadyOwnedException ex) {
        log.warn("Game already owned: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(responseMapper.toErrorResponse(ex.getMessage()));
    }


    @ExceptionHandler(PurchaseException.class)
    public ResponseEntity<ApiResponse<Object>> handlePurchaseException(PurchaseException ex) {
        log.warn("Purchase exception: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(responseMapper.toErrorResponse(ex.getMessage()));
    }
}
