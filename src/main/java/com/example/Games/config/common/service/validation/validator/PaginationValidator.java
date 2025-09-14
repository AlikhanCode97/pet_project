package com.example.Games.config.common.service.validation.validator;

public final class PaginationValidator {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int MIN_PAGE_SIZE = 1;

    private PaginationValidator() {}

    public static void validate(int page, int size) {
        validatePageNumber(page);
        validatePageSize(size);
    }

    public static void validatePageNumber(int page) {
        if (page < 0) {
            throw new IllegalArgumentException("Page number cannot be negative");
        }
    }

    public static void validatePageSize(int size) {
        if (size < MIN_PAGE_SIZE || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException(
                String.format("Page size must be between %d and %d", MIN_PAGE_SIZE, MAX_PAGE_SIZE)
            );
        }
    }
}
