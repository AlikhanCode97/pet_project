package com.example.Games.config.common.service.validation.validator;

import com.example.Games.game.Game;

public final class GameValidator {

    private GameValidator() {}

    public static void validate(Game game) {
        validateNotNull(game);
        validatePersisted(game);
        validateTitle(game);
    }

    public static void validateNotNull(Game game) {
        if (game == null) {
            throw new IllegalArgumentException("Game cannot be null");
        }
    }

    public static void validatePersisted(Game game) {
        if (game.getId() == null) {
            throw new IllegalArgumentException("Game must be persisted (ID cannot be null)");
        }
    }

    public static void validateTitle(Game game) {
        if (game.getTitle() == null || game.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Game title cannot be null or empty");
        }
    }
}
