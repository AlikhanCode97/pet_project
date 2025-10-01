package com.example.Games.config.exception.gameHistory;

import com.example.Games.config.common.dto.ApiResponse;
import com.example.Games.config.common.mappers.ResponseMapStruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GameHistoryExceptionHandler {

    private final ResponseMapStruct responseMapper;

    @ExceptionHandler(GameHistoryException.class)
    public ResponseEntity<ApiResponse<Object>> handleGameHistoryException(GameHistoryException ex) {
        log.warn("Game history error: {}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body(responseMapper.toErrorResponse(ex.getMessage()));
    }
}