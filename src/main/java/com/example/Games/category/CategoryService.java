package com.example.Games.category;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapStruct categoryMapStruct;

    // ✅ Create new category
    public CategoryResponse createCategory(CategoryRequest request) {
        if (categoryRepository.findByName(request.name()).isPresent()) {
            throw new RuntimeException("Category already exists");
        }
        Category category = categoryMapStruct.toEntity(request);
        return categoryMapStruct.toDto(categoryRepository.save(category));
    }

    // ✅ Get all categories
    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAll()
                .stream()
                .map(categoryMapStruct::toDto)
                .toList();
    }

    // ✅ Get by ID
    public CategoryResponse getCategoryById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found"));
        return categoryMapStruct.toDto(category);
    }

    // ✅ Delete category
    public void deleteCategory(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new RuntimeException("Category not found");
        }
        categoryRepository.deleteById(id);
    }
}
