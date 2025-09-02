package com.example.Games.history;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/game-history")
@RequiredArgsConstructor
public class GameHistoryController {

    private final GameHistoryRepository historyRepository;

    @GetMapping("/game/{gameId}")
    @PreAuthorize("hasAuthority('ROLE_DEVELOPER')")
    public ResponseEntity<List<GameHistory>> getByGame(@PathVariable Long gameId) {
        return ResponseEntity.ok(historyRepository.findByGameId(gameId));
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAuthority('ROLE_DEVELOPER')")
    public ResponseEntity<List<GameHistory>> getByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(historyRepository.findByChangedById(userId));
    }
}

