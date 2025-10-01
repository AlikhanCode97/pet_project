package com.example.Games.category;

import com.example.Games.category.dto.CategoryRequest;
import com.example.Games.category.dto.CategoryResponse;
import com.example.Games.config.common.service.UserContextService;
import com.example.Games.config.exception.category.CategoryNotFoundException;
import com.example.Games.config.exception.category.CategoryAlreadyExistsException;
import com.example.Games.config.exception.category.CategoryInUseException;
import com.example.Games.config.exception.category.UnauthorizedCategoryAccessException;
import com.example.Games.user.auth.User;
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
    private final UserContextService userContextService;

    public User getCurrentUser() {
        return userContextService.getAuthorizedUser();
    }

    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        User currentUser = getCurrentUser();
        log.info("Creating new category with name: {} by user: {}", request.name(), currentUser.getUsername());

        if (categoryRepository.findByName(request.name()).isPresent()) {
            throw CategoryAlreadyExistsException.withName(request.name());
        }

        Category category = categoryMapStruct.toEntity(request);
        category.setCreatedBy(currentUser);
        Category savedCategory = categoryRepository.save(category);
        log.info("Successfully created category '{}' with ID: {} by user: {}", 
                savedCategory.getName(), savedCategory.getId(), currentUser.getUsername());
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
        User currentUser = getCurrentUser();
        log.info("Updating category with ID: {} to name: {} by user: {}", id, updateRequest.name(), currentUser.getUsername());

        Category category = categoryRepository.findByIdWithCreator(id)
                .orElseThrow(() -> CategoryNotFoundException.byId(id));

        if (!category.getCreatedBy().getId().equals(currentUser.getId())) {
            throw UnauthorizedCategoryAccessException.notOwner(id, currentUser.getUsername());
        }

        categoryRepository.findByName(updateRequest.name())
                .ifPresent(existing -> {
                    if (!existing.getId().equals(id)) {
                        throw CategoryAlreadyExistsException.withName(updateRequest.name());
                    }
                });

        category.updateName(updateRequest.name());
        Category updatedCategory = categoryRepository.save(category);
        
        log.info("Successfully updated category ID: {} to name: '{}' by user: {}", 
                id, updatedCategory.getName(), currentUser.getUsername());
        
        return categoryMapStruct.toDto(updatedCategory);
    }

    @Transactional
    public void deleteCategory(Long id) {
        User currentUser = getCurrentUser();
        log.info("Deleting category with ID: {} by user: {}", id, currentUser.getUsername());

        Category category = categoryRepository.findByIdWithCreator(id)
                .orElseThrow(() -> CategoryNotFoundException.byId(id));

        if (!category.getCreatedBy().getId().equals(currentUser.getId())) {
            throw UnauthorizedCategoryAccessException.cannotDelete(category.getName(), currentUser.getUsername());
        }

        if (categoryRepository.hasGames(id)) {
            int gameCount = categoryRepository.countGamesByCategoryId(id);
            throw CategoryInUseException.withGames(category.getName(), gameCount);
        }
        
        categoryRepository.deleteById(id);
        
        log.info("Successfully deleted category '{}' with ID: {} by user: {}", category.getName(), id, currentUser.getUsername());
    }
}
