package com.example.Games.config.exception;

import com.example.Games.config.common.mappers.ResponseMapStruct;
import com.example.Games.config.common.dto.ApiResponse;
import com.example.Games.config.exception.cart.CartOperationException;
import com.example.Games.config.exception.cart.GameAlreadyInCartException;
import com.example.Games.config.exception.cart.GameAlreadyOwnedException;
import com.example.Games.config.exception.category.CategoryAlreadyExistsException;
import com.example.Games.config.exception.category.CategoryInUseException;
import com.example.Games.config.exception.category.CategoryNotFoundException;
import com.example.Games.config.exception.game.GameNotFoundException;
import com.example.Games.config.exception.game.UnauthorizedGameAccessException;
import com.example.Games.config.exception.purchase.InsufficientFundsException;
import com.example.Games.config.exception.purchase.PurchaseException;
import com.example.Games.config.exception.user.LogoutException;
import com.example.Games.config.exception.user.UserAlreadyExistsException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final ResponseMapStruct responseMapper;

    // ============= VALIDATION ERRORS =============
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        log.warn("Validation error: {}", errors);
        return ResponseEntity.badRequest()
                .body(responseMapper.toErrorResponse("Validation failed", errors));
    }

    // ============= RESOURCE ERRORS =============
    
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleResourceNotFound(ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(responseMapper.toErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(GameNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleGameNotFound(GameNotFoundException ex) {
        log.warn("Game not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(responseMapper.toErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(CategoryNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleCategoryNotFound(CategoryNotFoundException ex) {
        log.warn("Category not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(responseMapper.toErrorResponse(ex.getMessage()));
    }

    // ============= AUTHENTICATION & AUTHORIZATION ERRORS =============
    
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Object>> handleAuthenticationException(AuthenticationException ex) {
        log.warn("Authentication failed: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(responseMapper.toErrorResponse("Authentication failed"));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Object>> handleBadCredentials(BadCredentialsException ex) {
        log.warn("Bad credentials: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(responseMapper.toErrorResponse("Invalid credentials"));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Object>> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(responseMapper.toErrorResponse("Access denied"));
    }

    @ExceptionHandler(UnauthorizedGameAccessException.class)
    public ResponseEntity<ApiResponse<Object>> handleUnauthorizedGameAccess(UnauthorizedGameAccessException ex) {
        log.warn("Unauthorized game access: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(responseMapper.toErrorResponse(ex.getMessage()));
    }

    // ============= BUSINESS LOGIC ERRORS =============
    
    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Object>> handleUserAlreadyExists(UserAlreadyExistsException ex) {
        log.warn("User already exists: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(responseMapper.toErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(GameAlreadyOwnedException.class)
    public ResponseEntity<ApiResponse<Object>> handleGameAlreadyOwned(GameAlreadyOwnedException ex) {
        log.warn("Game already owned: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(responseMapper.toErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(GameAlreadyInCartException.class)
    public ResponseEntity<ApiResponse<Object>> handleGameAlreadyInCart(GameAlreadyInCartException ex) {
        log.warn("Game already in cart: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
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

    // ============= FINANCIAL ERRORS =============
    
    @ExceptionHandler(InsufficientFundsException.class)
    public ResponseEntity<ApiResponse<Object>> handleInsufficientFunds(InsufficientFundsException ex) {
        log.warn("Insufficient funds: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
                .body(responseMapper.toErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(PurchaseException.class)
    public ResponseEntity<ApiResponse<Object>> handlePurchaseException(PurchaseException ex) {
        log.warn("Purchase failed: {}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body(responseMapper.toErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(CartOperationException.class)
    public ResponseEntity<ApiResponse<Object>> handleCartOperation(CartOperationException ex) {
        log.warn("Cart operation failed: {}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body(responseMapper.toErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(LogoutException.class)
    public ResponseEntity<ApiResponse<Object>> handleLogoutException(LogoutException ex) {
        log.warn("Logout failed: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(responseMapper.toErrorResponse(ex.getMessage()));
    }

    // ============= GENERAL ERRORS =============
    
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Object>> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Illegal argument: {}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body(responseMapper.toErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Object>> handleIllegalState(IllegalStateException ex) {
        log.warn("Illegal state: {}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body(responseMapper.toErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Object>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        log.warn("Type mismatch for parameter '{}': {}", ex.getName(), ex.getMessage());
        return ResponseEntity.badRequest()
                .body(responseMapper.toErrorResponse("Invalid parameter type for: " + ex.getName()));
    }

    // ============= CATCH-ALL ERRORS =============
    
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Object>> handleRuntimeException(RuntimeException ex, WebRequest request) {
        log.error("Runtime exception at {}: {}", request.getDescription(false), ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(responseMapper.toErrorResponse("An error occurred while processing your request"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGenericException(Exception ex, WebRequest request) {
        log.error("Unexpected error at {}: {}", request.getDescription(false), ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(responseMapper.toErrorResponse("An unexpected error occurred"));
    }
}
