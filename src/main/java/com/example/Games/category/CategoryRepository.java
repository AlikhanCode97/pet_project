package com.example.Games.category;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findByName(String name);

    @Query("SELECT COUNT(g) > 0 FROM Game g WHERE g.category.id = :categoryId")
    boolean hasGames(@Param("categoryId") Long categoryId);

    @Query("SELECT COUNT(g) FROM Game g WHERE g.category.id = :categoryId")
    int countGamesByCategoryId(@Param("categoryId") Long categoryId);

    @Query("SELECT c FROM Category c JOIN FETCH c.createdBy WHERE c.id = :id")
    Optional<Category> findByIdWithCreator(@Param("id") Long id);
}
