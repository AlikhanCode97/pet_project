package com.example.Games.config.exception.game;

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
public class GameExceptionHandler {

    private final ResponseMapStruct responseMapper;

    @ExceptionHandler(GameNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleGameNotFound(GameNotFoundException ex) {
        log.warn("Game not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(responseMapper.toErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(GameTitleAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Object>> handleGameTitleAlreadyExists(GameTitleAlreadyExistsException ex) {
        log.warn("Game title already exists: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(responseMapper.toErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(InvalidGameDataException.class)
    public ResponseEntity<ApiResponse<Object>> handleInvalidGameData(InvalidGameDataException ex) {
        log.warn("Invalid game data: {}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body(responseMapper.toErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(UnauthorizedGameAccessException.class)
    public ResponseEntity<ApiResponse<Object>> handleUnauthorizedGameAccess(UnauthorizedGameAccessException ex) {
        log.warn("Unauthorized game access: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(responseMapper.toErrorResponse(ex.getMessage()));
    }
}
