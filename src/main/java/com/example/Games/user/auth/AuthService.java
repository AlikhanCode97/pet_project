package com.example.Games.user.auth;

import com.example.Games.user.auth.dto.*;
import com.example.Games.user.role.Role;
import com.example.Games.user.role.RoleRepository;
import com.example.Games.config.security.CustomUserDetails;
import com.example.Games.config.security.JwtService;
import com.example.Games.config.security.TokenBlacklistService;
import com.example.Games.config.exception.auth.UserAlreadyExistsException;
import com.example.Games.config.exception.auth.RoleNotFoundException;
import com.example.Games.config.exception.auth.UserNotFoundException;
import com.example.Games.config.exception.auth.InvalidTokenException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
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
                .orElseThrow(() -> new RoleNotFoundException(request.roleType().toString()));

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

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.username(),
                        request.password()
                )
        );

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new UserNotFoundException(userDetails.getUsername()));
        user.updateLastLogin();
        userRepository.save(user);

        log.info("Successfully authenticated user: {}", userDetails.getUsername());
        return generateTokenResponse(userDetails);
    }

    @Transactional
    public TokenResponse refreshToken(RefreshTokenRequest request) {
        log.info("Refresh token request received");
        String refreshToken = request.refreshToken();

        if (!jwtService.isRefreshToken(refreshToken)) {
            throw new InvalidTokenException(
                InvalidTokenException.TokenErrorType.WRONG_TYPE,
                "Provided token is not a refresh token"
            );
        }
        
        String username;
        try {
            username = jwtService.extractUsername(refreshToken);
        } catch (Exception e) {
            log.error("Failed to extract username from refresh token", e);
            throw new InvalidTokenException(
                InvalidTokenException.TokenErrorType.MALFORMED,
                "Refresh token is invalid"
            );
        }
        
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException(username));
        CustomUserDetails userDetails = new CustomUserDetails(user);

        if (!jwtService.isTokenValid(refreshToken, userDetails)) {
            throw new InvalidTokenException(
                InvalidTokenException.TokenErrorType.EXPIRED,
                "Refresh token is invalid or expired"
            );
        }

        log.info("Refreshing tokens for user: {}", username);
        return generateTokenResponse(userDetails);
    }

    public void logout(String authHeader, LogoutRequest request) {
        log.info("Processing logout request");
        
        boolean accessTokenProcessed = false;
        boolean refreshTokenProcessed = false;

        String accessToken = extractTokenFromHeader(authHeader);
        if (accessToken != null && !accessToken.trim().isEmpty()) {
            if (jwtService.isRefreshToken(accessToken)) {
                throw new InvalidTokenException(
                    InvalidTokenException.TokenErrorType.WRONG_TYPE,
                    "Cannot use refresh token as access token"
                );
            }
            try {
                jwtService.extractUsername(accessToken);
            } catch (Exception e) {
                log.error("Invalid access token", e);
                throw new InvalidTokenException(
                    InvalidTokenException.TokenErrorType.MALFORMED,
                    "Invalid access token"
                );
            }
            tokenBlacklistService.blacklistToken(accessToken);
            log.info("Access token blacklisted successfully");
            accessTokenProcessed = true;
        } else {
            log.warn("No access token found in Authorization header");
        }

        if (request != null && request.refreshToken() != null && !request.refreshToken().trim().isEmpty()) {
            if (!jwtService.isRefreshToken(request.refreshToken())) {
                throw new InvalidTokenException(
                    InvalidTokenException.TokenErrorType.WRONG_TYPE,
                    "Provided token is not a refresh token"
                );
            }
            try {
                jwtService.extractUsername(request.refreshToken());
            } catch (Exception e) {
                log.error("Invalid refresh token", e);
                throw new InvalidTokenException(
                    InvalidTokenException.TokenErrorType.MALFORMED,
                    "Invalid refresh token"
                );
            }
            tokenBlacklistService.blacklistToken(request.refreshToken());
            log.info("Refresh token blacklisted successfully");
            refreshTokenProcessed = true;
        } else {
            log.debug("No refresh token provided in request body");
        }

        if (!accessTokenProcessed && !refreshTokenProcessed) {
            throw new InvalidTokenException(
                InvalidTokenException.TokenErrorType.MISSING,
                "No valid tokens provided for logout"
            );
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
