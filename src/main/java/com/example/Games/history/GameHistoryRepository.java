package com.example.Games.history;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GameHistoryRepository extends JpaRepository<GameHistory, Long> {
    List<GameHistory> findByGameId(Long gameId);
    List<GameHistory> findByChangedById(Long userId);
}
