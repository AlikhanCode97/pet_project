package com.example.Games.config.common.service;

import com.example.Games.config.security.CustomUserDetails;
import com.example.Games.config.security.JwtUserPrincipal;
import com.example.Games.user.role.RoleType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;


@Component("authUtils")
public class AuthorizationUtils {

    public boolean hasRole(String requiredRole) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return hasRole(auth, requiredRole);
    }

    public boolean hasRole(Authentication auth, String requiredRole) {
        if (auth == null || auth.getPrincipal() == null) {
            return false;
        }
        String userRole = extractUserRole(auth);
        return requiredRole.equals(userRole);
    }

    public boolean isDeveloper() {
        return hasRole(RoleType.DEVELOPER.name());
    }

    public boolean isDeveloper(Authentication auth) {
        return hasRole(auth, RoleType.DEVELOPER.name());
    }

    private String extractUserRole(Authentication auth) {
        Object principal = auth.getPrincipal();
        
        if (principal instanceof JwtUserPrincipal jwtPrincipal) {
            return jwtPrincipal.getRole();
        } else if (principal instanceof CustomUserDetails customUserDetails) {
            return customUserDetails.getRoleName();
        }
        return null;
    }
}
