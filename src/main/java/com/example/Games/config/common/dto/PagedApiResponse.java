package com.example.Games.config.common.dto;

import java.util.List;

public record PagedApiResponse<T>(
        List<T> data,
        PageMetadata pagination,
        String message,
        Long timestamp
) {
    public static <T> PagedApiResponse<T> success(List<T> data, PageMetadata pagination) {
        return new PagedApiResponse<>(data, pagination, null, System.currentTimeMillis());
    }
    
    public static <T> PagedApiResponse<T> success(String message, List<T> data, PageMetadata pagination) {
        return new PagedApiResponse<>(data, pagination, message, System.currentTimeMillis());
    }
    
    public static <T> PagedApiResponse<T> error(String message) {
        return new PagedApiResponse<>(null, null, message, System.currentTimeMillis());
    }
}
