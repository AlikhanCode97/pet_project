package com.example.Games.game;

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
public interface GameRepository extends JpaRepository<Game, Long> {

    Optional<Game> findByTitle(String title);

    List<Game> findByAuthorAndPriceLessThan(String author, BigDecimal price);

    @Query("SELECT g FROM Game g WHERE g.price BETWEEN :minPrice AND :maxPrice")
    List<Game> findGamesInPriceRange(@Param("minPrice") BigDecimal minPrice, @Param("maxPrice") BigDecimal maxPrice);

    Page<Game> findByCategoryId(Long categoryId, Pageable pageable);

    List<Game> findAllByOrderByPriceAsc();

    @Query(value = "SELECT * FROM game WHERE author LIKE %:author%", nativeQuery = true)
    List<Game> searchByAuthorNative(String author);

    @Query("SELECT g FROM Game g WHERE LOWER(g.title) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Game> searchByTitle(@Param("keyword") String keyword, Pageable pageable);

    long countByCategoryId(Long categoryId);

    List<Game> findByRatingGreaterThan(BigDecimal rating);
}

