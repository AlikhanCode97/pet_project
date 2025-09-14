package com.example.Games.game;

import com.example.Games.user.auth.User;
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
    
    List<Game> findByAuthor(User author);
    
    List<Game> findByAuthor_Username(String username);

    @Query("SELECT g FROM Game g WHERE g.price BETWEEN :minPrice AND :maxPrice")
    List<Game> findGamesInPriceRange(@Param("minPrice") BigDecimal minPrice, @Param("maxPrice") BigDecimal maxPrice);

    List<Game> findAllByOrderByPriceAsc();
    
    List<Game> findAllByOrderByPriceDesc();

    Page<Game> findByCategoryId(Long categoryId, Pageable pageable);

    @Query("SELECT g FROM Game g WHERE LOWER(g.title) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Game> searchByTitle(@Param("keyword") String keyword, Pageable pageable);
    }
