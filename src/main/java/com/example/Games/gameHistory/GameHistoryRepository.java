package com.example.Games.gameHistory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface GameHistoryRepository extends JpaRepository<GameHistory, Long> {

    @Query(
        value = "SELECT h FROM GameHistory h JOIN FETCH h.game JOIN FETCH h.changedBy WHERE h.game.id = :gameId ORDER BY h.changedAt DESC",
        countQuery = "SELECT COUNT(h) FROM GameHistory h WHERE h.game.id = :gameId"
    )
    Page<GameHistory> findByGameIdWithRelations(@Param("gameId") Long gameId, Pageable pageable);
    
    @Query(
        value = "SELECT h FROM GameHistory h JOIN FETCH h.game JOIN FETCH h.changedBy WHERE h.changedBy.id = :userId ORDER BY h.changedAt DESC",
        countQuery = "SELECT COUNT(h) FROM GameHistory h WHERE h.changedBy.id = :userId"
    )
    Page<GameHistory> findByChangedByIdWithRelations(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT COUNT(h) FROM GameHistory h WHERE h.changedBy.id = :userId AND h.actionType = :actionType")
    long countByUserAndActionType(@Param("userId") Long userId, @Param("actionType") ActionType actionType);

    @Query("SELECT COUNT(h) FROM GameHistory h WHERE h.changedBy.id = :userId")
    long countByUserId(@Param("userId") Long userId);

    List<GameHistory> findTop5ByChangedByIdOrderByChangedAtDesc(Long userId);


    @Query("SELECT MIN(h.changedAt) FROM GameHistory h WHERE h.changedBy.id = :userId")
    LocalDateTime findFirstActivityByUser(@Param("userId") Long userId);
    
    @Query("SELECT MAX(h.changedAt) FROM GameHistory h WHERE h.changedBy.id = :userId") 
    LocalDateTime findLastActivityByUser(@Param("userId") Long userId);

}
