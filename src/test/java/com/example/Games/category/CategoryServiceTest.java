package com.example.Games.category;

import com.example.Games.category.dto.CategoryRequest;
import com.example.Games.category.dto.CategoryResponse;
import com.example.Games.config.exception.category.CategoryAlreadyExistsException;
import com.example.Games.config.exception.category.CategoryInUseException;
import com.example.Games.config.exception.category.CategoryNotFoundException;
import com.example.Games.game.Game;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
@DisplayName("CategoryService Unit Tests")
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private CategoryMapStruct categoryMapStruct;

    @InjectMocks
    private CategoryService categoryService;

    private Category actionCategory;
    private CategoryRequest categoryRequest;
    private CategoryResponse categoryResponse;

    @BeforeEach
    void setUp() {
        LocalDateTime now = LocalDateTime.now();
        
        actionCategory = Category.builder()
                .id(1L)
                .name("Action")
                .games(new ArrayList<>())
                .createdAt(now)
                .updatedAt(now)
                .build();

        categoryRequest = new CategoryRequest("Action");
        
        categoryResponse = new CategoryResponse(
                1L, 
                "Action", 
                0, 
                now, 
                now
        );
    }

    @Nested
    @DisplayName("createCategory() Tests")
    class CreateCategoryTests {

        @Test
        @DisplayName("Should create category successfully")
        void shouldCreateCategorySuccessfully() {
            // Given
            when(categoryRepository.findByName("Action")).thenReturn(Optional.empty());
            when(categoryMapStruct.toEntity(categoryRequest)).thenReturn(actionCategory);
            when(categoryRepository.save(actionCategory)).thenReturn(actionCategory);
            when(categoryMapStruct.toDto(actionCategory)).thenReturn(categoryResponse);

            // When
            CategoryResponse result = categoryService.createCategory(categoryRequest);

            // Then
            assertThat(result).isEqualTo(categoryResponse);
            
            // Verify interactions
            verify(categoryRepository).findByName("Action");
            verify(categoryMapStruct).toEntity(categoryRequest);
            verify(categoryRepository).save(actionCategory);
            verify(categoryMapStruct).toDto(actionCategory);
        }

        @Test
        @DisplayName("Should throw exception when category name already exists")
        void shouldThrowExceptionWhenCategoryNameAlreadyExists() {
            // Given
            when(categoryRepository.findByName("Action")).thenReturn(Optional.of(actionCategory));

            // When & Then
            assertThatThrownBy(() -> categoryService.createCategory(categoryRequest))
                    .isInstanceOf(CategoryAlreadyExistsException.class)
                    .hasMessageContaining("Category already exists with name: 'Action'");

            // Verify interactions
            verify(categoryRepository).findByName("Action");
            verify(categoryMapStruct, never()).toEntity(any());
            verify(categoryRepository, never()).save(any());
            verify(categoryMapStruct, never()).toDto(any());
        }

        @Test
        @DisplayName("Should handle case insensitive duplicate check")
        void shouldHandleCaseInsensitiveDuplicateCheck() {
            // Given
            CategoryRequest upperCaseRequest = new CategoryRequest("ACTION");
            when(categoryRepository.findByName("ACTION")).thenReturn(Optional.of(actionCategory));

            // When & Then
            assertThatThrownBy(() -> categoryService.createCategory(upperCaseRequest))
                    .isInstanceOf(CategoryAlreadyExistsException.class);

            verify(categoryRepository).findByName("ACTION");
        }
    }

    @Nested
    @DisplayName("getAllCategories() Tests")
    class GetAllCategoriesTests {

        @Test
        @DisplayName("Should return all categories")
        void shouldReturnAllCategories() {
            // Given
            Category rpgCategory = Category.builder()
                    .id(2L)
                    .name("RPG")
                    .games(new ArrayList<>())
                    .build();

            List<Category> categories = Arrays.asList(actionCategory, rpgCategory);
            List<CategoryResponse> expectedResponses = Arrays.asList(
                    categoryResponse,
                    new CategoryResponse(2L, "RPG", 0, LocalDateTime.now(), LocalDateTime.now())
            );

            when(categoryRepository.findAll()).thenReturn(categories);
            when(categoryMapStruct.toDtoList(categories)).thenReturn(expectedResponses);

            // When
            List<CategoryResponse> result = categoryService.getAllCategories();

            // Then
            assertThat(result).hasSize(2);
            assertThat(result).isEqualTo(expectedResponses);

            verify(categoryRepository).findAll();
            verify(categoryMapStruct).toDtoList(categories);
        }

        @Test
        @DisplayName("Should return empty list when no categories exist")
        void shouldReturnEmptyListWhenNoCategoriesExist() {
            // Given
            List<Category> emptyList = new ArrayList<>();
            List<CategoryResponse> emptyResponseList = new ArrayList<>();

            when(categoryRepository.findAll()).thenReturn(emptyList);
            when(categoryMapStruct.toDtoList(emptyList)).thenReturn(emptyResponseList);

            // When
            List<CategoryResponse> result = categoryService.getAllCategories();

            // Then
            assertThat(result).isEmpty();

            verify(categoryRepository).findAll();
            verify(categoryMapStruct).toDtoList(emptyList);
        }
    }

    @Nested
    @DisplayName("getCategoryById() Tests")
    class GetCategoryByIdTests {

        @Test
        @DisplayName("Should return category when found by ID")
        void shouldReturnCategoryWhenFoundById() {
            // Given
            Long categoryId = 1L;
            when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(actionCategory));
            when(categoryMapStruct.toDto(actionCategory)).thenReturn(categoryResponse);

            // When
            CategoryResponse result = categoryService.getCategoryById(categoryId);

            // Then
            assertThat(result).isEqualTo(categoryResponse);

            verify(categoryRepository).findById(categoryId);
            verify(categoryMapStruct).toDto(actionCategory);
        }

        @Test
        @DisplayName("Should throw exception when category not found by ID")
        void shouldThrowExceptionWhenCategoryNotFoundById() {
            // Given
            Long categoryId = 999L;
            when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> categoryService.getCategoryById(categoryId))
                    .isInstanceOf(CategoryNotFoundException.class)
                    .hasMessageContaining("Category not found with ID: 999");

            verify(categoryRepository).findById(categoryId);
            verify(categoryMapStruct, never()).toDto(any());
        }
    }

    @Nested
    @DisplayName("updateCategory() Tests")
    class UpdateCategoryTests {

        @Test
        @DisplayName("Should update category successfully with new name")
        void shouldUpdateCategorySuccessfullyWithNewName() {
            // Given
            Long categoryId = 1L;
            CategoryRequest updateRequest = new CategoryRequest("Adventure");
            Category updatedCategory = Category.builder()
                    .id(1L)
                    .name("Adventure")
                    .games(new ArrayList<>())
                    .build();
            CategoryResponse updatedResponse = new CategoryResponse(
                    1L, "Adventure", 0, LocalDateTime.now(), LocalDateTime.now()
            );

            when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(actionCategory));
            when(categoryRepository.findByName("Adventure")).thenReturn(Optional.empty());
            when(categoryRepository.save(actionCategory)).thenReturn(updatedCategory);
            when(categoryMapStruct.toDto(updatedCategory)).thenReturn(updatedResponse);

            // When
            CategoryResponse result = categoryService.updateCategory(categoryId, updateRequest);

            // Then
            assertThat(result).isEqualTo(updatedResponse);

            verify(categoryRepository).findById(categoryId);
            verify(categoryRepository).findByName("Adventure");
            verify(categoryRepository).save(actionCategory);
            verify(categoryMapStruct).toDto(updatedCategory);
        }

        @Test
        @DisplayName("Should update category successfully with same name (case insensitive)")
        void shouldUpdateCategorySuccessfullyWithSameName() {
            // Given
            Long categoryId = 1L;
            CategoryRequest updateRequest = new CategoryRequest("action"); // different case
            
            // Mock isNameEqual to return true for case insensitive comparison
            actionCategory = spy(actionCategory);
            when(actionCategory.isNameEqual("action")).thenReturn(true);
            
            when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(actionCategory));
            when(categoryRepository.save(actionCategory)).thenReturn(actionCategory);
            when(categoryMapStruct.toDto(actionCategory)).thenReturn(categoryResponse);

            // When
            CategoryResponse result = categoryService.updateCategory(categoryId, updateRequest);

            // Then
            assertThat(result).isEqualTo(categoryResponse);

            verify(categoryRepository).findById(categoryId);
            verify(categoryRepository, never()).findByName(anyString()); // Should not check for duplicates
            verify(categoryRepository).save(actionCategory);
        }

        @Test
        @DisplayName("Should throw exception when category not found for update")
        void shouldThrowExceptionWhenCategoryNotFoundForUpdate() {
            // Given
            Long categoryId = 999L;
            CategoryRequest updateRequest = new CategoryRequest("Adventure");
            when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> categoryService.updateCategory(categoryId, updateRequest))
                    .isInstanceOf(CategoryNotFoundException.class)
                    .hasMessageContaining("Category not found with ID: 999");

            verify(categoryRepository).findById(categoryId);
            verify(categoryRepository, never()).findByName(anyString());
            verify(categoryRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when new name already exists")
        void shouldThrowExceptionWhenNewNameAlreadyExists() {
            // Given
            Long categoryId = 1L;
            CategoryRequest updateRequest = new CategoryRequest("RPG");
            Category existingRpgCategory = Category.builder()
                    .id(2L)
                    .name("RPG")
                    .build();

            when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(actionCategory));
            when(categoryRepository.findByName("RPG")).thenReturn(Optional.of(existingRpgCategory));

            // When & Then
            assertThatThrownBy(() -> categoryService.updateCategory(categoryId, updateRequest))
                    .isInstanceOf(CategoryAlreadyExistsException.class)
                    .hasMessageContaining("Category already exists with name: 'RPG'");

            verify(categoryRepository).findById(categoryId);
            verify(categoryRepository).findByName("RPG");
            verify(categoryRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("deleteCategory() Tests")
    class DeleteCategoryTests {

        @Test
        @DisplayName("Should delete category successfully when no games associated")
        void shouldDeleteCategorySuccessfullyWhenNoGamesAssociated() {
            // Given
            Long categoryId = 1L;
            when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(actionCategory));

            // When
            categoryService.deleteCategory(categoryId);

            // Then
            verify(categoryRepository).findById(categoryId);
            verify(categoryRepository).deleteById(categoryId);
        }

        @Test
        @DisplayName("Should throw exception when category not found for deletion")
        void shouldThrowExceptionWhenCategoryNotFoundForDeletion() {
            // Given
            Long categoryId = 999L;
            when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> categoryService.deleteCategory(categoryId))
                    .isInstanceOf(CategoryNotFoundException.class)
                    .hasMessageContaining("Category not found with ID: 999");

            verify(categoryRepository).findById(categoryId);
            verify(categoryRepository, never()).deleteById(any());
        }

        @Test
        @DisplayName("Should throw exception when category has associated games")
        void shouldThrowExceptionWhenCategoryHasAssociatedGames() {
            // Given
            Long categoryId = 1L;
            Game game1 = Game.builder().id(1L).title("Game 1").build();
            Game game2 = Game.builder().id(2L).title("Game 2").build();
            actionCategory.getGames().addAll(Arrays.asList(game1, game2));

            when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(actionCategory));

            // When & Then
            assertThatThrownBy(() -> categoryService.deleteCategory(categoryId))
                    .isInstanceOf(CategoryInUseException.class)
                    .hasMessageContaining("Cannot delete category 'Action' as it has 2 associated games");

            verify(categoryRepository).findById(categoryId);
            verify(categoryRepository, never()).deleteById(any());
        }
    }

    @Nested
    @DisplayName("Edge Cases and Error Handling Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle repository exceptions gracefully")
        void shouldHandleRepositoryExceptionsGracefully() {
            // Given
            when(categoryRepository.findByName("Action")).thenThrow(new RuntimeException("Database error"));

            // When & Then
            assertThatThrownBy(() -> categoryService.createCategory(categoryRequest))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Database error");
        }

        @Test
        @DisplayName("Should handle mapper exceptions gracefully")
        void shouldHandleMapperExceptionsGracefully() {
            // Given
            when(categoryRepository.findByName("Action")).thenReturn(Optional.empty());
            when(categoryMapStruct.toEntity(categoryRequest)).thenThrow(new RuntimeException("Mapping error"));

            // When & Then
            assertThatThrownBy(() -> categoryService.createCategory(categoryRequest))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Mapping error");
        }
    }
}
