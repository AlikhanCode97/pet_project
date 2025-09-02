package com.example.Games.rating;

import com.example.Games.game.Game;
import com.example.Games.game.GameRepository;
import com.example.Games.user.User;
import com.example.Games.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@RequiredArgsConstructor
@Service
public class GameRatingService {

    private final GameRatingRepository ratingRepository;
    private final GameRepository gameRepository;
    private final UserRepository userRepository;

    public void rateGame(Long gameId, int score, String comment) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new RuntimeException("Game not found"));

        GameRating rating = ratingRepository.findByUserIdAndGameId(user.getId(), gameId)
                .orElse(
                        GameRating.builder()
                                .user(user)
                                .game(game)
                                .build()
                );

        rating.setScore(score);
        rating.setComment(comment);
        ratingRepository.save(rating);

        // Recalculate game rating
        List<GameRating> allRatings = ratingRepository.findByGameId(gameId);
        BigDecimal avg = BigDecimal.valueOf(
                allRatings.stream().mapToInt(GameRating::getScore).average().orElse(0)
        );

        game.setAverageRating(avg);
        game.setTotalRatings(allRatings.size());
        gameRepository.save(game);
    }
}
