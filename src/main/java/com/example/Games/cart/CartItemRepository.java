package com.example.Games.cart;

import com.example.Games.user.auth.User;
import com.example.Games.game.Game;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    boolean existsByUserAndGame(User user, Game game);
    void deleteByUserAndGame(User user, Game game);
    void deleteAllByUser(User user);
    int countByUser(User user);

    @Query("SELECT ci FROM CartItem ci JOIN FETCH ci.game WHERE ci.user = :user")
    List<CartItem> findByUserWithGame(@Param("user") User user);

    @Query("SELECT ci.game.id FROM CartItem ci WHERE ci.user = :user")
    List<Long> findGameIdsByUser(@Param("user") User user);
}
