package com.example.Games.config.common.service.validation.validator;

public final class StringValidator {

    private StringValidator() {}

    public static void validateTitle(String title) {
        validateNotNullOrEmpty(title, "Title");
    }

    public static void validateUsername(String username) {
        validateNotNullOrEmpty(username, "Username");
    }

    public static void validateNotNullOrEmpty(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " cannot be null or empty");
        }
    }
}
