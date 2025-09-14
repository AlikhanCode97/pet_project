package com.example.Games.gameHistory.dto;

import java.util.List;

public record DeveloperActivityResponse(
        Long developerId,
        String developerUsername,
        String developerEmail,
        long totalGamesCreated,
        long totalChanges,
        List<GameHistoryResponse> recentActivity,
        java.time.LocalDateTime firstActivity,
        java.time.LocalDateTime lastActivity
) {}
