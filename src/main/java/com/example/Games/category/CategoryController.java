package com.example.Games.category;

import com.example.Games.config.common.mappers.ResponseMapStruct;
import com.example.Games.config.common.dto.ApiResponse;
import com.example.Games.category.dto.CategoryRequest;
import com.example.Games.category.dto.CategoryResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/categories")
public class CategoryController {

    private final CategoryService categoryService;
    private final ResponseMapStruct responseMapper;

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_DEVELOPER')")
    public ResponseEntity<ApiResponse<CategoryResponse>> create(@RequestBody @Valid CategoryRequest request) {
        CategoryResponse category = categoryService.createCategory(request);
        return ResponseEntity.ok(
                responseMapper.toSuccessResponse("Category created successfully", category)
        );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getAll() {
        List<CategoryResponse> categories = categoryService.getAllCategories();
        return ResponseEntity.ok(responseMapper.toSuccessResponse(categories));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> getById(@PathVariable Long id) {
        CategoryResponse category = categoryService.getCategoryById(id);
        return ResponseEntity.ok(responseMapper.toSuccessResponse(category));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_DEVELOPER')")
    public ResponseEntity<ApiResponse<CategoryResponse>> update(@PathVariable Long id,
                                                               @RequestBody @Valid CategoryRequest updateRequest) {
        CategoryResponse category = categoryService.updateCategory(id, updateRequest);
        return ResponseEntity.ok(
                responseMapper.toSuccessResponse("Category updated successfully", category)
        );
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_DEVELOPER')")
    public ResponseEntity<ApiResponse<Object>> delete(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.ok(
                responseMapper.toSuccessResponse("Category deleted successfully")
        );
    }
}
