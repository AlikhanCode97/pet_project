package com.example.Games.user.userLibrary;

import com.example.Games.user.User;
import com.example.Games.game.Game;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserLibraryRepository extends JpaRepository<UserLibrary, Long> {

    List<UserLibrary> findByUserId(Long userId);

    List<UserLibrary> findByGameId(Long gameId);

    Optional<UserLibrary> findByUserIdAndGameId(Long userId, Long gameId);

    boolean existsByUserIdAndGameId(Long userId, Long gameId);

    List<UserLibrary> findByUser(User user);

    void deleteByUserAndGame(User user, Game game);

    void deleteAllByUser(User user);
}
