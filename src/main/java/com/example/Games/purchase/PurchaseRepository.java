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

    @Query("SELECT p.game.id FROM PurchaseHistory p WHERE p.user.id = :userId AND p.game.id IN :gameIds")
    List<Long> findOwnedGameIds(@Param("userId") Long userId, @Param("gameIds") List<Long> gameIds);

    @Query("SELECT p FROM PurchaseHistory p " +
            "JOIN FETCH p.game g " +
            "JOIN FETCH g.author " +
            "WHERE p.user = :user " +
            "ORDER BY p.purchasedAt DESC")
    List<PurchaseHistory> findByUserWithGameAndAuthor(@Param("user") User user);

    @Query(value = "SELECT p FROM PurchaseHistory p " +
            "JOIN FETCH p.game g " +
            "JOIN FETCH g.author " +
            "WHERE p.user = :user",
            countQuery = "SELECT COUNT(p) FROM PurchaseHistory p WHERE p.user = :user")
    Page<PurchaseHistory> findByUserWithGameAndAuthor(@Param("user") User user, Pageable pageable);

    @Query("SELECT p FROM PurchaseHistory p " +
            "JOIN FETCH p.game g " +
            "JOIN FETCH g.author " +
            "WHERE p.user.id = :userId " +
            "ORDER BY p.purchasedAt DESC")
    List<PurchaseHistory> findByUserIdWithGameAndAuthor(@Param("userId") Long userId);

    @Query("SELECT p FROM PurchaseHistory p " +
            "JOIN FETCH p.game g " +
            "JOIN FETCH g.author " +
            "WHERE p.game.id = :gameId " +
            "ORDER BY p.purchasedAt DESC")
    List<PurchaseHistory> findByGameIdWithGameAndAuthor(@Param("gameId") Long gameId);

    @Query("SELECT p FROM PurchaseHistory p " +
            "JOIN FETCH p.game g " +
            "JOIN FETCH g.author " +
            "WHERE g.author.id = :developerId " +
            "ORDER BY p.purchasedAt DESC")
    List<PurchaseHistory> findSalesByDeveloperIdWithGame(@Param("developerId") Long developerId);

    @Query("SELECT SUM(p.purchasePrice) FROM PurchaseHistory p WHERE p.game.author.id = :developerId")
    Optional<BigDecimal> calculateTotalRevenueForDeveloper(@Param("developerId") Long developerId);
}
