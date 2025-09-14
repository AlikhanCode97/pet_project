package com.example.Games.purchase;

import com.example.Games.user.auth.User;
import com.example.Games.game.Game;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PurchaseRepository extends JpaRepository<PurchaseHistory, Long> {

    // Check if user owns a game
    boolean existsByUserIdAndGameId(Long userId, Long gameId);
    
    Optional<PurchaseHistory> findByUserIdAndGameId(Long userId, Long gameId);
    
    // Get user's purchases
    List<PurchaseHistory> findByUserOrderByPurchasedAtDesc(User user);
    
    List<PurchaseHistory> findByUserIdOrderByPurchasedAtDesc(Long userId);
    
    Page<PurchaseHistory> findByUser(User user, Pageable pageable);
    
    // Get purchases for a game
    List<PurchaseHistory> findByGameOrderByPurchasedAtDesc(Game game);
    
    List<PurchaseHistory> findByGameIdOrderByPurchasedAtDesc(Long gameId);
    
    // Date range queries
    @Query("SELECT p FROM PurchaseHistory p WHERE p.user = :user AND p.purchasedAt BETWEEN :startDate AND :endDate ORDER BY p.purchasedAt DESC")
    List<PurchaseHistory> findByUserAndDateRange(
            @Param("user") User user,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
    
    // Statistics queries
    @Query("SELECT COUNT(p) FROM PurchaseHistory p WHERE p.user = :user")
    int countByUser(@Param("user") User user);
    
    @Query("SELECT SUM(p.purchasePrice) FROM PurchaseHistory p WHERE p.user = :user")
    Optional<BigDecimal> calculateTotalSpentByUser(@Param("user") User user);
    
    // Get user's library (all games they own)
    @Query("SELECT p.game FROM PurchaseHistory p WHERE p.user = :user")
    List<Game> findGamesByUser(@Param("user") User user);
    
    // Developer sales query
    @Query("SELECT p FROM PurchaseHistory p WHERE p.game.author.id = :developerId ORDER BY p.purchasedAt DESC")
    List<PurchaseHistory> findSalesByDeveloperId(@Param("developerId") Long developerId);
    
    @Query("SELECT SUM(p.purchasePrice) FROM PurchaseHistory p WHERE p.game.author.id = :developerId")
    Optional<BigDecimal> calculateTotalRevenueForDeveloper(@Param("developerId") Long developerId);
}
