package com.example.Games.config.common.service;

import com.example.Games.config.exception.auth.UserNotFoundException;
import com.example.Games.user.auth.User;
import com.example.Games.user.auth.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserContextService {

    private final UserRepository userRepository;

    public User getAuthorizedUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("No authenticated user found");
        }

        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.warn("User not found in database: {}", username);
                    return new UserNotFoundException(username);
                });
    }

    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("User not found with ID: {}", userId);
                    return new UserNotFoundException(userId);
                });
    }
}