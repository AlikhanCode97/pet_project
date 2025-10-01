package com.example.Games.config;

import com.example.Games.config.common.service.UserContextService;
import com.example.Games.config.exception.auth.UserNotFoundException;
import com.example.Games.user.auth.User;
import com.example.Games.user.auth.UserRepository;
import com.example.Games.user.role.Role;
import com.example.Games.user.role.RoleType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserContextService Tests")
class UserContextServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserContextService userContextService;

    private User testUser;
    private User adminUser;
    private SecurityContext securityContext;

    @BeforeEach
    void setUp() {
        // Setup roles
        Role userRole = Role.builder()
                .id(1L)
                .name(RoleType.USER)
                .build();

        Role adminRole = Role.builder()
                .id(2L)
                .name(RoleType.ADMIN)
                .build();

        // Setup users
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .password("password123")
                .role(userRole)
                .build();

        adminUser = User.builder()
                .id(2L)
                .username("admin")
                .email("admin@example.com")
                .password("admin123")
                .role(adminRole)
                .build();

        // Setup security context
        securityContext = new SecurityContextImpl();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Should get authorized user successfully")
    void shouldGetAuthorizedUserSuccessfully() {
        // Given
        String username = "testuser";
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                username,
                "password",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );

        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(testUser));

        // When
        User result = userContextService.getAuthorizedUser();

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(testUser);
        assertThat(result.getUsername()).isEqualTo(username);
        assertThat(result.getId()).isEqualTo(1L);

        verify(userRepository).findByUsername(username);
    }

    @Test
    @DisplayName("Should get admin user successfully")
    void shouldGetAdminUserSuccessfully() {
        // Given
        String adminUsername = "admin";
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                adminUsername,
                "password",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );

        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);

        when(userRepository.findByUsername(adminUsername)).thenReturn(Optional.of(adminUser));

        // When
        User result = userContextService.getAuthorizedUser();

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(adminUser);
        assertThat(result.getRole().getName()).isEqualTo(RoleType.ADMIN);

        verify(userRepository).findByUsername(adminUsername);
    }

    @Test
    @DisplayName("Should throw exception when no authentication exists")
    void shouldThrowExceptionWhenNoAuthenticationExists() {
        // Given
        SecurityContextHolder.setContext(securityContext); // No authentication set

        // When & Then
        assertThatThrownBy(() -> userContextService.getAuthorizedUser())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("No authenticated user found");

        verify(userRepository, never()).findByUsername(any());
    }

    @Test
    @DisplayName("Should throw exception when authentication is not authenticated")
    void shouldThrowExceptionWhenAuthenticationIsNotAuthenticated() {
        // Given
        Authentication authentication = mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(false);

        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);

        // When & Then
        assertThatThrownBy(() -> userContextService.getAuthorizedUser())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("No authenticated user found");

        verify(userRepository, never()).findByUsername(any());
    }

    @Test
    @DisplayName("Should throw exception when user not found by username")
    void shouldThrowExceptionWhenUserNotFoundByUsername() {
        // Given
        String nonExistentUsername = "nonexistent";
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                nonExistentUsername,
                "password",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );

        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);

        when(userRepository.findByUsername(nonExistentUsername)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userContextService.getAuthorizedUser())
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining(nonExistentUsername);

        verify(userRepository).findByUsername(nonExistentUsername);
    }

    @Test
    @DisplayName("Should get user by ID successfully")
    void shouldGetUserByIdSuccessfully() {
        // Given
        Long userId = 1L;
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        // When
        User result = userContextService.getUserById(userId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(testUser);
        assertThat(result.getId()).isEqualTo(userId);

        verify(userRepository).findById(userId);
    }

    @Test
    @DisplayName("Should throw exception when user not found by ID")
    void shouldThrowExceptionWhenUserNotFoundById() {
        // Given
        Long nonExistentId = 999L;
        when(userRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userContextService.getUserById(nonExistentId))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining(nonExistentId.toString());

        verify(userRepository).findById(nonExistentId);
    }

    @Test
    @DisplayName("Should handle principal as username string")
    void shouldHandlePrincipalAsUsernameString() {
        // Given
        String username = "testuser";
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                username,  // Principal as String
                null,
                Collections.emptyList()
        );

        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(testUser));

        // When
        User result = userContextService.getAuthorizedUser();

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(testUser);

        verify(userRepository).findByUsername(username);
    }
}