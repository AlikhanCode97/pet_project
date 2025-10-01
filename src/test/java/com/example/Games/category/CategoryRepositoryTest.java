package com.example.Games.category;

import com.example.Games.config.TestJpaAuditingConfig;
import com.example.Games.game.Game;
import com.example.Games.user.auth.User;
import com.example.Games.user.role.Role;
import com.example.Games.user.role.RoleType;
import jakarta.persistence.PersistenceException;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@Import(TestJpaAuditingConfig.class)
@ActiveProfiles("test")
@DisplayName("CategoryRepository Tests")
class CategoryRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private CategoryRepository categoryRepository;

    private User creator;
    private Category actionCategory;
    private Category adventureCategory;


    @BeforeEach
    void setUp() {
        Role role = Role.builder().name(RoleType.DEVELOPER).build();
        entityManager.persist(role);

        creator = User.builder()
                .username("testdev")
                .password("password")
                .email("dev@example.com")
                .role(role)
                .build();
        entityManager.persist(creator);

        actionCategory = Category.builder()
                .name("Action")
                .createdBy(creator)
                .build();
        entityManager.persist(actionCategory);

        adventureCategory = Category.builder()
                .name("Adventure")
                .createdBy(creator)
                .build();
        entityManager.persist(adventureCategory);


        Game game = Game.builder()
                .title("Test Game")
                .category(actionCategory)
                .author(creator)
                .price(BigDecimal.valueOf(29.99))
                .build();
        entityManager.persist(game);

        Game game2 = Game.builder()
                .title("Test Game2")
                .category(actionCategory)
                .author(creator)
                .price(BigDecimal.valueOf(29.99))
                .build();
        entityManager.persist(game2);
    }

    @Test
    @DisplayName("Should find category by name")
    void shouldFindByName() {
        Optional<Category> found = categoryRepository.findByName("Action");

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Action");
        assertThat(found.get().getCreatedBy().getUsername()).isEqualTo("testdev");
    }

    @Test
    @DisplayName("Should return empty when category name does not exist")
    void shouldReturnEmptyIfNameNotFound() {
        Optional<Category> found = categoryRepository.findByName("NonExisting");
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Should not allow duplicate category names")
    void shouldNotAllowDuplicateNames() {
        Category duplicate = Category.builder()
                .name("Action")
                .createdBy(creator)
                .build();

        assertThatThrownBy(() -> {
            entityManager.persistAndFlush(duplicate);
        }).isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    @DisplayName("Should return true if category has games")
    void shouldReturnTrueIfCategoryHasGames() {
        boolean hasGames = categoryRepository.hasGames(actionCategory.getId());

        assertThat(hasGames).isTrue();
    }

    @Test
    @DisplayName("Should return false if category has no games")
    void shouldReturnFalseIfCategoryHasNoGames() {
        boolean hasGames = categoryRepository.hasGames(adventureCategory.getId());

        assertThat(hasGames).isFalse();
    }


    @Test
    @DisplayName("Should count games by category ID")
    void shouldCountGamesByCategoryId() {

        int count = categoryRepository.countGamesByCategoryId(actionCategory.getId());
        int count2 = categoryRepository.countGamesByCategoryId(adventureCategory.getId());

        assertThat(count).isEqualTo(2);
        assertThat(count2).isEqualTo(0);
    }

    @Test
    @DisplayName("Should find category with creator eagerly loaded")
    void shouldFindByIdWithCreator() {
        Optional<Category> found = categoryRepository.findByIdWithCreator(actionCategory.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getCreatedBy()).isNotNull();
        assertThat(found.get().getCreatedBy().getUsername()).isEqualTo("testdev");
    }

    @Test
    @DisplayName("Should set createdAt and updatedAt automatically")
    void shouldSetAuditingFields() {

        assertThat(actionCategory.getCreatedAt()).isNotNull();
        assertThat(actionCategory.getUpdatedAt()).isNotNull();
    }
}
