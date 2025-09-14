package com.example.Games.config.common.mappers;

import com.example.Games.config.common.dto.ApiResponse;
import com.example.Games.config.common.dto.PagedApiResponse;
import com.example.Games.config.common.dto.PageMetadata;
import org.mapstruct.Mapper;
import org.springframework.data.domain.Page;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mapper(componentModel = "spring")
public interface ResponseMapStruct {
    

    default <T> ApiResponse<T> toSuccessResponse(T data) {
        return ApiResponse.success(data);
    }

    default <T> ApiResponse<T> toSuccessResponse(String message, T data) {
        return ApiResponse.success(message, data);
    }

    default <T> ApiResponse<T> toSuccessResponse(String message) {
        return ApiResponse.success(message, null);
    }

    default <T> PagedApiResponse<T> toPagedSuccessResponse(List<T> data, Page<?> page) {
        PageMetadata metadata = PageMetadata.from(page);
        return PagedApiResponse.success(data, metadata);
    }

    default <T> PagedApiResponse<T> toPagedSuccessResponse(String message, List<T> data, Page<?> page) {
        PageMetadata metadata = PageMetadata.from(page);
        return PagedApiResponse.success(message, data, metadata);
    }

    default <T> ApiResponse<T> toErrorResponse(String message) {
        return ApiResponse.error(message);
    }

    default <T> ApiResponse<T> toErrorResponse(BindingResult bindingResult) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError error : bindingResult.getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }
        
        String message = "Validation failed: " + 
            String.join(", ", errors.values());
        
        return (ApiResponse<T>) ApiResponse.error(message, errors);
    }

    default <T> ApiResponse<T> toErrorResponse(String message, T errorData) {
        return ApiResponse.error(message, errorData);
    }

    default <T> ApiResponse<T> toNotFoundResponse(String resourceType, Object identifier) {
        String message = String.format("%s not found with identifier: %s", resourceType, identifier);
        return ApiResponse.error(message);
    }

    default <T> ApiResponse<T> toBadRequestResponse(String message) {
        return ApiResponse.error(message);
    }
}
