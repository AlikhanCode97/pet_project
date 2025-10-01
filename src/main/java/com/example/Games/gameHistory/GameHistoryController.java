package com.example.Games.gameHistory;

import com.example.Games.config.common.mappers.ResponseMapStruct;
import com.example.Games.config.common.dto.ApiResponse;
import com.example.Games.gameHistory.dto.DeveloperActivityResponse;
import com.example.Games.gameHistory.dto.GameHistoryResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/history")
@RequiredArgsConstructor
public class GameHistoryController {

    private final GameHistoryService historyService;
    private final ResponseMapStruct responseMapper;

    @GetMapping("/game/{gameId}")
    @PreAuthorize("@authorizationUtils.isAdmin()")
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
    @PreAuthorize("@authorizationUtils.isAdmin()")
    public ResponseEntity<ApiResponse<DeveloperActivityResponse>> getDeveloperActivity(@PathVariable Long developerId) {
        DeveloperActivityResponse activity = historyService.getDeveloperActivity(developerId);
        return ResponseEntity.ok(
                responseMapper.toSuccessResponse("Developer activity retrieved", activity)
        );
    }

    @GetMapping("/developer/{developerId}/history")
    @PreAuthorize("@authorizationUtils.isAdmin()")
    public ResponseEntity<ApiResponse<Page<GameHistoryResponse>>> getDeveloperHistory(
            @PathVariable Long developerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<GameHistoryResponse> history = historyService.getDeveloperHistory(developerId, page, size);
        return ResponseEntity.ok(
                responseMapper.toSuccessResponse("Developer history retrieved", history)
        );
    }

    @GetMapping("/my/game/{gameId}")
    @PreAuthorize("@authorizationUtils.isDeveloper()")
    public ResponseEntity<ApiResponse<Page<GameHistoryResponse>>> getMyGameHistory(
            @PathVariable Long gameId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<GameHistoryResponse> history = historyService.getMyGameHistory(gameId, page, size);
        return ResponseEntity.ok(
                responseMapper.toSuccessResponse("My game history retrieved", history)
        );
    }

    @GetMapping("/my/activity")
    @PreAuthorize("@authorizationUtils.isDeveloper()")
    public ResponseEntity<ApiResponse<DeveloperActivityResponse>> getMyDeveloperActivity() {
        DeveloperActivityResponse activity = historyService.getMyDeveloperActivity();
        return ResponseEntity.ok(
                responseMapper.toSuccessResponse("My developer activity retrieved", activity)
        );
    }

    @GetMapping("/my/history")
    @PreAuthorize("@authorizationUtils.isDeveloper()")
    public ResponseEntity<ApiResponse<Page<GameHistoryResponse>>> getMyDeveloperHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<GameHistoryResponse> history = historyService.getMyDeveloperHistory(page, size);
        return ResponseEntity.ok(
                responseMapper.toSuccessResponse("My developer history retrieved", history)
        );
    }
}
