package com.example.Games.config.exception.category;

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
public class CategoryExceptionHandler {

    private final ResponseMapStruct responseMapper;

    @ExceptionHandler(CategoryNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleCategoryNotFound(CategoryNotFoundException ex) {
        log.warn("Category not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(responseMapper.toErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(CategoryAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Object>> handleCategoryAlreadyExists(CategoryAlreadyExistsException ex) {
        log.warn("Category already exists: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(responseMapper.toErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(CategoryInUseException.class)
    public ResponseEntity<ApiResponse<Object>> handleCategoryInUse(CategoryInUseException ex) {
        log.warn("Category in use: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(responseMapper.toErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(UnauthorizedCategoryAccessException.class)
    public ResponseEntity<ApiResponse<Object>> handleUnauthorizedCategoryAccess(UnauthorizedCategoryAccessException ex) {
        log.warn("Unauthorized category access: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(responseMapper.toErrorResponse(ex.getMessage()));
    }
}
