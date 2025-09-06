package com.example.Games.user;

import com.example.Games.user.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody @Valid RegisterRequest request, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(createValidationErrorResponse(bindingResult));
        }
        
        try {
            TokenResponse response = authService.register(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Registration failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error during registration: {}", e.getMessage());
            return ResponseEntity.status(500).body(createErrorResponse("Registration failed. Please try again."));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody @Valid AuthRequest request, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(createValidationErrorResponse(bindingResult));
        }
        
        try {
            TokenResponse response = authService.login(request);
            return ResponseEntity.ok(response);
        } catch (BadCredentialsException e) {
            log.warn("Login failed for user: {}", request.username());
            return ResponseEntity.status(401).body(createErrorResponse("Invalid username or password"));
        } catch (Exception e) {
            log.error("Unexpected error during login: {}", e.getMessage());
            return ResponseEntity.status(500).body(createErrorResponse("Login failed. Please try again."));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody @Valid RefreshTokenRequest request, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(createValidationErrorResponse(bindingResult));
        }
        
        try {
            TokenResponse response = authService.refreshToken(request);
            return ResponseEntity.ok(response);
        } catch (BadCredentialsException e) {
            log.warn("Token refresh failed: {}", e.getMessage());
            return ResponseEntity.status(401).body(createErrorResponse("Invalid or expired refresh token"));
        } catch (Exception e) {
            log.error("Unexpected error during token refresh: {}", e.getMessage());
            return ResponseEntity.status(500).body(createErrorResponse("Token refresh failed. Please try again."));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody(required = false) LogoutRequest request, 
                                   @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            String accessToken = null;
            String refreshToken = null;
            
            // Extract access token from Authorization header
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                accessToken = authHeader.substring(7);
            }
            
            // Get refresh token from request body if provided
            if (request != null && request.refreshToken() != null) {
                refreshToken = request.refreshToken();
            }
            
            // If we have access token from request body, use that instead
            if (request != null && request.accessToken() != null) {
                accessToken = request.accessToken();
            }
            
            authService.logout(accessToken, refreshToken);
            
            log.info("User successfully logged out");
            return ResponseEntity.ok(createSuccessResponse("Successfully logged out"));
            
        } catch (Exception e) {
            log.error("Error during logout: {}", e.getMessage());
            // Always return success for logout - don't leak error information
            return ResponseEntity.ok(createSuccessResponse("Successfully logged out"));
        }
    }
    
    private Map<String, Object> createValidationErrorResponse(BindingResult bindingResult) {
        Map<String, Object> response = new HashMap<>();
        Map<String, String> errors = new HashMap<>();
        
        for (FieldError error : bindingResult.getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }
        
        response.put("success", false);
        response.put("message", "Validation failed");
        response.put("errors", errors);
        return response;
    }
    
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        return response;
    }
    
    private Map<String, Object> createSuccessResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", message);
        return response;
    }
}