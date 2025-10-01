package com.example.Games.game;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface GameRepository extends JpaRepository<Game, Long> {

    Optional<Game> findByTitle(String title);
    
    List<Game> findByTitleContainingIgnoreCase(String title);

    List<Game> findByAuthor_Username(String username);

    @Query("SELECT g FROM Game g WHERE g.price BETWEEN :minPrice AND :maxPrice")
    List<Game> findGamesInPriceRange(@Param("minPrice") BigDecimal minPrice, @Param("maxPrice") BigDecimal maxPrice);

    List<Game> findAllByOrderByPriceAsc();
    
    List<Game> findAllByOrderByPriceDesc();

    Page<Game> findByCategoryId(Long categoryId, Pageable pageable);

    // Fetch game with author to avoid N+1 (for permission checks)
    @Query("SELECT g FROM Game g JOIN FETCH g.author WHERE g.id = :id")
    Optional<Game> findByIdWithAuthor(@Param("id") Long id);

    // Get category ID without loading the full category entity
    @Query("SELECT g.category.id FROM Game g WHERE g.id = :gameId")
    Optional<Long> findCategoryIdByGameId(@Param("gameId") Long gameId);

    // Fetch games with authors to avoid N+1
    @Query("SELECT g FROM Game g JOIN FETCH g.author WHERE g.id IN :gameIds")
    List<Game> findAllByIdWithAuthor(@Param("gameIds") List<Long> gameIds);
    }
