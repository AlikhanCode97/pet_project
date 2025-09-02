package com.example.Games.user.userLibrary;

import com.example.Games.game.Game;
import com.example.Games.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(
        name = "user_library",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "game_id"})
)
public class UserLibrary {

    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "game_id")
    private Game game;

    private LocalDateTime addedAt;

    @PrePersist
    public void prePersist() {
        addedAt = LocalDateTime.now();
    }

    public static UserLibrary of(User user, Game game) {
        UserLibrary ul = new UserLibrary();
        ul.user = user;
        ul.game = game;
        return ul;
    }
}
