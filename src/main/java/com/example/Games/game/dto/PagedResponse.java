package com.example.Games.game.dto;

import java.util.List;

public record PagedResponse(
        List<Response> games,
        int pageNumber,
        int pageSize,
        long totalElements,
        int totalPages
) {
}
