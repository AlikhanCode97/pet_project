package com.example.Games.config.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final TokenBlacklistService tokenBlacklistService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            final String jwt = authHeader.substring(7);
            final String username = jwtService.extractUsername(jwt);

            // Process token if username exists and no authentication is set
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                authenticateFromJwt(jwt, username, request);
            }
        } catch (JwtException e) {
            log.warn("JWT token validation failed: {}", e.getMessage());
            // Don't throw exception, just log and continue - let the request proceed unauthenticated
        } catch (Exception e) {
            log.error("Unexpected error during JWT authentication: {}", e.getMessage());
        }
        filterChain.doFilter(request, response);
    }

    private void authenticateFromJwt(String jwt, String username, HttpServletRequest request) {
        try {
            // Check if this is a refresh token (should not be used for authentication)
            if (jwtService.isRefreshToken(jwt)) {
                log.warn("Refresh token used for authentication attempt for user: {}", username);
                return;
            }
            
            // Check if token is blacklisted (logged out)
            if (tokenBlacklistService.isTokenBlacklisted(jwt)) {
                log.debug("Blacklisted token used for authentication attempt for user: {}", username);
                return;
            }

            Long userId = jwtService.extractUserId(jwt);
            String userRole = jwtService.extractUserRole(jwt);
            String email = jwtService.extractUserEmail(jwt);

            if (userId != null && userRole != null) {
                // Create lightweight JWT-based UserDetails
                JwtUserPrincipal userPrincipal = new JwtUserPrincipal(userId, username, email, userRole);

                // Validate token with minimal user details
                if (jwtService.isTokenValid(jwt, userPrincipal)) {
                    // Create authorities from JWT claims
                    List<GrantedAuthority> authorities = List.of(
                            new SimpleGrantedAuthority("ROLE_" + userRole)
                    );

                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(userPrincipal, null, authorities);

                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);

                    log.debug("Successfully authenticated user: {} with role: {}", username, userRole);
                } else {
                    log.warn("Invalid JWT token for user: {}", username);
                }
            }
        } catch (Exception e) {
            log.error("Error authenticating user {}: {}", username, e.getMessage());
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Skip JWT authentication for public endpoints
        String path = request.getRequestURI();
        return path.startsWith("/api/auth/") ||
                path.startsWith("/swagger-ui/") ||
                path.startsWith("/v3/api-docs/") ||
                (path.startsWith("/api/games/") && "GET".equals(request.getMethod()));
    }
}

