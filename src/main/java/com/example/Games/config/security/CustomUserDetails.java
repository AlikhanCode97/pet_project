package com.example.Games.config.security;

import com.example.Games.user.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public record CustomUserDetails(User user) implements UserDetails {

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Ensure consistent authority naming with "ROLE_" prefix
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().getName().name()));
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    /**
     * Get user ID for JWT token claims
     */
    public Long getUserId() {
        return user.getId();
    }

    /**
     * Get user email
     */
    public String getEmail() {
        return user.getEmail();
    }

    /**
     * Get user role name
     */
    public String getRoleName() {
        return user.getRole().getName().name();
    }

    /**
     * Get the underlying User entity
     */
    public User getUser() {
        return user;
    }

    // Account status methods - these could be enhanced to check actual user status
    @Override
    public boolean isAccountNonExpired() {
        return true; // Could be enhanced to check actual expiration
    }

    @Override
    public boolean isAccountNonLocked() {
        return true; // Could be enhanced to check lock status
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; // Could be enhanced to check password expiration
    }

    @Override
    public boolean isEnabled() {
        return true; // Could be enhanced to check if user is active
    }

    @Override
    public String toString() {
        return "CustomUserDetails{" +
                "userId=" + getUserId() +
                ", username='" + getUsername() + '\'' +
                ", email='" + getEmail() + '\'' +
                ", role='" + getRoleName() + '\'' +
                '}';
    }
}