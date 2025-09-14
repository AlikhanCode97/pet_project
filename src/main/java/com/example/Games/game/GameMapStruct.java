package com.example.Games.game;

import com.example.Games.category.Category;
import com.example.Games.category.CategoryMapStruct;
import com.example.Games.game.dto.CreateRequest;
import com.example.Games.game.dto.Response;
import com.example.Games.user.auth.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", uses = CategoryMapStruct.class)
public interface GameMapStruct {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "category", source = "category")
    @Mapping(target = "author", source = "author")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Game toEntity(CreateRequest request, Category category, User author);

    @Mapping(target = "author", source = "author.username")
    Response toDto(Game game);

    List<Response> toDtoList(List<Game> games);
}
