package com.example.Games.game;

import com.example.Games.game.dto.CreateRequest;
import com.example.Games.game.dto.Response;
import com.example.Games.game.dto.UpdateRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/games")
@RequiredArgsConstructor
public class GameController {

    private final GameService gameService;

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_DEVELOPER')")
    public ResponseEntity<Response> create(@RequestBody @Valid CreateRequest request) {
        return ResponseEntity.ok(gameService.createGame(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_DEVELOPER')")
    public ResponseEntity<Response> update(@PathVariable Long id, @RequestBody @Valid UpdateRequest request) {
        return ResponseEntity.ok(gameService.updateGame(id, request));
    }

    @GetMapping
    public ResponseEntity<List<Response>> getAll() {
        return ResponseEntity.ok(gameService.getAllGames());
    }

    @GetMapping("/{title}")
    public ResponseEntity<Response> getByTitle(@PathVariable String title) {
        return ResponseEntity.ok(gameService.getGameById(title));
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        gameService.deleteGame(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search/author")
    public ResponseEntity<List<Response>> searchByAuthor(@RequestParam String author) {
        return ResponseEntity.ok(gameService.searchGamesByAuthorNative(author));
    }

    @GetMapping("/search/title")
    public ResponseEntity<List<Response>> searchByTitle(@RequestParam String title) {
        return ResponseEntity.ok(gameService.searchByTitle(title));
    }

    @GetMapping("/filter/price")
    public ResponseEntity<List<Response>> filterByPriceRange(
            @RequestParam BigDecimal min,
            @RequestParam BigDecimal max
    ) {
        return ResponseEntity.ok(gameService.getGamesInPriceRange(min, max));
    }

    @GetMapping("/sorted")
    public ResponseEntity<List<Response>> getSorted() {
        return ResponseEntity.ok(gameService.getGamesSortedByPrice());
    }
}
