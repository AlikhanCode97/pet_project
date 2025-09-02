package com.example.Games.cart;

import com.example.Games.user.User;
import com.example.Games.game.Game;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    List<CartItem> findByUser(User user);
    Optional<CartItem> findByUserAndGame(User user, Game game);
    boolean existsByUserAndGame(User user, Game game);
    void deleteByUserAndGame(User user, Game game);
    void deleteAllByUser(User user);
}
