package com.example.Games.config.security;

import com.example.Games.user.role.RoleType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Utility class for role-based authorization decisions.
 * Works with both JwtUserPrincipal and CustomUserDetails.
 */
@Component("authUtils")
public class AuthorizationUtils {

    /**
     * Check if current user has the specified role
     */
    public boolean hasRole(String requiredRole) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return hasRole(auth, requiredRole);
    }

    /**
     * Check if authenticated user has the specified role
     */
    public boolean hasRole(Authentication auth, String requiredRole) {
        if (auth == null || auth.getPrincipal() == null) {
            return false;
        }

        String userRole = extractUserRole(auth);
        return requiredRole.equals(userRole);
    }

    /**
     * Check if current user has any of the specified roles
     */
    public boolean hasAnyRole(String... requiredRoles) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return hasAnyRole(auth, requiredRoles);
    }

    /**
     * Check if authenticated user has any of the specified roles
     */
    public boolean hasAnyRole(Authentication auth, String... requiredRoles) {
        if (auth == null || auth.getPrincipal() == null) {
            return false;
        }

        String userRole = extractUserRole(auth);
        for (String role : requiredRoles) {
            if (role.equals(userRole)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if current user is a developer
     */
    public boolean isDeveloper() {
        return hasRole(RoleType.DEVELOPER.name());
    }

    /**
     * Check if authenticated user is a developer
     */
    public boolean isDeveloper(Authentication auth) {
        return hasRole(auth, RoleType.DEVELOPER.name());
    }

    /**
     * Check if current user owns the resource (by userId)
     */
    public boolean isOwner(Long resourceUserId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return isOwner(auth, resourceUserId);
    }

    /**
     * Check if authenticated user owns the resource (by userId)
     */
    public boolean isOwner(Authentication auth, Long resourceUserId) {
        if (auth == null || resourceUserId == null) {
            return false;
        }

        Long currentUserId = extractUserId(auth);
        return resourceUserId.equals(currentUserId);
    }

    /**
     * Check if current user is owner OR has developer role
     */
    public boolean isOwnerOrDeveloper(Long resourceUserId) {
        return isOwner(resourceUserId) || isDeveloper();
    }

    /**
     * Check if authenticated user is owner OR has developer role
     */
    public boolean isOwnerOrDeveloper(Authentication auth, Long resourceUserId) {
        return isOwner(auth, resourceUserId) || isDeveloper(auth);
    }

    /**
     * Get current user's ID
     */
    public Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return extractUserId(auth);
    }

    /**
     * Get current user's role
     */
    public String getCurrentUserRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return extractUserRole(auth);
    }

    /**
     * Extract user ID from authentication (works with both principal types)
     */
    private Long extractUserId(Authentication auth) {
        Object principal = auth.getPrincipal();
        
        if (principal instanceof JwtUserPrincipal jwtPrincipal) {
            return jwtPrincipal.getUserId();
        } else if (principal instanceof CustomUserDetails customUserDetails) {
            return customUserDetails.getUserId();
        }
        
        return null;
    }

    /**
     * Extract user role from authentication (works with both principal types)
     */
    private String extractUserRole(Authentication auth) {
        Object principal = auth.getPrincipal();
        
        if (principal instanceof JwtUserPrincipal jwtPrincipal) {
            return jwtPrincipal.getRole();
        } else if (principal instanceof CustomUserDetails customUserDetails) {
            return customUserDetails.getRoleName();
        }
        
        return null;
    }

    // Future: Permission-based methods can be added here
    // public boolean hasPermission(String permission) { ... }
    // public boolean hasAnyPermission(String... permissions) { ... }
}
