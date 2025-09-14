package com.example.Games.category;

import com.example.Games.category.dto.CategoryRequest;
import com.example.Games.category.dto.CategoryResponse;
import com.example.Games.config.exception.category.CategoryNotFoundException;
import com.example.Games.config.exception.category.CategoryAlreadyExistsException;
import com.example.Games.config.exception.category.CategoryInUseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapStruct categoryMapStruct;

    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        log.info("Creating new category with name: {}", request.name());

        if (categoryRepository.findByName(request.name()).isPresent()) {
            throw CategoryAlreadyExistsException.withName(request.name());
        }

        Category category = categoryMapStruct.toEntity(request);
        Category savedCategory = categoryRepository.save(category);
        log.info("Successfully created category '{}' with ID: {}", 
                savedCategory.getName(), savedCategory.getId());
        return categoryMapStruct.toDto(savedCategory);
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {
        log.debug("Retrieving all categories");

        List<Category> categories = categoryRepository.findAll();
        log.debug("Found {} categories", categories.size());
        
        return categoryMapStruct.toDtoList(categories);
    }


    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(Long id) {
        log.debug("Retrieving category with ID: {}", id);
        
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> CategoryNotFoundException.byId(id));
        
        log.debug("Found category '{}' with ID: {}", category.getName(), id);
        return categoryMapStruct.toDto(category);
    }

    @Transactional
    public CategoryResponse updateCategory(Long id, CategoryRequest updateRequest) {
        log.info("Updating category with ID: {} to name: {}", id, updateRequest.name());
        
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> CategoryNotFoundException.byId(id));

        if (!category.isNameEqual(updateRequest.name())) {
            categoryRepository.findByName(updateRequest.name())
                    .ifPresent(existing -> {
                        throw CategoryAlreadyExistsException.withName(updateRequest.name());
                    });
        }

        category.updateName(updateRequest.name());
        Category updatedCategory = categoryRepository.save(category);
        
        log.info("Successfully updated category ID: {} to name: '{}'", 
                id, updatedCategory.getName());
        
        return categoryMapStruct.toDto(updatedCategory);
    }

    @Transactional
    public void deleteCategory(Long id) {
        log.info("Deleting category with ID: {}", id);
        
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> CategoryNotFoundException.byId(id));

        if (category.hasGames()) {
            throw CategoryInUseException.withGames(category.getName(), category.getGameCount());
        }
        
        categoryRepository.deleteById(id);
        
        log.info("Successfully deleted category '{}' with ID: {}", category.getName(), id);
    }
}
