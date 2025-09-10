package com.example.Games.config.security;

import io.jsonwebtoken.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration:86400}") // 24 hours in seconds, configurable
    private Long jwtExpiration;

    @Value("${jwt.refresh-expiration:604800}") // 7 days in seconds, configurable
    private Long refreshExpiration;

    private SecretKey getSigningKey() {
        byte[] keyBytes = Base64.getDecoder().decode(jwtSecret);
        return new SecretKeySpec(keyBytes, "HmacSHA512");
    }

    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        long expirationInMillis = TimeUnit.SECONDS.toMillis(jwtExpiration);
        return buildToken(extraClaims, userDetails, expirationInMillis);
    }

    public String generateRefreshToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("tokenType", "refresh");
        long expirationInMillis = TimeUnit.SECONDS.toMillis(refreshExpiration);
        return buildToken(claims, userDetails, expirationInMillis);
    }

    private String buildToken(Map<String, Object> extraClaims, UserDetails userDetails, long expiration) {
        // Extract user details regardless of UserDetails implementation
        Long userId = extractUserIdFromUserDetails(userDetails);
        String userRole = extractUserRoleFromUserDetails(userDetails);
        String email = extractEmailFromUserDetails(userDetails);

        // Add business claims
        extraClaims.put("userId", userId);
        extraClaims.put("userRole", userRole);
        extraClaims.put("email", email);

        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration)) // 1 day
                .setIssuer("Games-API")
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Long extractUserId(String token) {
        return extractClaim(token, claims -> {
            Object userIdObj = claims.get("userId");
            if (userIdObj instanceof Integer) {
                return ((Integer) userIdObj).longValue();
            } else if (userIdObj instanceof Long) {
                return (Long) userIdObj;
            }
            return null;
        });
    }

    public String extractUserRole(String token) {
        return extractClaim(token, claims -> (String) claims.get("userRole"));
    }

    public String extractUserEmail(String token) {
        return extractClaim(token, claims -> (String) claims.get("email"));
    }

    public String extractTokenType(String token) {
        return extractClaim(token, claims -> (String) claims.get("tokenType"));
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        try {
            final Claims claims = extractAllClaims(token);
            return claimsResolver.apply(claims);
        } catch (JwtException | IllegalArgumentException e) {
            throw new JwtException("Invalid JWT token", e);
        }
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public boolean isRefreshToken(String token) {
        try {
            return "refresh".equals(extractTokenType(token));
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public boolean isTokenExpired(String token) {
        try {
            return extractExpiration(token).before(new Date());
        } catch (Exception e) {
            return true; // Consider invalid tokens as expired
        }
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            final String username = extractUsername(token);
            return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public long getExpirationTime() {
        return TimeUnit.SECONDS.toMillis(jwtExpiration);
    }

    public long getRefreshExpirationTime() {
        return TimeUnit.SECONDS.toMillis(refreshExpiration);
    }

    private Long extractUserIdFromUserDetails(UserDetails userDetails) {
        if (userDetails instanceof CustomUserDetails customUserDetails) {
            return customUserDetails.getUserId();
        } else if (userDetails instanceof JwtUserPrincipal jwtPrincipal) {
            return jwtPrincipal.getUserId();
        }
        return null;
    }

    private String extractUserRoleFromUserDetails(UserDetails userDetails) {
        if (userDetails instanceof CustomUserDetails customUserDetails) {
            return customUserDetails.getRoleName();
        } else if (userDetails instanceof JwtUserPrincipal jwtPrincipal) {
            return jwtPrincipal.getRole();
        }
        return null;
    }

    private String extractEmailFromUserDetails(UserDetails userDetails) {
        if (userDetails instanceof CustomUserDetails customUserDetails) {
            return customUserDetails.getEmail();
        } else if (userDetails instanceof JwtUserPrincipal jwtPrincipal) {
            return jwtPrincipal.getEmail();
        }
        return null;
    }
}
