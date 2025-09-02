package com.example.Games.rating;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ratings")
@RequiredArgsConstructor
public class GameRatingController {

    private final GameRatingService ratingService;

    @PostMapping("/{gameId}")
    public ResponseEntity<Void> rateGame(
            @PathVariable Long gameId,
            @RequestParam int score,
            @RequestParam String comment
    ) {
        ratingService.rateGame(gameId, score, comment);
        return ResponseEntity.ok().build();
    }
}

