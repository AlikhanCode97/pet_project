package com.example.Games.user.auth;

import com.example.Games.user.auth.dto.*;
import com.example.Games.user.role.Role;
import com.example.Games.user.role.RoleRepository;
import com.example.Games.config.security.CustomUserDetails;
import com.example.Games.config.security.JwtService;
import com.example.Games.config.security.TokenBlacklistService;
import com.example.Games.config.exception.user.UserAlreadyExistsException;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final TokenBlacklistService tokenBlacklistService;
    private final UserMapper userMapper;

    @Transactional
    public TokenResponse register(RegisterRequest request) {
        log.info("Registering new user: {}", request.username());

        if (userRepository.existsByUsername(request.username())) {
            throw UserAlreadyExistsException.username(request.username());
        }

        if (userRepository.existsByEmail(request.email())) {
            throw UserAlreadyExistsException.email(request.email());
        }

        Role userRole = roleRepository.findByName(request.roleType())
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + request.roleType()));

        User user = User.builder()
                .username(request.username())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role(userRole)
                .build();

        user = userRepository.save(user);

        log.info("Successfully registered user: {} with role: {}",
                user.getUsername(), user.getRole().getName());

        CustomUserDetails userDetails = new CustomUserDetails(user);
        return generateTokenResponse(userDetails);
    }

    @Transactional
    public TokenResponse login(AuthRequest request) {
        log.info("Login attempt for user: {}", request.username());

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.username(),
                            request.password()
                    )
            );

            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            
            User user = userRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new UsernameNotFoundException("User not found: " + userDetails.getUsername()));
            user.updateLastLogin();
            userRepository.save(user);
            
            log.info("Successfully authenticated user: {}", userDetails.getUsername());
            return generateTokenResponse(userDetails);

        } catch (AuthenticationException e) {
            log.warn("Failed login attempt for user: {}", request.username());
            throw new BadCredentialsException("Invalid username or password");
        }
    }

    @Transactional
    public TokenResponse refreshToken(RefreshTokenRequest request) {
        log.info("Refresh token request received");

        try {
            String refreshToken = request.refreshToken();

            if (!jwtService.isRefreshToken(refreshToken)) {
                throw new BadCredentialsException("Not a refresh token");
            }

            String username = jwtService.extractUsername(refreshToken);

            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

            CustomUserDetails userDetails = new CustomUserDetails(user);

            if (!jwtService.isTokenValid(refreshToken, userDetails)) {
                throw new BadCredentialsException("Invalid or expired refresh token");
            }

            log.info("Refreshing tokens for user: {}", username);
            return generateTokenResponse(userDetails);

        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Invalid refresh token: {}", e.getMessage());
            throw new BadCredentialsException("Invalid refresh token");
        }
    }

    public void logout(String authHeader, LogoutRequest request) {
        log.info("Processing logout request");
        
        boolean accessTokenProcessed = false;
        boolean refreshTokenProcessed = false;

        String accessToken = extractTokenFromHeader(authHeader);
        if (accessToken != null && !accessToken.trim().isEmpty()) {
            try {
                jwtService.extractUsername(accessToken);
                tokenBlacklistService.blacklistToken(accessToken);
                log.info("Access token blacklisted successfully");
                accessTokenProcessed = true;
            } catch (JwtException e) {
                log.warn("Invalid access token format: {}", e.getMessage());
                throw new BadCredentialsException("Invalid access token");
            } catch (Exception e) {
                log.error("Failed to blacklist access token: {}", e.getMessage());
                throw new RuntimeException("Logout failed: unable to invalidate access token");
            }
        } else {
            log.warn("No access token found in Authorization header");
        }

        if (request != null && request.refreshToken() != null && !request.refreshToken().trim().isEmpty()) {
            try {
                // Validate refresh token format and type
                if (!jwtService.isRefreshToken(request.refreshToken())) {
                    throw new BadCredentialsException("Provided token is not a refresh token");
                }
                jwtService.extractUsername(request.refreshToken()); // Validate format
                tokenBlacklistService.blacklistToken(request.refreshToken());
                log.info("Refresh token blacklisted successfully");
                refreshTokenProcessed = true;
            } catch (JwtException e) {
                log.warn("Invalid refresh token format: {}", e.getMessage());
                throw new BadCredentialsException("Invalid refresh token");
            } catch (BadCredentialsException e) {
                throw e; // Re-throw business exceptions
            } catch (Exception e) {
                log.error("Failed to blacklist refresh token: {}", e.getMessage());
                throw new RuntimeException("Logout failed: unable to invalidate refresh token");
            }
        } else {
            log.debug("No refresh token provided in request body");
        }

        if (!accessTokenProcessed && !refreshTokenProcessed) {
            log.warn("Logout attempted with no valid tokens");
            throw new BadCredentialsException("No valid tokens provided for logout");
        }
        
        log.info("User successfully logged out - {} tokens blacklisted", 
                (accessTokenProcessed ? 1 : 0) + (refreshTokenProcessed ? 1 : 0));
    }
    
    private String extractTokenFromHeader(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    private TokenResponse generateTokenResponse(CustomUserDetails userDetails) {
        String accessToken = jwtService.generateToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);
        long expiresIn = jwtService.getExpirationTime() / 1000;

        return userMapper.toTokenResponse(
            accessToken,
            refreshToken,
            expiresIn,
            userDetails
        );
    }
}
