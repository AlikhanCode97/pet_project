package com.example.Games.user.auth;

import com.example.Games.config.exception.auth.InvalidTokenException;
import com.example.Games.config.exception.auth.RoleNotFoundException;
import com.example.Games.config.exception.auth.UserAlreadyExistsException;
import com.example.Games.config.exception.auth.UserNotFoundException;
import com.example.Games.config.security.CustomUserDetails;
import com.example.Games.config.security.JwtService;
import com.example.Games.config.security.TokenBlacklistService;
import com.example.Games.user.auth.dto.*;
import com.example.Games.user.role.Role;
import com.example.Games.user.role.RoleRepository;
import com.example.Games.user.role.RoleType;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Tests")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private Role userRole;
    private RegisterRequest registerRequest;
    private AuthRequest authRequest;
    private CustomUserDetails customUserDetails;
    private TokenResponse tokenResponse;

    @BeforeEach
    void setUp() {
        userRole = Role.builder()
                .id(1L)
                .name(RoleType.USER)
                .build();

        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .password("hashedPassword")
                .role(userRole)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        registerRequest = new RegisterRequest(
                "testuser",
                "test@example.com",
                "Password123!",
                RoleType.USER
        );

        authRequest = new AuthRequest("testuser", "Password123!");

        customUserDetails = new CustomUserDetails(testUser);

        tokenResponse = new TokenResponse(
                "access-token",
                "refresh-token",
                3600L,
                Instant.now(),
                new TokenResponse.UserInfo(1L, "testuser", "test@example.com", "USER")
        );
    }

    // REGISTER TESTS
    @Test
    @DisplayName("Should register user successfully")
    void shouldRegisterUserSuccessfully() {
        // Given
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(roleRepository.findByName(RoleType.USER)).thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode("Password123!")).thenReturn("hashedPassword");
        when(userRepository.save(Mockito.any(User.class))).thenReturn(testUser);
        when(userMapper.toTokenResponse(anyString(), anyString(), anyLong(), any(CustomUserDetails.class)))
                .thenReturn(tokenResponse);
        when(jwtService.generateToken(any(CustomUserDetails.class))).thenReturn("access-token");
        when(jwtService.generateRefreshToken(any(CustomUserDetails.class))).thenReturn("refresh-token");
        when(jwtService.getExpirationTime()).thenReturn(3600000L);

        // When
        TokenResponse result = authService.register(registerRequest);

        // Then
        assertThat(result).isEqualTo(tokenResponse);
        verify(userRepository).existsByUsername("testuser");
        verify(userRepository).existsByEmail("test@example.com");
        verify(roleRepository).findByName(RoleType.USER);
        verify(passwordEncoder).encode("Password123!");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when username already exists")
    void shouldThrowExceptionWhenUsernameAlreadyExists() {
        // Given
        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("testuser");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when email already exists")
    void shouldThrowExceptionWhenEmailAlreadyExists() {
        // Given
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("test@example.com");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when role not found")
    void shouldThrowExceptionWhenRoleNotFound() {
        // Given
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(roleRepository.findByName(RoleType.USER)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(RoleNotFoundException.class)
                .hasMessageContaining("USER");

        verify(userRepository, never()).save(any());
    }

    // LOGIN TESTS
    @Test
    @DisplayName("Should login user successfully")
    void shouldLoginUserSuccessfully() {
        // Given
        Authentication authentication = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(customUserDetails);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(userRepository.save(testUser)).thenReturn(testUser);
        when(userMapper.toTokenResponse(anyString(), anyString(), anyLong(), any(CustomUserDetails.class)))
                .thenReturn(tokenResponse);
        when(jwtService.generateToken(any(CustomUserDetails.class))).thenReturn("access-token");
        when(jwtService.generateRefreshToken(any(CustomUserDetails.class))).thenReturn("refresh-token");
        when(jwtService.getExpirationTime()).thenReturn(3600000L);

        // When
        TokenResponse result = authService.login(authRequest);

        // Then
        assertThat(result).isEqualTo(tokenResponse);
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository).findByUsername("testuser");
        verify(userRepository).save(testUser);
        assertThat(testUser.getLastLoginAt()).isNotNull();
    }

    @Test
    @DisplayName("Should throw exception when authentication fails")
    void shouldThrowExceptionWhenAuthenticationFails() {
        // Given
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        // When & Then
        assertThatThrownBy(() -> authService.login(authRequest))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Invalid credentials");

        verify(userRepository, never()).findByUsername(anyString());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when user not found after authentication")
    void shouldThrowExceptionWhenUserNotFoundAfterAuthentication() {
        // Given
        Authentication authentication = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(customUserDetails);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> authService.login(authRequest))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("User not found: testuser");

        verify(userRepository, never()).save(any());
    }

    // REFRESH TOKEN TESTS
    @Test
    @DisplayName("Should refresh token successfully")
    void shouldRefreshTokenSuccessfully() {
        // Given
        RefreshTokenRequest refreshRequest = new RefreshTokenRequest("valid-refresh-token");
        when(jwtService.isRefreshToken("valid-refresh-token")).thenReturn(true);
        when(jwtService.extractUsername("valid-refresh-token")).thenReturn("testuser");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(jwtService.isTokenValid(eq("valid-refresh-token"), any(CustomUserDetails.class))).thenReturn(true);
        when(userMapper.toTokenResponse(anyString(), anyString(), anyLong(), any(CustomUserDetails.class)))
                .thenReturn(tokenResponse);
        when(jwtService.generateToken(any(CustomUserDetails.class))).thenReturn("new-access-token");
        when(jwtService.generateRefreshToken(any(CustomUserDetails.class))).thenReturn("new-refresh-token");
        when(jwtService.getExpirationTime()).thenReturn(3600000L);

        // When
        TokenResponse result = authService.refreshToken(refreshRequest);

        // Then
        assertThat(result).isEqualTo(tokenResponse);
        verify(jwtService).isRefreshToken("valid-refresh-token");
        verify(jwtService).extractUsername("valid-refresh-token");
        verify(userRepository).findByUsername("testuser");
        verify(jwtService).isTokenValid(eq("valid-refresh-token"), any(CustomUserDetails.class));
    }

    @Test
    @DisplayName("Should throw exception when token is not a refresh token")
    void shouldThrowExceptionWhenTokenIsNotRefreshToken() {
        // Given
        RefreshTokenRequest refreshRequest = new RefreshTokenRequest("access-token");
        when(jwtService.isRefreshToken("access-token")).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> authService.refreshToken(refreshRequest))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("Provided token is not a refresh token");

        verify(userRepository, never()).findByUsername(anyString());
    }

    @Test
    @DisplayName("Should throw exception when refresh token is malformed")
    void shouldThrowExceptionWhenRefreshTokenIsMalformed() {
        // Given
        RefreshTokenRequest refreshRequest = new RefreshTokenRequest("malformed-refresh-token");
        when(jwtService.isRefreshToken("malformed-refresh-token")).thenReturn(true);
        when(jwtService.extractUsername("malformed-refresh-token")).thenThrow(new JwtException("Invalid token"));

        // When & Then
        assertThatThrownBy(() -> authService.refreshToken(refreshRequest))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("invalid");

        verify(userRepository, never()).findByUsername(anyString());
    }
    
    @Test
    @DisplayName("Should throw exception when user not found for refresh token")
    void shouldThrowExceptionWhenUserNotFoundForRefreshToken() {
        // Given
        RefreshTokenRequest refreshRequest = new RefreshTokenRequest("valid-refresh-token");
        when(jwtService.isRefreshToken("valid-refresh-token")).thenReturn(true);
        when(jwtService.extractUsername("valid-refresh-token")).thenReturn("deleteduser");
        when(userRepository.findByUsername("deleteduser")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> authService.refreshToken(refreshRequest))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("User not found: deleteduser");

        verify(userRepository).findByUsername("deleteduser");
        verify(jwtService, never()).isTokenValid(anyString(), any());
    }
    
    @Test
    @DisplayName("Should throw exception when refresh token is expired")
    void shouldThrowExceptionWhenRefreshTokenIsExpired() {
        // Given
        RefreshTokenRequest refreshRequest = new RefreshTokenRequest("expired-refresh-token");
        when(jwtService.isRefreshToken("expired-refresh-token")).thenReturn(true);
        when(jwtService.extractUsername("expired-refresh-token")).thenReturn("testuser");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(jwtService.isTokenValid(eq("expired-refresh-token"), any(CustomUserDetails.class))).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> authService.refreshToken(refreshRequest))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("Refresh token is invalid or expired");

        verify(userRepository).findByUsername("testuser");
        verify(jwtService).isTokenValid(eq("expired-refresh-token"), any(CustomUserDetails.class));
    }

    // LOGOUT TESTS
    @Test
    @DisplayName("Should logout successfully with both tokens")
    void shouldLogoutSuccessfullyWithBothTokens() {
        // Given
        String authHeader = "Bearer valid-access-token";
        LogoutRequest logoutRequest = new LogoutRequest("valid-refresh-token");
        
        when(jwtService.isRefreshToken("valid-access-token")).thenReturn(false);
        when(jwtService.extractUsername("valid-access-token")).thenReturn("testuser");
        when(jwtService.isRefreshToken("valid-refresh-token")).thenReturn(true);
        when(jwtService.extractUsername("valid-refresh-token")).thenReturn("testuser");
        doNothing().when(tokenBlacklistService).blacklistToken("valid-access-token");
        doNothing().when(tokenBlacklistService).blacklistToken("valid-refresh-token");

        // When
        authService.logout(authHeader, logoutRequest);

        // Then
        verify(jwtService).extractUsername("valid-access-token");
        verify(jwtService).isRefreshToken("valid-refresh-token");
        verify(jwtService).extractUsername("valid-refresh-token");
        verify(tokenBlacklistService).blacklistToken("valid-access-token");
        verify(tokenBlacklistService).blacklistToken("valid-refresh-token");
    }

    @Test
    @DisplayName("Should logout successfully with access token only")
    void shouldLogoutSuccessfullyWithAccessTokenOnly() {
        // Given
        String authHeader = "Bearer valid-access-token";
        LogoutRequest logoutRequest = null;
        
        when(jwtService.isRefreshToken("valid-access-token")).thenReturn(false);
        when(jwtService.extractUsername("valid-access-token")).thenReturn("testuser");
        doNothing().when(tokenBlacklistService).blacklistToken("valid-access-token");

        // When
        authService.logout(authHeader, logoutRequest);

        // Then
        verify(jwtService).extractUsername("valid-access-token");
        verify(tokenBlacklistService).blacklistToken("valid-access-token");
        verify(jwtService, times(1)).isRefreshToken("valid-access-token");
    }

    @Test
    @DisplayName("Should logout successfully with refresh token only")
    void shouldLogoutSuccessfullyWithRefreshTokenOnly() {
        // Given
        String authHeader = null;
        LogoutRequest logoutRequest = new LogoutRequest("valid-refresh-token");
        
        when(jwtService.isRefreshToken("valid-refresh-token")).thenReturn(true);
        when(jwtService.extractUsername("valid-refresh-token")).thenReturn("testuser");
        doNothing().when(tokenBlacklistService).blacklistToken("valid-refresh-token");

        // When
        authService.logout(authHeader, logoutRequest);

        // Then
        verify(jwtService).isRefreshToken("valid-refresh-token");
        verify(jwtService).extractUsername("valid-refresh-token");
        verify(tokenBlacklistService).blacklistToken("valid-refresh-token");
    }

    @Test
    @DisplayName("Should throw exception when no tokens provided for logout")
    void shouldThrowExceptionWhenNoTokensProvidedForLogout() {
        // Given
        String authHeader = null;
        LogoutRequest logoutRequest = null;

        // When & Then
        assertThatThrownBy(() -> authService.logout(authHeader, logoutRequest))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("No valid tokens provided for logout");

        verify(tokenBlacklistService, never()).blacklistToken(anyString());
    }

    @Test
    @DisplayName("Should throw exception when access token is invalid")
    void shouldThrowExceptionWhenAccessTokenIsInvalid() {
        // Given
        String authHeader = "Bearer invalid-access-token";
        LogoutRequest logoutRequest = null;
        
        when(jwtService.isRefreshToken("invalid-access-token")).thenReturn(false);
        when(jwtService.extractUsername("invalid-access-token")).thenThrow(new JwtException("Invalid access token"));

        // When & Then
        assertThatThrownBy(() -> authService.logout(authHeader, logoutRequest))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("Invalid access token");

        verify(tokenBlacklistService, never()).blacklistToken(anyString());
    }

    @Test
    @DisplayName("Should throw exception when refresh token is not refresh type")
    void shouldThrowExceptionWhenRefreshTokenIsNotRefreshType() {
        // Given
        String authHeader = null;
        LogoutRequest logoutRequest = new LogoutRequest("access-token-not-refresh");
        
        when(jwtService.isRefreshToken("access-token-not-refresh")).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> authService.logout(authHeader, logoutRequest))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("Provided token is not a refresh token");

        verify(tokenBlacklistService, never()).blacklistToken(anyString());
    }

    @Test
    @DisplayName("Should throw exception when refresh token format is invalid")
    void shouldThrowExceptionWhenRefreshTokenFormatIsInvalid() {
        // Given
        String authHeader = null;
        LogoutRequest logoutRequest = new LogoutRequest("invalid-refresh-token");
        
        when(jwtService.isRefreshToken("invalid-refresh-token")).thenReturn(true);
        when(jwtService.extractUsername("invalid-refresh-token")).thenThrow(new JwtException("Invalid refresh token"));

        // When & Then
        assertThatThrownBy(() -> authService.logout(authHeader, logoutRequest))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("Invalid refresh token");

        verify(tokenBlacklistService, never()).blacklistToken(anyString());
    }
    
    @Test
    @DisplayName("Should throw exception when trying to use refresh token as access token")
    void shouldThrowExceptionWhenUsingRefreshTokenAsAccessToken() {
        // Given
        String authHeader = "Bearer refresh-token-as-access";
        LogoutRequest logoutRequest = null;
        
        when(jwtService.isRefreshToken("refresh-token-as-access")).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> authService.logout(authHeader, logoutRequest))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("Cannot use refresh token as access token");

        verify(tokenBlacklistService, never()).blacklistToken(anyString());
    }
}
