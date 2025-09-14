package com.example.Games.gameHistory;

import com.example.Games.game.Game;
import com.example.Games.gameHistory.dto.GameHistoryResponse;
import com.example.Games.gameHistory.dto.GameTimelineResponse;
import com.example.Games.gameHistory.dto.DeveloperActivityResponse;
import com.example.Games.gameHistory.dto.FieldChange;
import com.example.Games.user.auth.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Mapper(componentModel = "spring")
public interface GameHistoryMapStruct {

    @Mapping(target = "gameId", source = "game.id")
    @Mapping(target = "gameTitle", source = "game.title")
    @Mapping(target = "actionType", expression = "java(history.getActionType().name())")
    @Mapping(target = "actionDescription", expression = "java(getActionDescription(history.getActionType()))")
    @Mapping(target = "changedBy", source = "changedBy.username")
    @Mapping(target = "changedAt", source = "changedAt")
    @Mapping(target = "fieldChanged", source = "fieldChanged")
    @Mapping(target = "oldValue", source = "oldValue")
    @Mapping(target = "newValue", source = "newValue")
    @Mapping(target = "description", source = "description")
    GameHistoryResponse toDto(GameHistory history);

    List<GameHistoryResponse> toDtoList(List<GameHistory> histories);
    
    default GameTimelineResponse toTimelineResponse(List<GameHistory> timeline) {
        if (timeline == null || timeline.isEmpty()) {
            return new GameTimelineResponse(null, null, null, null, 0L, null, List.of());
        }
        
        return new GameTimelineResponse(
            getGameId(timeline),
            getGameTitle(timeline),
            getCreatedBy(timeline),
            getCreatedAt(timeline),
            (long) timeline.size(),
            getLastModified(timeline),
            toDtoList(timeline)
        );
    }

    // Factory methods for creating GameHistory entities
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "changedAt", ignore = true)
    @Mapping(target = "actionType", constant = "CREATE")
    @Mapping(target = "fieldChanged", ignore = true)
    @Mapping(target = "oldValue", ignore = true)
    @Mapping(target = "newValue", ignore = true)
    @Mapping(target = "description", expression = "java(String.format(\"Game '%s' created\", game.getTitle()))")
    GameHistory createAction(Game game, User changedBy);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "changedAt", ignore = true)
    @Mapping(target = "actionType", constant = "UPDATE")
    @Mapping(target = "fieldChanged", source = "fieldChange.fieldName")
    @Mapping(target = "oldValue", source = "fieldChange.oldValue")
    @Mapping(target = "newValue", source = "fieldChange.newValue")
    @Mapping(target = "description", expression = "java(String.format(\"Game '%s' field '%s' updated\", game.getTitle(), fieldChange.fieldName()))")
    GameHistory updateAction(Game game, User changedBy, FieldChange fieldChange);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "changedAt", ignore = true)
    @Mapping(target = "actionType", constant = "DELETE")
    @Mapping(target = "fieldChanged", ignore = true)
    @Mapping(target = "oldValue", ignore = true)
    @Mapping(target = "newValue", ignore = true)
    @Mapping(target = "description", expression = "java(String.format(\"Game '%s' deleted\", game.getTitle()))")
    GameHistory deleteAction(Game game, User changedBy);

    // NEW: Purchase factory method
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "changedAt", ignore = true)
    @Mapping(target = "changedBy", source = "purchaser")
    @Mapping(target = "actionType", constant = "PURCHASE")
    @Mapping(target = "fieldChanged", ignore = true)
    @Mapping(target = "oldValue", ignore = true)
    @Mapping(target = "newValue", expression = "java(purchasePrice != null ? purchasePrice.toString() : null)")
    @Mapping(target = "description", expression = "java(String.format(\"Game '%s' purchased by '%s' for $%s\", game.getTitle(), purchaser.getUsername(), purchasePrice))")
    GameHistory purchaseAction(Game game, User purchaser, BigDecimal purchasePrice);

    default String getActionDescription(ActionType actionType) {
        return switch (actionType) {
            case CREATE -> "Game Created";
            case UPDATE -> "Game Updated";
            case DELETE -> "Game Deleted";
            case PURCHASE -> "Game Purchased";
        };
    }

    default Long getGameId(List<GameHistory> timeline) {
        return timeline.isEmpty() ? null : timeline.getFirst().getGame().getId();
    }

    default String getGameTitle(List<GameHistory> timeline) {
        return timeline.isEmpty() ? null : timeline.getFirst().getGame().getTitle();
    }

    default String getCreatedBy(List<GameHistory> timeline) {
        return timeline.isEmpty() ? null : timeline.getFirst().getChangedBy().getUsername();
    }

    default LocalDateTime getCreatedAt(List<GameHistory> timeline) {
        return timeline.isEmpty() ? null : timeline.getFirst().getChangedAt();
    }

    default LocalDateTime getLastModified(List<GameHistory> timeline) {
        return timeline.isEmpty() ? null : timeline.getLast().getChangedAt();
    }

    // Create a helper method for developer activity mapping
    default DeveloperActivityResponse createDeveloperActivityResponse(
            User developer,
            long totalGamesCreated,
            long totalChanges,
            List<GameHistoryResponse> recentActivity,
            LocalDateTime firstActivity,
            LocalDateTime lastActivity) {
        
        return new DeveloperActivityResponse(
                developer.getId(),
                developer.getUsername(),
                developer.getEmail(),
                totalGamesCreated,
                totalChanges,
                recentActivity,
                firstActivity,
                lastActivity
        );
    }
}
