package com.example.Games.user;

import com.example.Games.user.balance.Balance;
import com.example.Games.user.balance.BalanceRepository;
import com.example.Games.user.dto.*;
import com.example.Games.user.role.Role;
import com.example.Games.user.role.RoleRepository;
import com.example.Games.config.security.CustomUserDetails;
import com.example.Games.config.security.JwtService;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final BalanceRepository balanceRepository;

    public TokenResponse register(RegisterRequest request) {

        log.info("Registering new user: {}", request.username());

        // Check if user already exists
        if (userRepository.existsByUsername(request.username())) {
            throw new IllegalArgumentException("Username already exists: " + request.username());
        }

        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already exists: " + request.email());
        }
        // Get the requested role
        Role userRole = roleRepository.findByName(request.roleType())
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + request.roleType()));


        User user = User.builder()
                .username(request.username())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role(userRole)
                .build();

        user = userRepository.save(user);

        Balance balance = Balance.builder()
                .user(user)
                .amount(BigDecimal.ZERO)
                .build();

        balanceRepository.save(balance);
        log.info("Successfully registered user: {} with role: {}", user.getUsername(), user.getRole().getName());

        CustomUserDetails userDetails = new CustomUserDetails(user);
        return generateTokenResponse(userDetails);
    }

    public TokenResponse login(AuthRequest request) {
        log.info("Login attempt for user: {}", request.username());

        try {
            // Authenticate the user
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.username(),
                            request.password()
                    )
            );

        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        log.info("Successfully authenticated user: {}", user.getUsername());

        CustomUserDetails userDetails = new CustomUserDetails(user);
        return generateTokenResponse(userDetails);

        } catch (AuthenticationException e) {
            log.warn("Failed login attempt for user: {}", request.username());
            throw new BadCredentialsException("Invalid username or password");
        }
    }
    public TokenResponse refreshToken(RefreshTokenRequest request) {
        log.info("Refresh token request received");

        try {
            String refreshToken = request.refreshToken();

            // Validate that this is actually a refresh token
            if (!jwtService.isRefreshToken(refreshToken)) {
                throw new JwtException("Invalid refresh token");
            }

            // Extract username from refresh token
            String username = jwtService.extractUsername(refreshToken);

            // Load user details
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

            CustomUserDetails userDetails = new CustomUserDetails(user);

            // Validate refresh token
            if (jwtService.isTokenValid(refreshToken, userDetails)) {
                log.info("Refreshing tokens for user: {}", username);
                return generateTokenResponse(userDetails);
            } else {
                throw new JwtException("Invalid or expired refresh token");
            }

        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Invalid refresh token: {}", e.getMessage());
            throw new BadCredentialsException("Invalid refresh token");
        }
    }

    private TokenResponse
    generateTokenResponse(CustomUserDetails userDetails) {
        String accessToken = jwtService.generateToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);
        long expiresIn = jwtService.getExpirationTime();

        return new TokenResponse(
                accessToken,
                refreshToken,
                expiresIn,
                userDetails.getUserId(),
                userDetails.getUsername(),
                userDetails.getEmail(),
                userDetails.getRoleName()
        );
    }
}
