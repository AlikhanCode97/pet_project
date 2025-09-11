package com.example.Games.user.auth;

import com.example.Games.config.common.ResponseMapStruct;
import com.example.Games.config.common.dto.ApiResponse;
import com.example.Games.user.auth.dto.*;
import com.example.Games.config.exception.UserAlreadyExistsException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final ResponseMapStruct responseMapper;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody @Valid RegisterRequest request,
                                    BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest()
                    .body(responseMapper.toErrorResponse(bindingResult));
        }
        
        try {
            TokenResponse response = authService.register(request);
            return ResponseEntity.ok(
                    responseMapper.toSuccessResponse("Registration successful", response)
            );
        } catch (UserAlreadyExistsException e) {
            log.warn("Registration failed - user exists: {}", e.getMessage());
            return ResponseEntity.status(409)
                    .body(responseMapper.toErrorResponse(e.getMessage()));
        } catch (IllegalArgumentException e) {
            log.warn("Registration failed - invalid input: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(responseMapper.toErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error during registration: {}", e.getMessage());
            return ResponseEntity.status(500)
                    .body(responseMapper.toErrorResponse("Registration failed. Please try again."));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody @Valid AuthRequest request,
                                 BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest()
                    .body(responseMapper.toErrorResponse(bindingResult));
        }
        
        try {
            TokenResponse response = authService.login(request);
            return ResponseEntity.ok(
                    responseMapper.toSuccessResponse("Login successful", response)
            );
        } catch (BadCredentialsException e) {
            log.warn("Login failed for user: {}", request.username());
            return ResponseEntity.status(401)
                    .body(responseMapper.toErrorResponse("Invalid username or password"));
        } catch (Exception e) {
            log.error("Unexpected error during login: {}", e.getMessage());
            return ResponseEntity.status(500)
                    .body(responseMapper.toErrorResponse("Login failed. Please try again."));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody @Valid RefreshTokenRequest request,
                                        BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest()
                    .body(responseMapper.toErrorResponse(bindingResult));
        }
        
        try {
            TokenResponse response = authService.refreshToken(request);
            return ResponseEntity.ok(responseMapper.toSuccessResponse(response));
        } catch (BadCredentialsException e) {
            log.warn("Token refresh failed: {}", e.getMessage());
            return ResponseEntity.status(401)
                    .body(responseMapper.toErrorResponse("Invalid or expired refresh token"));
        } catch (Exception e) {
            log.error("Unexpected error during token refresh: {}", e.getMessage());
            return ResponseEntity.status(500)
                    .body(responseMapper.toErrorResponse("Token refresh failed. Please try again."));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Object>> logout(@RequestBody(required = false) LogoutRequest request,
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
            return ResponseEntity.ok(
                    responseMapper.toSuccessResponse("Successfully logged out", null)
            );
            
        } catch (Exception e) {
            log.error("Error during logout: {}", e.getMessage());
            // Always return success for logout - don't leak error information
            return ResponseEntity.ok(
                    responseMapper.toSuccessResponse("Successfully logged out", null)
            );
        }
    }
}
