package com.example.Games.category;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")

public interface CategoryMapStruct {

    // Map from DTO to Entity (no id, rating, or releaseDate in request)
    @Mapping(target = "id", ignore = true)
    Category toEntity(CategoryRequest request);
    // Map from Entity to Response
    CategoryResponse toDto(Category category);

    List<CategoryResponse> toDtoList(List<Category> categories);
}
