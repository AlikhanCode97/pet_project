package com.example.Games.game;

import com.example.Games.category.Category;
import com.example.Games.rating.RatingLabel;
import com.example.Games.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.math.BigDecimal;
import java.time.LocalDateTime;

//@EnableJpaAuditing
//@EntityListeners(AuditingEntityListener.class)
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Game {

    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "game_seq")
    @SequenceGenerator(name = "game_seq", sequenceName = "game_sequence", allocationSize = 2)
    private Long id;
    private String title;
    @ManyToOne
    @JoinColumn(name = "author_id", nullable = false)
    private User author;
    private BigDecimal price;

    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "category_id")
    private Category category;

    //@CreatedDate
    //@Column(updatable = false)
    private LocalDateTime createdAt;

    //@LastModifiedDate
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public static Game of(String title, BigDecimal price, Category category){
        Game game = new Game();
        game.title = title;
        game.price = price;
        game.category = category;
        return game;
    }


    public void updatePrice(BigDecimal price) {
        this.price = price;
    }

    public void updateTitle(String title) {
        this.title = title;
    }

    public void updateCategory(Category category) {
        this.category = category;
    }
}
