package com.example.Games.gameHistory.dto;

import java.time.LocalDateTime;
import java.util.List;

public record DeveloperActivityResponse(
        Long developerId,
        String developerUsername,
        String developerEmail,
        long totalGamesCreated,
        long totalChanges,
        List<GameHistoryResponse> recentActivity,
        LocalDateTime firstActivity,
        LocalDateTime lastActivity
) {}
