package com.example.Games.config.common.dto;

public record ApiResponse<T>(
        String message,
        T data,
        Long timestamp
) {
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>( null, data, System.currentTimeMillis());
    }
    
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(message, data, System.currentTimeMillis());
    }
    
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(message, null, System.currentTimeMillis());
    }
    
    public static <T> ApiResponse<T> error(String message, T data) {
        return new ApiResponse<>( message, data, System.currentTimeMillis());
    }
}
