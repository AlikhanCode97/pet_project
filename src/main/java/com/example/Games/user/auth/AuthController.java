package com.example.Games.user.auth;

import com.example.Games.config.common.mappers.ResponseMapStruct;
import com.example.Games.config.common.dto.ApiResponse;
import com.example.Games.user.auth.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final ResponseMapStruct responseMapper;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<TokenResponse>> register(@RequestBody @Valid RegisterRequest request) {
        TokenResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(responseMapper.toSuccessResponse("Registration successful", response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(@RequestBody @Valid AuthRequest request) {
        TokenResponse response = authService.login(request);
        return ResponseEntity.ok(responseMapper.toSuccessResponse("Login successful", response));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> refreshToken(@RequestBody @Valid RefreshTokenRequest request) {
        TokenResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(
                responseMapper.toSuccessResponse("Token refreshed successfully", response)
        );
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Object>> logout(@RequestHeader(value = "Authorization" , required = false) String authHeader,
                                                     @RequestBody(required = false) LogoutRequest request) {
        authService.logout(authHeader, request);
        return ResponseEntity.ok(
                responseMapper.toSuccessResponse("Successfully logged out")
        );
    }
}
