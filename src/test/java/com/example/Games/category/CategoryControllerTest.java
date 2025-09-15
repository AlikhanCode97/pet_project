package com.example.Games.category;

import com.example.Games.category.dto.CategoryRequest;
import com.example.Games.category.dto.CategoryResponse;
import com.example.Games.config.common.mappers.ResponseMapStruct;
import com.example.Games.config.exception.category.CategoryAlreadyExistsException;
import com.example.Games.config.exception.category.CategoryInUseException;
import com.example.Games.config.exception.category.CategoryNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CategoryController.class)
@ActiveProfiles("test")
@DisplayName("CategoryController Integration Tests")
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CategoryService categoryService;

    @MockBean
    private ResponseMapStruct responseMapper;

    private CategoryRequest categoryRequest;
    private CategoryResponse categoryResponse;
    private List<CategoryResponse> categoryList;

    @BeforeEach
    void setUp() {
        LocalDateTime now = LocalDateTime.now();
        
        categoryRequest = new CategoryRequest("Action");
        
        categoryResponse = new CategoryResponse(
                1L, 
                "Action", 
                0, 
                now, 
                now
        );

        CategoryResponse rpgResponse = new CategoryResponse(
                2L, 
                "RPG", 
                3, 
                now, 
                now
        );

        categoryList = Arrays.asList(categoryResponse, rpgResponse);
    }

    @Nested
    @DisplayName("POST /api/v1/categories - Create Category Tests")
    class CreateCategoryTests {

        @Test
        @WithMockUser(authorities = "ROLE_DEVELOPER")
        @DisplayName("Should create category successfully with valid request")
        void shouldCreateCategorySuccessfullyWithValidRequest() throws Exception {
            // Given
            when(categoryService.createCategory(any(CategoryRequest.class))).thenReturn(categoryResponse);
            when(responseMapper.toSuccessResponse(anyString(), any())).thenReturn(null); // Mock API response

            // When & Then
            mockMvc.perform(post("/api/v1/categories")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(categoryRequest)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON));

            verify(categoryService).createCategory(any(CategoryRequest.class));
            verify(responseMapper).toSuccessResponse(eq("Category created successfully"), eq(categoryResponse));
        }

        @Test
        @WithMockUser(authorities = "ROLE_USER")
        @DisplayName("Should return 403 when user lacks DEVELOPER authority")
        void shouldReturn403WhenUserLacksDeveloperAuthority() throws Exception {
            // When & Then
            mockMvc.perform(post("/api/v1/categories")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(categoryRequest)))
                    .andDo(print())
                    .andExpect(status().isForbidden());

            verify(categoryService, never()).createCategory(any());
        }

        @Test
        @DisplayName("Should return 401 when user is not authenticated")
        void shouldReturn401WhenUserIsNotAuthenticated() throws Exception {
            // When & Then
            mockMvc.perform(post("/api/v1/categories")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(categoryRequest)))
                    .andDo(print())
                    .andExpect(status().isUnauthorized());

            verify(categoryService, never()).createCategory(any());
        }

        @Test
        @WithMockUser(authorities = "ROLE_DEVELOPER")
        @DisplayName("Should return 400 when category name is blank")
        void shouldReturn400WhenCategoryNameIsBlank() throws Exception {
            // Given
            CategoryRequest invalidRequest = new CategoryRequest("");

            // When & Then
            mockMvc.perform(post("/api/v1/categories")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());

            verify(categoryService, never()).createCategory(any());
        }

        @Test
        @WithMockUser(authorities = "ROLE_DEVELOPER")
        @DisplayName("Should return 400 when category name is too long")
        void shouldReturn400WhenCategoryNameIsTooLong() throws Exception {
            // Given
            String longName = "a".repeat(101);
            CategoryRequest invalidRequest = new CategoryRequest(longName);

            // When & Then
            mockMvc.perform(post("/api/v1/categories")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());

            verify(categoryService, never()).createCategory(any());
        }

        @Test
        @WithMockUser(authorities = "ROLE_DEVELOPER")
        @DisplayName("Should return 409 when category already exists")
        void shouldReturn409WhenCategoryAlreadyExists() throws Exception {
            // Given
            when(categoryService.createCategory(any(CategoryRequest.class)))
                    .thenThrow(CategoryAlreadyExistsException.withName("Action"));

            // When & Then
            mockMvc.perform(post("/api/v1/categories")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(categoryRequest)))
                    .andDo(print())
                    .andExpect(status().isConflict());

            verify(categoryService).createCategory(any(CategoryRequest.class));
        }

        @Test
        @WithMockUser(authorities = "ROLE_DEVELOPER")
        @DisplayName("Should return 400 when request body is malformed")
        void shouldReturn400WhenRequestBodyIsMalformed() throws Exception {
            // When & Then
            mockMvc.perform(post("/api/v1/categories")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{invalid json}"))
                    .andDo(print())
                    .andExpect(status().isBadRequest());

            verify(categoryService, never()).createCategory(any());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/categories - Get All Categories Tests")
    class GetAllCategoriesTests {

        @Test
        @WithMockUser
        @DisplayName("Should return all categories successfully")
        void shouldReturnAllCategoriesSuccessfully() throws Exception {
            // Given
            when(categoryService.getAllCategories()).thenReturn(categoryList);
            when(responseMapper.toSuccessResponse(categoryList)).thenReturn(null);

            // When & Then
            mockMvc.perform(get("/api/v1/categories")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON));

            verify(categoryService).getAllCategories();
            verify(responseMapper).toSuccessResponse(categoryList);
        }

        @Test
        @DisplayName("Should allow unauthenticated access to get all categories")
        void shouldAllowUnauthenticatedAccessToGetAllCategories() throws Exception {
            // Given
            when(categoryService.getAllCategories()).thenReturn(categoryList);
            when(responseMapper.toSuccessResponse(categoryList)).thenReturn(null);

            // When & Then
            mockMvc.perform(get("/api/v1/categories")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk());

            verify(categoryService).getAllCategories();
        }
    }

    @Nested
    @DisplayName("GET /api/v1/categories/{id} - Get Category By ID Tests")
    class GetCategoryByIdTests {

        @Test
        @WithMockUser
        @DisplayName("Should return category when found by ID")
        void shouldReturnCategoryWhenFoundById() throws Exception {
            // Given
            Long categoryId = 1L;
            when(categoryService.getCategoryById(categoryId)).thenReturn(categoryResponse);
            when(responseMapper.toSuccessResponse(categoryResponse)).thenReturn(null);

            // When & Then
            mockMvc.perform(get("/api/v1/categories/{id}", categoryId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON));

            verify(categoryService).getCategoryById(categoryId);
            verify(responseMapper).toSuccessResponse(categoryResponse);
        }

        @Test
        @WithMockUser
        @DisplayName("Should return 404 when category not found by ID")
        void shouldReturn404WhenCategoryNotFoundById() throws Exception {
            // Given
            Long categoryId = 999L;
            when(categoryService.getCategoryById(categoryId))
                    .thenThrow(CategoryNotFoundException.byId(categoryId));

            // When & Then
            mockMvc.perform(get("/api/v1/categories/{id}", categoryId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isNotFound());

            verify(categoryService).getCategoryById(categoryId);
        }

        @Test
        @WithMockUser
        @DisplayName("Should return 400 for invalid ID format")
        void shouldReturn400ForInvalidIdFormat() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/v1/categories/{id}", "invalid")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isBadRequest());

            verify(categoryService, never()).getCategoryById(any());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/categories/{id} - Update Category Tests")
    class UpdateCategoryTests {

        @Test
        @WithMockUser(authorities = "ROLE_DEVELOPER")
        @DisplayName("Should update category successfully")
        void shouldUpdateCategorySuccessfully() throws Exception {
            // Given
            Long categoryId = 1L;
            CategoryRequest updateRequest = new CategoryRequest("Updated Action");
            CategoryResponse updatedResponse = new CategoryResponse(
                    1L, "Updated Action", 0, LocalDateTime.now(), LocalDateTime.now()
            );

            when(categoryService.updateCategory(eq(categoryId), any(CategoryRequest.class)))
                    .thenReturn(updatedResponse);
            when(responseMapper.toSuccessResponse(anyString(), any())).thenReturn(null);

            // When & Then
            mockMvc.perform(put("/api/v1/categories/{id}", categoryId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON));

            verify(categoryService).updateCategory(eq(categoryId), any(CategoryRequest.class));
            verify(responseMapper).toSuccessResponse(eq("Category updated successfully"), eq(updatedResponse));
        }

        @Test
        @WithMockUser(authorities = "ROLE_USER")
        @DisplayName("Should return 403 when user lacks DEVELOPER authority for update")
        void shouldReturn403WhenUserLacksDeveloperAuthorityForUpdate() throws Exception {
            // Given
            Long categoryId = 1L;
            CategoryRequest updateRequest = new CategoryRequest("Updated Action");

            // When & Then
            mockMvc.perform(put("/api/v1/categories/{id}", categoryId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andDo(print())
                    .andExpect(status().isForbidden());

            verify(categoryService, never()).updateCategory(any(), any());
        }

        @Test
        @WithMockUser(authorities = "ROLE_DEVELOPER")
        @DisplayName("Should return 404 when category not found for update")
        void shouldReturn404WhenCategoryNotFoundForUpdate() throws Exception {
            // Given
            Long categoryId = 999L;
            CategoryRequest updateRequest = new CategoryRequest("Updated Action");

            when(categoryService.updateCategory(eq(categoryId), any(CategoryRequest.class)))
                    .thenThrow(CategoryNotFoundException.byId(categoryId));

            // When & Then
            mockMvc.perform(put("/api/v1/categories/{id}", categoryId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andDo(print())
                    .andExpect(status().isNotFound());

            verify(categoryService).updateCategory(eq(categoryId), any(CategoryRequest.class));
        }

        @Test
        @WithMockUser(authorities = "ROLE_DEVELOPER")
        @DisplayName("Should return 409 when updated name already exists")
        void shouldReturn409WhenUpdatedNameAlreadyExists() throws Exception {
            // Given
            Long categoryId = 1L;
            CategoryRequest updateRequest = new CategoryRequest("Existing Name");

            when(categoryService.updateCategory(eq(categoryId), any(CategoryRequest.class)))
                    .thenThrow(CategoryAlreadyExistsException.withName("Existing Name"));

            // When & Then
            mockMvc.perform(put("/api/v1/categories/{id}", categoryId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andDo(print())
                    .andExpect(status().isConflict());

            verify(categoryService).updateCategory(eq(categoryId), any(CategoryRequest.class));
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/categories/{id} - Delete Category Tests")
    class DeleteCategoryTests {

        @Test
        @WithMockUser(authorities = "ROLE_DEVELOPER")
        @DisplayName("Should delete category successfully")
        void shouldDeleteCategorySuccessfully() throws Exception {
            // Given
            Long categoryId = 1L;
            doNothing().when(categoryService).deleteCategory(categoryId);
            when(responseMapper.toSuccessResponse(anyString())).thenReturn(null);

            // When & Then
            mockMvc.perform(delete("/api/v1/categories/{id}", categoryId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON));

            verify(categoryService).deleteCategory(categoryId);
            verify(responseMapper).toSuccessResponse("Category deleted successfully");
        }

        @Test
        @WithMockUser(authorities = "ROLE_USER")
        @DisplayName("Should return 403 when user lacks DEVELOPER authority for deletion")
        void shouldReturn403WhenUserLacksDeveloperAuthorityForDeletion() throws Exception {
            // Given
            Long categoryId = 1L;

            // When & Then
            mockMvc.perform(delete("/api/v1/categories/{id}", categoryId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isForbidden());

            verify(categoryService, never()).deleteCategory(any());
        }

        @Test
        @WithMockUser(authorities = "ROLE_DEVELOPER")
        @DisplayName("Should return 404 when category not found for deletion")
        void shouldReturn404WhenCategoryNotFoundForDeletion() throws Exception {
            // Given
            Long categoryId = 999L;
            doThrow(CategoryNotFoundException.byId(categoryId))
                    .when(categoryService).deleteCategory(categoryId);

            // When & Then
            mockMvc.perform(delete("/api/v1/categories/{id}", categoryId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isNotFound());

            verify(categoryService).deleteCategory(categoryId);
        }

        @Test
        @WithMockUser(authorities = "ROLE_DEVELOPER")
        @DisplayName("Should return 409 when category is in use")
        void shouldReturn409WhenCategoryIsInUse() throws Exception {
            // Given
            Long categoryId = 1L;
            doThrow(CategoryInUseException.withGames("Action", 5))
                    .when(categoryService).deleteCategory(categoryId);

            // When & Then
            mockMvc.perform(delete("/api/v1/categories/{id}", categoryId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isConflict());

            verify(categoryService).deleteCategory(categoryId);
        }

        @Test
        @WithMockUser(authorities = "ROLE_DEVELOPER")
        @DisplayName("Should return 400 for invalid ID format in deletion")
        void shouldReturn400ForInvalidIdFormatInDeletion() throws Exception {
            // When & Then
            mockMvc.perform(delete("/api/v1/categories/{id}", "invalid")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isBadRequest());

            verify(categoryService, never()).deleteCategory(any());
        }
    }

    @Nested
    @DisplayName("CSRF and Security Tests")
    class SecurityTests {

        @Test
        @WithMockUser(authorities = "ROLE_DEVELOPER")
        @DisplayName("Should return 403 when CSRF token is missing for POST")
        void shouldReturn403WhenCSRFTokenIsMissingForPost() throws Exception {
            // When & Then
            mockMvc.perform(post("/api/v1/categories")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(categoryRequest)))
                    .andDo(print())
                    .andExpect(status().isForbidden());

            verify(categoryService, never()).createCategory(any());
        }

        @Test
        @WithMockUser(authorities = "ROLE_DEVELOPER")
        @DisplayName("Should return 403 when CSRF token is missing for PUT")
        void shouldReturn403WhenCSRFTokenIsMissingForPut() throws Exception {
            // When & Then
            mockMvc.perform(put("/api/v1/categories/{id}", 1L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(categoryRequest)))
                    .andDo(print())
                    .andExpect(status().isForbidden());

            verify(categoryService, never()).updateCategory(any(), any());
        }

        @Test
        @WithMockUser(authorities = "ROLE_DEVELOPER")
        @DisplayName("Should return 403 when CSRF token is missing for DELETE")
        void shouldReturn403WhenCSRFTokenIsMissingForDelete() throws Exception {
            // When & Then
            mockMvc.perform(delete("/api/v1/categories/{id}", 1L)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isForbidden());

            verify(categoryService, never()).deleteCategory(any());
        }
    }

    @Nested
    @DisplayName("Content Type and Headers Tests")
    class ContentTypeTests {

        @Test
        @WithMockUser(authorities = "ROLE_DEVELOPER")
        @DisplayName("Should return 415 when content type is not JSON for POST")
        void shouldReturn415WhenContentTypeIsNotJsonForPost() throws Exception {
            // When & Then
            mockMvc.perform(post("/api/v1/categories")
                            .with(csrf())
                            .contentType(MediaType.TEXT_PLAIN)
                            .content("Action"))
                    .andDo(print())
                    .andExpect(status().isUnsupportedMediaType());

            verify(categoryService, never()).createCategory(any());
        }

        @Test
        @WithMockUser
        @DisplayName("Should accept requests without Accept header")
        void shouldAcceptRequestsWithoutAcceptHeader() throws Exception {
            // Given
            when(categoryService.getAllCategories()).thenReturn(categoryList);
            when(responseMapper.toSuccessResponse(categoryList)).thenReturn(null);

            // When & Then
            mockMvc.perform(get("/api/v1/categories"))
                    .andDo(print())
                    .andExpect(status().isOk());

            verify(categoryService).getAllCategories();
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @WithMockUser(authorities = "ROLE_DEVELOPER")
        @DisplayName("Should handle service layer exceptions gracefully")
        void shouldHandleServiceLayerExceptionsGracefully() throws Exception {
            // Given
            when(categoryService.createCategory(any(CategoryRequest.class)))
                    .thenThrow(new RuntimeException("Unexpected service error"));

            // When & Then
            mockMvc.perform(post("/api/v1/categories")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(categoryRequest)))
                    .andDo(print())
                    .andExpect(status().isInternalServerError());

            verify(categoryService).createCategory(any(CategoryRequest.class));
        }

        @Test
        @WithMockUser
        @DisplayName("Should handle null pointer exceptions gracefully")
        void shouldHandleNullPointerExceptionsGracefully() throws Exception {
            // Given
            when(categoryService.getAllCategories()).thenThrow(new NullPointerException("Null pointer error"));

            // When & Then
            mockMvc.perform(get("/api/v1/categories")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isInternalServerError());

            verify(categoryService).getAllCategories();
        }
    }
}
