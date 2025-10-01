package com.example.Games.category;

import com.example.Games.category.dto.CategoryRequest;
import com.example.Games.category.dto.CategoryResponse;
import com.example.Games.config.common.dto.ApiResponse;
import com.example.Games.config.common.mappers.ResponseMapStruct;
import com.example.Games.config.exception.category.CategoryAlreadyExistsException;
import com.example.Games.config.exception.category.CategoryInUseException;
import com.example.Games.config.exception.category.CategoryNotFoundException;
import com.example.Games.config.exception.category.UnauthorizedCategoryAccessException;
import com.example.Games.config.security.JwtAuthenticationFilter;
import com.example.Games.config.security.SecurityConfig;
import com.example.Games.config.security.TokenBlacklistService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = CategoryController.class, excludeFilters = {
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                SecurityConfig.class,
                JwtAuthenticationFilter.class,
                TokenBlacklistService.class
        })
})
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("CategoryController Tests")
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CategoryService categoryService;

    @MockitoBean
    private ResponseMapStruct responseMapper;

    private CategoryRequest categoryRequest;
    private CategoryResponse categoryResponse;
    private CategoryResponse categoryResponse2;
    private List<CategoryResponse> categoryList;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.now();

        categoryRequest = new CategoryRequest("Action");
        
        categoryResponse = new CategoryResponse(
                1L, 
                "Action", 
                "developer1",
                10L,
                now.minusDays(5), 
                now.minusDays(5)
        );
        
        categoryResponse2 = new CategoryResponse(
                2L, 
                "RPG", 
                "developer2",
                11L,
                now.minusDays(3), 
                now.minusDays(2)
        );
        
        categoryList = Arrays.asList(categoryResponse, categoryResponse2);
    }



    @Test
    @DisplayName("Should create category successfully with valid data")
    void shouldCreateCategorySuccessfully() throws Exception {
        // Given
        when(categoryService.createCategory(any(CategoryRequest.class))).thenReturn(categoryResponse);
        when(responseMapper.toSuccessResponse(eq("Category created successfully"), eq(categoryResponse)))
                .thenReturn(new ApiResponse<>("Category created successfully", categoryResponse, System.currentTimeMillis()));

        // When & Then
        mockMvc.perform(post("/api/v1/categories")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(categoryRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Category created successfully"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("Action"))
                .andExpect(jsonPath("$.data.createdByUsername").value("developer1"))
                .andExpect(jsonPath("$.data.createdById").value(10))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(categoryService).createCategory(any(CategoryRequest.class));
        verify(responseMapper).toSuccessResponse(eq("Category created successfully"), eq(categoryResponse));
    }

    @Test
    @DisplayName("Should return 409 when creating duplicate category")
    void shouldReturn409WhenCreatingDuplicateCategory() throws Exception {
        // Given
        when(categoryService.createCategory(any(CategoryRequest.class)))
                .thenThrow(new CategoryAlreadyExistsException("Category with name 'Action' already exists"));
        when(responseMapper.toErrorResponse(anyString()))
                .thenReturn(new ApiResponse<>("Category with name 'Action' already exists", null, System.currentTimeMillis()));

        // When & Then
        mockMvc.perform(post("/api/v1/categories")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(categoryRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Category with name 'Action' already exists"))
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(categoryService).createCategory(any(CategoryRequest.class));
    }

    @Test
    @DisplayName("Should return 400 when category name is null")
    void shouldReturn400WhenCategoryNameIsNull() throws Exception {
        // Given
        String jsonWithNull = "{\"name\": null}";

        // When & Then
        mockMvc.perform(post("/api/v1/categories")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonWithNull))
                .andExpect(status().isBadRequest());

        verify(categoryService, never()).createCategory(any());
    }

    @Test
    @DisplayName("Should get all categories successfully")
    void shouldGetAllCategoriesSuccessfully() throws Exception {
        // Given
        when(categoryService.getAllCategories()).thenReturn(categoryList);
        when(responseMapper.toSuccessResponse(categoryList))
                .thenReturn(new ApiResponse<>(null, categoryList, System.currentTimeMillis()));

        // When & Then
        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].name").value("Action"))
                .andExpect(jsonPath("$.data[1].id").value(2))
                .andExpect(jsonPath("$.data[1].name").value("RPG"));

        verify(categoryService).getAllCategories();
        verify(responseMapper).toSuccessResponse(categoryList);
    }

    @Test
    @DisplayName("Should return empty list when no categories exist")
    void shouldReturnEmptyListWhenNoCategoriesExist() throws Exception {
        // Given
        List<CategoryResponse> emptyList = Collections.emptyList();
        when(categoryService.getAllCategories()).thenReturn(emptyList);
        when(responseMapper.toSuccessResponse(emptyList))
                .thenReturn(new ApiResponse<>(null, emptyList, System.currentTimeMillis()));

        // When & Then
        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));

        verify(categoryService).getAllCategories();
    }

    @Test
    @DisplayName("Should get category by ID successfully")
    void shouldGetCategoryByIdSuccessfully() throws Exception {
        // Given
        Long categoryId = 1L;
        when(categoryService.getCategoryById(categoryId)).thenReturn(categoryResponse);
        when(responseMapper.toSuccessResponse(categoryResponse))
                .thenReturn(new ApiResponse<>(null, categoryResponse, System.currentTimeMillis()));

        // When & Then
        mockMvc.perform(get("/api/v1/categories/{id}", categoryId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("Action"))
                .andExpect(jsonPath("$.data.createdByUsername").value("developer1"));

        verify(categoryService).getCategoryById(categoryId);
        verify(responseMapper).toSuccessResponse(categoryResponse);
    }

    @Test
    @DisplayName("Should return 404 when category not found by ID")
    void shouldReturn404WhenCategoryNotFoundById() throws Exception {
        // Given
        Long categoryId = 999L;
        when(categoryService.getCategoryById(categoryId))
                .thenThrow(new CategoryNotFoundException("Category with ID 999 not found"));
        when(responseMapper.toErrorResponse(anyString()))
                .thenReturn(new ApiResponse<>("Category with ID 999 not found", null, System.currentTimeMillis()));

        // When & Then
        mockMvc.perform(get("/api/v1/categories/{id}", categoryId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Category with ID 999 not found"));

        verify(categoryService).getCategoryById(categoryId);
    }

    @Test
    @DisplayName("Should update category successfully")
    void shouldUpdateCategorySuccessfully() throws Exception {
        // Given
        Long categoryId = 1L;
        CategoryRequest updateRequest = new CategoryRequest("Updated Action");
        CategoryResponse updatedResponse = new CategoryResponse(
                categoryId,
                "Updated Action",
                "developer1",
                10L,
                now.minusDays(5),
                now
        );

        when(categoryService.updateCategory(eq(categoryId), any(CategoryRequest.class)))
                .thenReturn(updatedResponse);
        when(responseMapper.toSuccessResponse(eq("Category updated successfully"), eq(updatedResponse)))
                .thenReturn(new ApiResponse<>("Category updated successfully", updatedResponse, System.currentTimeMillis()));

        // When & Then
        mockMvc.perform(put("/api/v1/categories/{id}", categoryId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Category updated successfully"))
                .andExpect(jsonPath("$.data.name").value("Updated Action"));

        verify(categoryService).updateCategory(eq(categoryId), any(CategoryRequest.class));
    }

    @Test
    @DisplayName("Should return 404 when updating non-existent category")
    void shouldReturn404WhenUpdatingNonExistentCategory() throws Exception {
        // Given
        Long categoryId = 999L;
        CategoryRequest updateRequest = new CategoryRequest("Updated Action");

        when(categoryService.updateCategory(eq(categoryId), any(CategoryRequest.class)))
                .thenThrow(new CategoryNotFoundException("Category with ID 999 not found"));
        when(responseMapper.toErrorResponse(anyString()))
                .thenReturn(new ApiResponse<>("Category with ID 999 not found", null, System.currentTimeMillis()));

        // When & Then
        mockMvc.perform(put("/api/v1/categories/{id}", categoryId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound());

        verify(categoryService).updateCategory(eq(categoryId), any(CategoryRequest.class));
    }

    @Test
    @DisplayName("Should return 409 when updating to duplicate name")
    void shouldReturn409WhenUpdatingToDuplicateName() throws Exception {
        // Given
        Long categoryId = 1L;
        CategoryRequest updateRequest = new CategoryRequest("RPG");

        when(categoryService.updateCategory(eq(categoryId), any(CategoryRequest.class)))
                .thenThrow(new CategoryAlreadyExistsException("Category with name 'RPG' already exists"));
        when(responseMapper.toErrorResponse(anyString()))
                .thenReturn(new ApiResponse<>("Category with name 'RPG' already exists", null, System.currentTimeMillis()));

        // When & Then
        mockMvc.perform(put("/api/v1/categories/{id}", categoryId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isConflict());

        verify(categoryService).updateCategory(eq(categoryId), any(CategoryRequest.class));
    }

    @Test
    @DisplayName("Should return 403 when updating category not owned by user")
    void shouldReturn403WhenUpdatingCategoryNotOwned() throws Exception {
        // Given
        Long categoryId = 1L;
        CategoryRequest updateRequest = new CategoryRequest("Updated Action");

        when(categoryService.updateCategory(eq(categoryId), any(CategoryRequest.class)))
                .thenThrow(new UnauthorizedCategoryAccessException("You don't have permission to update this category"));
        when(responseMapper.toErrorResponse(anyString()))
                .thenReturn(new ApiResponse<>("You don't have permission to update this category", null, System.currentTimeMillis()));

        // When & Then
        mockMvc.perform(put("/api/v1/categories/{id}", categoryId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isForbidden());

        verify(categoryService).updateCategory(eq(categoryId), any(CategoryRequest.class));
    }

    @Test
    @DisplayName("Should delete category successfully")
    void shouldDeleteCategorySuccessfully() throws Exception {
        // Given
        Long categoryId = 1L;
        doNothing().when(categoryService).deleteCategory(categoryId);
        when(responseMapper.toSuccessResponse(eq("Category deleted successfully")))
                .thenReturn(new ApiResponse<>("Category deleted successfully", null, System.currentTimeMillis()));

        // When & Then
        mockMvc.perform(delete("/api/v1/categories/{id}", categoryId)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Category deleted successfully"))
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(categoryService).deleteCategory(categoryId);
        verify(responseMapper).toSuccessResponse("Category deleted successfully");
    }

    @Test
    @DisplayName("Should return 404 when deleting non-existent category")
    void shouldReturn404WhenDeletingNonExistentCategory() throws Exception {
        // Given
        Long categoryId = 999L;
        doThrow(new CategoryNotFoundException("Category with ID 999 not found"))
                .when(categoryService).deleteCategory(categoryId);
        when(responseMapper.toErrorResponse(anyString()))
                .thenReturn(new ApiResponse<>("Category with ID 999 not found", null, System.currentTimeMillis()));

        // When & Then
        mockMvc.perform(delete("/api/v1/categories/{id}", categoryId)
                        .with(csrf()))
                .andExpect(status().isNotFound());

        verify(categoryService).deleteCategory(categoryId);
    }

    @Test
    @DisplayName("Should return 409 when deleting category with games")
    void shouldReturn409WhenDeletingCategoryWithGames() throws Exception {
        // Given
        Long categoryId = 1L;
        doThrow(new CategoryInUseException("Cannot delete category 'Action' because it has 5 associated games"))
                .when(categoryService).deleteCategory(categoryId);
        when(responseMapper.toErrorResponse(anyString()))
                .thenReturn(new ApiResponse<>("Cannot delete category 'Action' because it has 5 associated games", null, System.currentTimeMillis()));

        // When & Then
        mockMvc.perform(delete("/api/v1/categories/{id}", categoryId)
                        .with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Cannot delete category 'Action' because it has 5 associated games"));

        verify(categoryService).deleteCategory(categoryId);
    }

    @Test
    @DisplayName("Should return 403 when deleting category not owned by user")
    void shouldReturn403WhenDeletingCategoryNotOwned() throws Exception {
        // Given
        Long categoryId = 1L;
        doThrow(new UnauthorizedCategoryAccessException("You don't have permission to delete this category"))
                .when(categoryService).deleteCategory(categoryId);
        when(responseMapper.toErrorResponse(anyString()))
                .thenReturn(new ApiResponse<>("You don't have permission to delete this category", null, System.currentTimeMillis()));

        // When & Then
        mockMvc.perform(delete("/api/v1/categories/{id}", categoryId)
                        .with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You don't have permission to delete this category"));

        verify(categoryService).deleteCategory(categoryId);
    }
}
