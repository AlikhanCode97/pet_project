package com.example.Games.gameHistory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface GameHistoryRepository extends JpaRepository<GameHistory, Long> {

    // Paginated queries
    @Query("SELECT h FROM GameHistory h WHERE h.game.id = :gameId ORDER BY h.changedAt DESC")
    Page<GameHistory> findByGameId(@Param("gameId") Long gameId, Pageable pageable);
    
    @Query("SELECT h FROM GameHistory h WHERE h.changedBy.id = :userId ORDER BY h.changedAt DESC")
    Page<GameHistory> findByChangedBy_Id(@Param("userId") Long userId, Pageable pageable);

    // NEW: Purchase-specific queries
    @Query("SELECT h FROM GameHistory h WHERE h.game.id = :gameId AND h.actionType = :actionType ORDER BY h.changedAt DESC")
    Page<GameHistory> findByGameIdAndActionType(@Param("gameId") Long gameId, @Param("actionType") ActionType actionType, Pageable pageable);
    
    @Query("SELECT h FROM GameHistory h WHERE h.changedBy.id = :userId AND h.actionType = :actionType ORDER BY h.changedAt DESC")
    Page<GameHistory> findByChangedByIdAndActionType(@Param("userId") Long userId, @Param("actionType") ActionType actionType, Pageable pageable);

    // User activity queries
    @Query("SELECT COUNT(h) FROM GameHistory h WHERE h.changedBy.id = :userId AND h.actionType = :actionType")
    long countByUserAndActionType(@Param("userId") Long userId, @Param("actionType") ActionType actionType);

    @Query("SELECT COUNT(h) FROM GameHistory h WHERE h.changedBy.id = :userId")
    long countByUserId(@Param("userId") Long userId);

    @Query("SELECT h FROM GameHistory h JOIN FETCH h.game JOIN FETCH h.changedBy WHERE h.changedBy.id = :userId ORDER BY h.changedAt DESC")
    List<GameHistory> findRecentActivityByUser(@Param("userId") Long userId, Pageable pageable);
    
    @Query("SELECT MIN(h.changedAt) FROM GameHistory h WHERE h.changedBy.id = :userId")
    LocalDateTime findFirstActivityByUser(@Param("userId") Long userId);
    
    @Query("SELECT MAX(h.changedAt) FROM GameHistory h WHERE h.changedBy.id = :userId") 
    LocalDateTime findLastActivityByUser(@Param("userId") Long userId);
}
