package com.example.Games.gameHistory.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;

public record GameTimelineResponse(
        @JsonProperty("game_id")
        Long gameId,
        
        @JsonProperty("game_title")
        String gameTitle,
        
        @JsonProperty("created_by")
        String createdBy,
        
        @JsonProperty("created_at")
        LocalDateTime createdAt,
        
        @JsonProperty("total_changes")
        long totalChanges,
        
        @JsonProperty("last_modified")
        LocalDateTime lastModified,
        
        @JsonProperty("activity_timeline")
        List<GameHistoryResponse> timeline
) {}
