package com.example.Games.gameHistory.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public record GameHistoryResponse(
        Long id,
        
        @JsonProperty("game_id")
        Long gameId,
        
        @JsonProperty("game_title")
        String gameTitle,
        
        @JsonProperty("action_type")
        String actionType,
        
        @JsonProperty("action_description")
        String actionDescription,
        
        @JsonProperty("field_changed")
        String fieldChanged,
        
        @JsonProperty("old_value")
        String oldValue,
        
        @JsonProperty("new_value")
        String newValue,
        
        @JsonProperty("changed_by")
        String changedBy,
        
        @JsonProperty("changed_at")
        LocalDateTime changedAt,
        
        String description
) {}
