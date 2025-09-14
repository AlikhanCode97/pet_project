package com.example.Games.config.common.service.validation.validator;

public final class IdValidator {

    private IdValidator() {
    }

    public static void validateNotNull(Long id, String fieldName) {
        if (id == null) {
            throw new IllegalArgumentException(fieldName + " cannot be null");
        }
    }

    public static void validateGameId(Long gameId) {
        validateNotNull(gameId, "Game ID");
    }

    public static void validateUserId(Long userId) {
        validateNotNull(userId, "User ID");
    }

}
