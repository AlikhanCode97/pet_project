package com.example.Games.game;
import com.example.Games.config.common.CommonMapStruct;
import com.example.Games.game.dto.CreateRequest;
import com.example.Games.game.dto.Response;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import com.example.Games.category.Category;
import java.util.List;

@Mapper(componentModel = "spring", uses = { CommonMapStruct.class })
public interface GameMapStruct {

    //Map from DTO to Entity (no id, rating, or releaseDate in request)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "category", source = "category")
    @Mapping(target = "author", source = "author")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Game toEntity(CreateRequest request, Category category, String author);

    //Map from Entity to Response
    Response toDto(Game game);

    List<Response> toDtoList(List<Game> games);
}
