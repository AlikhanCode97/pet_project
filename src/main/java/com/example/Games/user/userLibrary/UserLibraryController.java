package com.example.Games.user.userLibrary;

import com.example.Games.game.dto.Response;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/library")
@RequiredArgsConstructor
public class UserLibraryController {

    private final UserLibraryService userLibraryService;

    @GetMapping
    public ResponseEntity<List<GameResponse>> getUserLibrary() {
        return ResponseEntity.ok(userLibraryService.getLibraryForCurrentUser());
    }

    @DeleteMapping("/{gameId}")
    public ResponseEntity<String> removeGame(@PathVariable Long gameId) {
        userLibraryService.removeGameFromLibrary(gameId);
        return ResponseEntity.ok("Game removed from library");
    }

    @GetMapping("/has/{gameId}")
    public ResponseEntity<Boolean> checkGame(@PathVariable Long gameId) {
        return ResponseEntity.ok(userLibraryService.hasGame(gameId));
    }
}
