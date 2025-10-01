package com.example.Games.game;

import com.example.Games.config.common.mappers.ResponseMapStruct;
import com.example.Games.config.common.dto.ApiResponse;
import com.example.Games.game.dto.CreateRequest;
import com.example.Games.game.dto.PagedResponse;
import com.example.Games.game.dto.Response;
import com.example.Games.game.dto.UpdateRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/games")
@RequiredArgsConstructor
@Validated
public class GameController {

    private final GameService gameService;
    private final ResponseMapStruct responseMapper;

    @PostMapping
    @PreAuthorize("@authorizationUtils.isDeveloper()")
    public ResponseEntity<ApiResponse<Response>> createGame(@Valid @RequestBody CreateRequest request) {
        Response response = gameService.createGame(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(responseMapper.toSuccessResponse("Game created successfully", response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@authorizationUtils.isDeveloper()")
    public ResponseEntity<ApiResponse<Response>> updateGame(
            @PathVariable @Positive Long id,
            @Valid @RequestBody UpdateRequest request) {
        Response response = gameService.updateGame(id, request);
        return ResponseEntity.ok(
                responseMapper.toSuccessResponse("Game updated successfully", response)
        );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Response>>> getAllGames() {
        List<Response> games = gameService.getAllGames();
        return ResponseEntity.ok(responseMapper.toSuccessResponse("Games retrieved successfully",games));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Response>> getGameById(@PathVariable @Positive Long id) {
        Response response = gameService.getGameById(id);
        return ResponseEntity.ok(responseMapper.toSuccessResponse("Game retrieved successfully",response));
    }

    @GetMapping("/title/{title}")
    public ResponseEntity<ApiResponse<Response>> getGameByTitle(@PathVariable String title) {
        Response response = gameService.getGameByTitle(title);
        return ResponseEntity.ok(responseMapper.toSuccessResponse(response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@authorizationUtils.isDeveloper()")
    public ResponseEntity<ApiResponse<Object>> deleteGame(@PathVariable @Positive Long id) {
        gameService.deleteGame(id);
        return ResponseEntity.ok(
                responseMapper.toSuccessResponse("Game deleted successfully")
        );
    }

    @GetMapping("/search/title")
    public ResponseEntity<ApiResponse<List<Response>>> searchByTitle(@RequestParam String title) {
        List<Response> games = gameService.searchByTitle(title);
        return ResponseEntity.ok(
                responseMapper.toSuccessResponse("Search completed", games)
        );
    }

    @GetMapping("/search/author")
    public ResponseEntity<ApiResponse<List<Response>>> searchByAuthor(@RequestParam String author) {
        List<Response> games = gameService.searchGamesByAuthor(author);
        return ResponseEntity.ok(
                responseMapper.toSuccessResponse("Search completed", games)
        );
    }

    @GetMapping("/filter/price")
    public ResponseEntity<ApiResponse<List<Response>>> filterByPriceRange(@RequestParam BigDecimal min,
                                                                         @RequestParam BigDecimal max) {
        List<Response> games = gameService.getGamesInPriceRange(min, max);
        return ResponseEntity.ok(
                responseMapper.toSuccessResponse("Price filter applied", games)
        );
    }

    @GetMapping("/sorted")
    public ResponseEntity<ApiResponse<List<Response>>> getSortedByPrice(@RequestParam(defaultValue = "true") boolean ascending) {
        List<Response> games = gameService.getGamesSortedByPrice(ascending);
        String sortDirection = ascending ? "ascending" : "descending";
        return ResponseEntity.ok(
                responseMapper.toSuccessResponse("Games sorted by price " + sortDirection, games)
        );
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<ApiResponse<PagedResponse>> getGamesByCategory(
            @PathVariable @Positive Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PagedResponse games = gameService.getGamesByCategoryPaged(categoryId, page, size);
        return ResponseEntity.ok(
                responseMapper.toSuccessResponse("Games retrieved for category", games)
        );
    }
}
