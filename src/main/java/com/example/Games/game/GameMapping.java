package com.example.Games.game;

import com.example.Games.category.Category;
import com.example.Games.category.CategoryMapping;
import com.example.Games.game.dto.CreateRequest;
import com.example.Games.game.dto.Response;

import java.math.BigDecimal;

public class GameMapping {

    public static Response toDto(Game game) {
        return new Response(
                game.getId(),
                game.getTitle(),
                game.getAuthor(),
                game.getPrice(),
                CategoryMapping.toDto(game.getCategory())
        );
    }

    public static Game toEntity(CreateRequest createRequest, Category category){
        return Game.of(
                createRequest.title(),
                createRequest.price(),
                category
        );
    }
}
