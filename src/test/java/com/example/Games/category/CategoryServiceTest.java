package com.example.Games.category;

import com.example.Games.category.dto.CategoryRequest;
import com.example.Games.category.dto.CategoryResponse;
import com.example.Games.config.common.service.UserContextService;
import com.example.Games.config.exception.auth.UserNotFoundException;
import com.example.Games.config.exception.category.CategoryAlreadyExistsException;
import com.example.Games.config.exception.category.CategoryInUseException;
import com.example.Games.config.exception.category.CategoryNotFoundException;
import com.example.Games.config.exception.category.UnauthorizedCategoryAccessException;
import com.example.Games.game.Game;
import com.example.Games.user.auth.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CategoryService Tests")
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private CategoryMapStruct categoryMapStruct;
    @Mock
    private UserContextService userContextService;
    @InjectMocks
    private CategoryService categoryService;


    private User currentUser;
    private Category category;
    private Category category2;
    private CategoryRequest request;
    private CategoryResponse response;
    private List<Category> categories;
    private List<CategoryResponse> responses;
    private EntityManager entityManager;

    @BeforeEach
    void setUp() {
        LocalDateTime now = LocalDateTime.now();
        currentUser = User.builder().id(1L).username("testuser").build();

        category = Category.builder().id(10L).name("Action").createdBy(currentUser).build();
        category2 = Category.builder().id(20L).name("Action").createdBy(currentUser).build();

        request = new CategoryRequest("Action");
        response = new CategoryResponse(10L, "Action", "testuser", 1L,now,now);
        CategoryResponse response2 = new CategoryResponse(20L, "Adventure","testuser",1L,now,now);

        categories = List.of(category,category2);
        responses = List.of(response, response2);

    }

    @Test
    @DisplayName("Should create category when name is unique")
    void shouldCreateCategory() {
        when(userContextService.getAuthorizedUser()).thenReturn(currentUser);
        when(categoryRepository.findByName("Action")).thenReturn(Optional.empty());
        when(categoryMapStruct.toEntity(request)).thenReturn(category);
        when(categoryRepository.save(category)).thenReturn(category);
        when(categoryMapStruct.toDto(category)).thenReturn(response);

        CategoryResponse result = categoryService.createCategory(request);

        assertThat(result).isEqualTo(response);
        verify(categoryRepository).save(category);
    }

    @Test
    @DisplayName("Should throw when creating category with existing name")
    void shouldThrowWhenCategoryAlreadyExists() {
        when(userContextService.getAuthorizedUser()).thenReturn(currentUser);
        when(categoryRepository.findByName("Action")).thenReturn(Optional.of(category));

        assertThatThrownBy(() -> categoryService.createCategory(request))
                .isInstanceOf(CategoryAlreadyExistsException.class);
    }

    @Test
    @DisplayName("Should return all categories")
    void shouldReturnAllCategories() {
        when(categoryRepository.findAll()).thenReturn(categories);
        when(categoryMapStruct.toDtoList(categories)).thenReturn(responses);

        List<CategoryResponse> results = categoryService.getAllCategories();

        assertThat(results).containsAll(responses);
    }


    @Test
    @DisplayName("Should return category by ID")
    void shouldReturnCategoryById() {
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));
        when(categoryMapStruct.toDto(category)).thenReturn(response);

        CategoryResponse result = categoryService.getCategoryById(10L);

        assertThat(result).isEqualTo(response);
    }

    @Test
    @DisplayName("Should throw when category not found by ID")
    void shouldThrowWhenCategoryNotFound() {
        when(categoryRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.getCategoryById(10L))
                .isInstanceOf(CategoryNotFoundException.class);
    }

    @Test
    @DisplayName("Should update category when user is owner and name is unique")
    void shouldUpdateCategory() {
        when(userContextService.getAuthorizedUser()).thenReturn(currentUser);
        when(categoryRepository.findByIdWithCreator(10L)).thenReturn(Optional.of(category));
        when(categoryRepository.findByName("Action")).thenReturn(Optional.of(category)); // same ID
        when(categoryRepository.save(category)).thenReturn(category);
        when(categoryMapStruct.toDto(category)).thenReturn(response);

        CategoryResponse result = categoryService.updateCategory(10L, request);

        assertThat(result).isEqualTo(response);
    }

    @Test
    @DisplayName("Should throw when updating non-existing category")
    void shouldThrowWhenUpdatingNonExistingCategory() {
        when(userContextService.getAuthorizedUser()).thenReturn(currentUser);
        when(categoryRepository.findByIdWithCreator(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.updateCategory(10L, request))
                .isInstanceOf(CategoryNotFoundException.class);
    }

    @Test
    @DisplayName("Should throw when updating category with duplicate name")
    void shouldThrowWhenUpdatingCategoryWithDuplicateName() {
        when(userContextService.getAuthorizedUser()).thenReturn(currentUser);
        when(categoryRepository.findByIdWithCreator(10L)).thenReturn(Optional.of(category));

        when(categoryRepository.findByName("Adventure")).thenReturn(Optional.of(category2));

        CategoryRequest updateRequest = new CategoryRequest("Adventure");

        assertThatThrownBy(() -> categoryService.updateCategory(10L, updateRequest))
                .isInstanceOf(CategoryAlreadyExistsException.class);
    }


    @Test
    @DisplayName("Should throw when updating not owned category")
    void shouldThrowWhenNotOwner() {
        when(userContextService.getAuthorizedUser()).thenReturn(currentUser);
        User otherUser = User.builder().id(2L).username("other").build();
        Category otherCategory = Category.builder().id(10L).name("Action").createdBy(otherUser).build();
        when(categoryRepository.findByIdWithCreator(10L)).thenReturn(Optional.of(otherCategory));

        assertThatThrownBy(() -> categoryService.updateCategory(10L, request))
                .isInstanceOf(UnauthorizedCategoryAccessException.class);
    }

    @Test
    @DisplayName("Should delete category when user is owner and no games exist")
    void shouldDeleteCategory() {
        when(userContextService.getAuthorizedUser()).thenReturn(currentUser);
        when(categoryRepository.findByIdWithCreator(10L)).thenReturn(Optional.of(category));
        when(categoryRepository.hasGames(10L)).thenReturn(false);

        categoryService.deleteCategory(10L);

        verify(categoryRepository).deleteById(10L);
    }

    @Test
    @DisplayName("Should throw when deleting non-existing category")
    void shouldThrowWhenDeletingNonExistingCategory() {
        when(userContextService.getAuthorizedUser()).thenReturn(currentUser);
        when(categoryRepository.findByIdWithCreator(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.deleteCategory(10L))
                .isInstanceOf(CategoryNotFoundException.class);
    }


    @Test
    @DisplayName("Should throw when deleting category with games")
    void shouldThrowWhenCategoryHasGames() {
        when(userContextService.getAuthorizedUser()).thenReturn(currentUser);
        when(categoryRepository.findByIdWithCreator(10L)).thenReturn(Optional.of(category));
        when(categoryRepository.hasGames(10L)).thenReturn(true);
        when(categoryRepository.countGamesByCategoryId(10L)).thenReturn(3);

        assertThatThrownBy(() -> categoryService.deleteCategory(10L))
                .isInstanceOf(CategoryInUseException.class);
    }

    @Test
    @DisplayName("Should throw when deleting not owned category")
    void shouldThrowWhenDeletingNotOwnedCategory() {
        when(userContextService.getAuthorizedUser()).thenReturn(currentUser);
        User otherUser = User.builder().id(2L).username("other").build();
        Category otherCategory = Category.builder().id(10L).name("Action").createdBy(otherUser).build();

        when(categoryRepository.findByIdWithCreator(10L)).thenReturn(Optional.of(otherCategory));

        assertThatThrownBy(() -> categoryService.deleteCategory(10L))
                .isInstanceOf(UnauthorizedCategoryAccessException.class);
    }
}
