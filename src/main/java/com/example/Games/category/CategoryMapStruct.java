package com.example.Games.category;

import com.example.Games.category.dto.CategoryRequest;
import com.example.Games.category.dto.CategoryResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CategoryMapStruct {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "games", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Category toEntity(CategoryRequest request);

    @Mapping(target = "createdByUsername", source = "createdBy.username")
    @Mapping(target = "createdById", source = "createdBy.id")
    CategoryResponse toDto(Category category);

    List<CategoryResponse> toDtoList(List<Category> categories);

}
