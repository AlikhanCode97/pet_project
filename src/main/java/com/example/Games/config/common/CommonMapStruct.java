package com.example.Games.config.common;

import com.example.Games.category.Category;
import com.example.Games.category.CategoryResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CommonMapStruct {
    CategoryResponse toDto(Category category);
}
