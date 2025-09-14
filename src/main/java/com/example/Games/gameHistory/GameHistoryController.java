package com.example.Games.gameHistory;

import com.example.Games.config.common.mappers.ResponseMapStruct;
import com.example.Games.config.common.dto.ApiResponse;
import com.example.Games.gameHistory.dto.DeveloperActivityResponse;
import com.example.Games.gameHistory.dto.GameHistoryResponse;
import com.example.Games.gameHistory.dto.GameTimelineResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/history")
@RequiredArgsConstructor
public class GameHistoryController {

    private final GameHistoryService historyService;
    private final ResponseMapStruct responseMapper;

    @GetMapping("/game/{gameId}")
    @PreAuthorize("hasAuthority('ROLE_DEVELOPER')")
    public ResponseEntity<ApiResponse<Page<GameHistoryResponse>>> getGameHistory(
            @PathVariable Long gameId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<GameHistoryResponse> history = historyService.getGameHistory(gameId, page, size);
        return ResponseEntity.ok(
                responseMapper.toSuccessResponse("Game history retrieved", history)
        );
    }

    @GetMapping("/developer/{developerId}")
    @PreAuthorize("hasAuthority('ROLE_DEVELOPER')")
    public ResponseEntity<ApiResponse<DeveloperActivityResponse>> getDeveloperActivity(@PathVariable Long developerId) {
        DeveloperActivityResponse activity = historyService.getDeveloperActivity(developerId);
        return ResponseEntity.ok(
                responseMapper.toSuccessResponse("Developer activity retrieved", activity)
        );
    }

    @GetMapping("/developer/{developerId}/history")
    @PreAuthorize("hasAuthority('ROLE_DEVELOPER')")
    public ResponseEntity<ApiResponse<Page<GameHistoryResponse>>> getDeveloperHistory(
            @PathVariable Long developerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<GameHistoryResponse> history = historyService.getDeveloperHistory(developerId, page, size);
        return ResponseEntity.ok(
                responseMapper.toSuccessResponse("Developer history retrieved", history)
        );
    }

    @GetMapping("/my-activity")
    @PreAuthorize("hasAuthority('ROLE_DEVELOPER')")
    public ResponseEntity<ApiResponse<DeveloperActivityResponse>> getMyActivity() {
        DeveloperActivityResponse activity = historyService.getMyActivity();
        return ResponseEntity.ok(
                responseMapper.toSuccessResponse("Your activity retrieved", activity)
        );
    }
}
