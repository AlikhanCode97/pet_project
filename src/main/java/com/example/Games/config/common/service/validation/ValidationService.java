package com.example.Games.config.common.service.validation;

import com.example.Games.config.common.service.validation.validator.*;
import com.example.Games.game.Game;
import com.example.Games.user.auth.User;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class ValidationService {

    public void validateGame(Game game) {
        GameValidator.validate(game);
    }

    public void validateGameId(Long gameId) {
        IdValidator.validateGameId(gameId);
    }

    public void validateUser(User user) {
        UserValidator.validate(user);
    }

    public void validateUserId(Long userId) {
        IdValidator.validateUserId(userId);
    }

    public void validatePurchasePrice(BigDecimal price) {
        PriceValidator.validate(price);
    }

    public void validateAmount(BigDecimal amount) {
        PriceValidator.validate(amount);
    }

    public void validatePagination(int page, int size) {
        PaginationValidator.validate(page, size);
    }

    public void validateId(Long id, String fieldName) {
        IdValidator.validateNotNull(id, fieldName);
    }


    public void validateTitle(String title) {
        StringValidator.validateTitle(title);
    }

    public void validateUsername(String username) {
        StringValidator.validateUsername(username);
    }

}
