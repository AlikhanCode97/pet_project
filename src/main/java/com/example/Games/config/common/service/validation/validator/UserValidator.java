package com.example.Games.config.common.service.validation.validator;

import com.example.Games.user.auth.User;

public final class UserValidator {

    private UserValidator() {}

    public static void validate(User user) {
        validateNotNull(user);
        validatePersisted(user);
    }

    public static void validateNotNull(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
    }

    public static void validatePersisted(User user) {
        if (user.getId() == null) {
            throw new IllegalArgumentException("User must be persisted (ID cannot be null)");
        }
    }
}
