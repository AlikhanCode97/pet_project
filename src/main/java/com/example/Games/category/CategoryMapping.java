package com.example.Games.category;

public class CategoryMapping {

    public static CategoryResponse toDto(Category category){
        return new CategoryResponse(
                category.getId(),
                category.getName()
        );
    }

    public static Category toEntity(CategoryRequest request) {
        return Category.of(request.name());
    }
}
