package com.example.Games.user.auth;

import com.example.Games.config.common.dto.ApiResponse;
import com.example.Games.config.common.mappers.ResponseMapStruct;
import com.example.Games.config.exception.auth.*;
import com.example.Games.config.security.JwtAuthenticationFilter;
import com.example.Games.config.security.SecurityConfig;
import com.example.Games.config.security.TokenBlacklistService;
import com.example.Games.user.auth.dto.*;
import com.example.Games.user.role.RoleType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AuthController.class, excludeFilters = {
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {SecurityConfig.class,
                JwtAuthenticationFilter.class, TokenBlacklistService.class})
})
@Import(AuthExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)

@ActiveProfiles("test")
@DisplayName("AuthController Tests")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private ResponseMapStruct responseMapper;

    private RegisterRequest registerRequest;
    private AuthRequest authRequest;
    private RefreshTokenRequest refreshTokenRequest;
    private LogoutRequest logoutRequest;
    private TokenResponse tokenResponse;
    private ApiResponse<TokenResponse> successTokenResponse;
    private ApiResponse<Object> successLogoutResponse;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest(
                "testuser",
                "test@example.com",
                "Password123!",
                RoleType.USER
        );

        authRequest = new AuthRequest("testuser", "Password123!");

        refreshTokenRequest = new RefreshTokenRequest("valid-refresh-token");

        logoutRequest = new LogoutRequest("valid-refresh-token");

        tokenResponse = new TokenResponse(
                "access-token",
                "refresh-token",
                3600L,
                Instant.now(),
                new TokenResponse.UserInfo(1L, "testuser", "test@example.com", "USER")
        );

        successTokenResponse = new ApiResponse<>("Registration successful", tokenResponse, System.currentTimeMillis());
        successLogoutResponse = new ApiResponse<>("Successfully logged out", null, System.currentTimeMillis());
    }

    // REGISTER TESTS
    @Test
    @DisplayName("Should register user successfully")
    void shouldRegisterUserSuccessfully() throws Exception {
        // Given
        when(authService.register(any(RegisterRequest.class))).thenReturn(tokenResponse);
        when(responseMapper.toSuccessResponse("Registration successful", tokenResponse))
                .thenReturn(successTokenResponse);


        // When & Then
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Registration successful"))
                .andExpect(jsonPath("$.data.access_token").value("access-token"))
                .andExpect(jsonPath("$.data.user.username").value("testuser"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andDo(MockMvcResultHandlers.print());

        verify(authService).register(any(RegisterRequest.class));
        verify(responseMapper).toSuccessResponse(eq("Registration successful"), any(TokenResponse.class));
    }

    @Test
    @DisplayName("Should return 400 when registration data is invalid")
    void shouldReturn400WhenRegistrationDataIsInvalid() throws Exception {
        RegisterRequest invalidRequest = new RegisterRequest("", "", "", null);

        // When & Then
        mockMvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(authService, never()).register(any());
    }

    @Test
    @DisplayName("Should return 400 when username is too short")
    void shouldReturn400WhenUsernameIsTooShort() throws Exception {
        // Given
        RegisterRequest invalidRequest = new RegisterRequest(
                "ab",
                "test@example.com",
                "Password123!",
                RoleType.USER
        );

        // When & Then
        mockMvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(authService, never()).register(any());
    }

    @Test
    @DisplayName("Should return 409 when user already exists")
    void shouldReturn409WhenUserAlreadyExists() throws Exception {
        // Given
        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(UserAlreadyExistsException.username("testuser"));
        when(responseMapper.toErrorResponse(anyString()))
                .thenReturn(new ApiResponse<>("Username already exists: testuser", null, System.currentTimeMillis()));

        // When & Then
        mockMvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Username already exists: testuser"))
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(authService).register(any(RegisterRequest.class));
    }

    @Test
    @DisplayName("Should return 404 when role not found during registration")
    void shouldReturn404WhenRoleNotFoundDuringRegistration() throws Exception {
        // Given
        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new RoleNotFoundException("USER"));
        when(responseMapper.toErrorResponse(anyString()))
                .thenReturn(new ApiResponse<>("Role not found: USER", null, System.currentTimeMillis()));

        // When & Then
        mockMvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Role not found: USER"))
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(authService).register(any(RegisterRequest.class));
    }

    @Test
    @DisplayName("Should login user successfully")
    void shouldLoginUserSuccessfully() throws Exception {
        // Given
        when(authService.login(any(AuthRequest.class))).thenReturn(tokenResponse);
        when(responseMapper.toSuccessResponse(anyString(), any(TokenResponse.class)))
                .thenReturn(new ApiResponse<>("Login successful", tokenResponse, System.currentTimeMillis()));

        // When & Then
        mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Login successful"))
                .andExpect(jsonPath("$.data.access_token").value("access-token"))
                .andExpect(jsonPath("$.data.user.username").value("testuser"));

        verify(authService).login(any(AuthRequest.class));
        verify(responseMapper).toSuccessResponse(eq("Login successful"), any(TokenResponse.class));
    }

    @Test
    @DisplayName("Should return 400 when login data is invalid")
    void shouldReturn400WhenLoginDataIsInvalid() throws Exception {
        AuthRequest invalidRequest = new AuthRequest("", "");

        // When & Then
        mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(authService, never()).login(any());
    }

    @Test
    @DisplayName("Should return 401 when credentials are invalid")
    void shouldReturn401WhenCredentialsAreInvalid() throws Exception {
        // Given
        when(authService.login(any(AuthRequest.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));
        when(responseMapper.toErrorResponse(anyString()))
                .thenReturn(new ApiResponse<>("Invalid username or password", null, System.currentTimeMillis()));

        // When & Then
        mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid username or password"));

        verify(authService).login(any(AuthRequest.class));
    }

    @Test
    @DisplayName("Should return 404 when user not found after authentication")
    void shouldReturn404WhenUserNotFoundAfterAuthentication() throws Exception {
        // Given - Race condition: user deleted after successful authentication
        when(authService.login(any(AuthRequest.class)))
                .thenThrow(new UserNotFoundException("testuser"));
        when(responseMapper.toErrorResponse(anyString()))
                .thenReturn(new ApiResponse<>("User not found: testuser", null, System.currentTimeMillis()));

        // When & Then
        mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found: testuser"))
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(authService).login(any(AuthRequest.class));
    }

    // REFRESH TOKEN TESTS
    @Test
    @DisplayName("Should refresh token successfully")
    void shouldRefreshTokenSuccessfully() throws Exception {
        // Given
        when(authService.refreshToken(any(RefreshTokenRequest.class))).thenReturn(tokenResponse);
        when(responseMapper.toSuccessResponse(anyString(), any(TokenResponse.class)))
                .thenReturn(new ApiResponse<>("Token refreshed successfully", tokenResponse, System.currentTimeMillis()));

        // When & Then
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshTokenRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Token refreshed successfully"))
                .andExpect(jsonPath("$.data.access_token").value("access-token"));

        verify(authService).refreshToken(any(RefreshTokenRequest.class));
        verify(responseMapper).toSuccessResponse(eq("Token refreshed successfully"), any(TokenResponse.class));
    }

    @Test
    @DisplayName("Should return 400 when refresh token is blank")
    void shouldReturn400WhenRefreshTokenIsBlank() throws Exception {
        // Given
        RefreshTokenRequest invalidRequest = new RefreshTokenRequest("");

        // When & Then
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(authService, never()).refreshToken(any());
    }

    @Test
    @DisplayName("Should return 401 when refresh token is invalid")
    void shouldReturn401WhenRefreshTokenIsInvalid() throws Exception {
        // Given
        when(authService.refreshToken(any(RefreshTokenRequest.class)))
                .thenThrow(new InvalidTokenException(
                        InvalidTokenException.TokenErrorType.EXPIRED,
                        "Refresh token is invalid or expired"));
        when(responseMapper.toErrorResponse(anyString()))
                .thenReturn(new ApiResponse<>("Invalid or expired token", null, System.currentTimeMillis()));

        // When & Then
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshTokenRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid or expired token"));

        verify(authService).refreshToken(any(RefreshTokenRequest.class));
    }

    @Test
    @DisplayName("Should return 404 when user not found during refresh")
    void shouldReturn404WhenUserNotFoundDuringRefresh() throws Exception {
        // Given - User deleted after token was issued
        when(authService.refreshToken(any(RefreshTokenRequest.class)))
                .thenThrow(new UserNotFoundException("deleteduser"));
        when(responseMapper.toErrorResponse(anyString()))
                .thenReturn(new ApiResponse<>("User not found: deleteduser", null, System.currentTimeMillis()));

        // When & Then
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshTokenRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found: deleteduser"))
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(authService).refreshToken(any(RefreshTokenRequest.class));
    }

    // LOGOUT TESTS
    @Test
    @DisplayName("Should logout successfully with valid authorization header")
    void shouldLogoutSuccessfullyWithValidAuthorizationHeader() throws Exception {
        // Given
        doNothing().when(authService).logout(eq("Bearer valid-access-token"), any(LogoutRequest.class));
        when(responseMapper.toSuccessResponse(anyString())).thenReturn(successLogoutResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/auth/logout")
                        .with(csrf())
                        .header("Authorization", "Bearer valid-access-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(logoutRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Successfully logged out"))
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(authService).logout(eq("Bearer valid-access-token"), any(LogoutRequest.class));
        verify(responseMapper).toSuccessResponse("Successfully logged out");
    }

    @Test
    @DisplayName("Should logout successfully without request body")
    void shouldLogoutSuccessfullyWithoutRequestBody() throws Exception {
        // Given
        doNothing().when(authService).logout(eq("Bearer valid-access-token"), isNull());
        when(responseMapper.toSuccessResponse(anyString())).thenReturn(successLogoutResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/auth/logout")
                        .with(csrf())
                        .header("Authorization", "Bearer valid-access-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Successfully logged out"));

        verify(authService).logout(eq("Bearer valid-access-token"), isNull());
    }

    @Test
    @DisplayName("Should return 401 when logout with invalid token")
    void shouldReturn401WhenLogoutWithInvalidToken() throws Exception {
        // Given
        doThrow(new InvalidTokenException(
                        InvalidTokenException.TokenErrorType.MALFORMED,
                        "Invalid access token"))
                .when(authService).logout(anyString(), any());
        when(responseMapper.toErrorResponse(anyString()))
                .thenReturn(new ApiResponse<>("Invalid or expired token", null, System.currentTimeMillis()));

        // When & Then
        mockMvc.perform(post("/api/v1/auth/logout")
                        .with(csrf())
                        .header("Authorization", "Bearer invalid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(logoutRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid or expired token"));

        verify(authService).logout(eq("Bearer invalid-token"), any(LogoutRequest.class));
    }

    @Test
    @DisplayName("Should return 401 when logout called without authorization header")
    void shouldReturn401WhenLogoutWithoutAuthorizationHeader() throws Exception {
        // Given - When no Authorization header is provided, authHeader will be null
        // The service should throw InvalidTokenException for missing tokens
        doThrow(new InvalidTokenException(
                InvalidTokenException.TokenErrorType.MISSING,
                "No valid tokens provided for logout"))
                .when(authService).logout(isNull(), isNull());

        when(responseMapper.toErrorResponse(anyString()))
                .thenReturn(new ApiResponse<>("Invalid or expired token", null, System.currentTimeMillis()));

        // When & Then
        mockMvc.perform(post("/api/v1/auth/logout")
                        .with(csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid or expired token"));

        verify(authService).logout(isNull(), isNull());
        verify(responseMapper).toErrorResponse(anyString());
    }


}
