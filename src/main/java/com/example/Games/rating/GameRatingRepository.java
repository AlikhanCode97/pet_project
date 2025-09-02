package com.example.Games.rating;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GameRatingRepository extends JpaRepository<GameRating, Long> {
    List<GameRating> findByGameId(Long gameId);
    Optional<GameRating> findByUserIdAndGameId(Long userId, Long gameId);
    long countByGameId(Long gameId);
}
