package com.example.Games.cart;

import com.example.Games.category.Category;
import com.example.Games.config.TestJpaAuditingConfig;
import com.example.Games.game.Game;
import com.example.Games.user.auth.User;
import com.example.Games.user.role.Role;
import com.example.Games.user.role.RoleType;
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
import java.time.LocalDateTime;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@Import(TestJpaAuditingConfig.class)
@ActiveProfiles("test")
@DisplayName("CartItemRepository Tests")
class CartItemRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private CartItemRepository cartItemRepository;

    private User testUser;
    private User secondUser;
    private User unknownUser;
    private Game actionGame;
    private Game rpgGame;
    private Game strategyGame;
    private Game expensiveGame;

    @BeforeEach
    void setUp() {
        Role developerRole = Role.builder()
                .name(RoleType.DEVELOPER)
                .build();
        developerRole = entityManager.persistAndFlush(developerRole);

        // Setup users
        testUser = User.builder()
                .username("testuser")
                .email("test@example.com")
                .password("password123")
                .role(developerRole)
                .build();
        testUser = entityManager.persistAndFlush(testUser);

        secondUser = User.builder()
                .username("seconduser")
                .email("second@example.com")
                .password("password123")
                .role(developerRole)
                .build();
        secondUser = entityManager.persistAndFlush(secondUser);

        unknownUser = User.builder()
                .username("nouser")
                .email("nouser@example.com")
                .password("nopass")
                .role(testUser.getRole())
                .build();
        entityManager.persistAndFlush(unknownUser);


        // Setup categories
        Category actionCategory = Category.builder()
                .name("Action")
                .createdBy(testUser)
                .build();
        actionCategory = entityManager.persistAndFlush(actionCategory);

        Category rpgCategory = Category.builder()
                .name("RPG")
                .createdBy(testUser)
                .build();
        rpgCategory = entityManager.persistAndFlush(rpgCategory);

        Category strategyCategory = Category.builder()
                .name("Strategy")
                .createdBy(testUser)
                .build();
        strategyCategory = entityManager.persistAndFlush(strategyCategory);

        //Games
        actionGame = Game.builder()
                .title("Epic Action Adventure")
                .author(testUser)
                .price(new BigDecimal("29.99"))
                .category(actionCategory)
                .build();
        actionGame = entityManager.persistAndFlush(actionGame);

        rpgGame = Game.builder()
                .title("Fantasy RPG Quest")
                .author(testUser)
                .price(new BigDecimal("39.99"))
                .category(rpgCategory)
                .build();
        rpgGame = entityManager.persistAndFlush(rpgGame);

        strategyGame = Game.builder()
                .title("Grand Strategy")
                .author(secondUser)
                .price(new BigDecimal("49.99"))
                .category(strategyCategory)
                .build();
        strategyGame = entityManager.persistAndFlush(strategyGame);

        expensiveGame = Game.builder()
                .title("Premium Collection")
                .author(secondUser)
                .price(new BigDecimal("99.99"))
                .category(actionCategory)
                .build();
        expensiveGame = entityManager.persistAndFlush(expensiveGame);

        // Create and persist CartItems
        CartItem cartItem1 = CartItem.builder()
                .user(testUser)
                .game(actionGame)
                .build();
        entityManager.persistAndFlush(cartItem1);

        CartItem cartItem2 = CartItem.builder()
                .user(testUser)
                .game(rpgGame)
                .build();
        entityManager.persistAndFlush(cartItem2);

        CartItem cartItem3 = CartItem.builder()
                .user(secondUser)
                .game(strategyGame)
                .build();
        entityManager.persistAndFlush(cartItem3);

        CartItem cartItem4 = CartItem.builder()
                .user(secondUser)
                .game(expensiveGame)
                .build();
        entityManager.persistAndFlush(cartItem4);
    }

    @Test
    @DisplayName("Should enforce unique constraint on user and game")
    void shouldEnforceUniqueConstraintOnUserAndGame() {
        CartItem duplicate = CartItem.builder()
                .user(testUser)
                .game(actionGame)
                .build();


        assertThatThrownBy(() -> cartItemRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("cart_items");
    }

    @Test
    @DisplayName("Should lazily load user and game")
    void shouldLazilyLoadUserAndGame() {
        CartItem cartItem = cartItemRepository.findByUserWithGame(testUser)
                .stream()
                .findFirst()
                .orElseThrow();


        // Initially only proxy
        assertThat(cartItem.getUser()).isNotNull();
        assertThat(cartItem.getUser().getUsername()).isEqualTo("testuser");

        assertThat(cartItem.getGame()).isNotNull();
        assertThat(cartItem.getGame().getTitle()).isNotBlank();
    }


    @Test
    @DisplayName("Should check if cart item exists by user and game")
    void shouldCheckIfCartItemExistsByUserAndGame() {

        // When & Then
        assertThat(cartItemRepository.existsByUserAndGame(testUser, actionGame)).isTrue();
        assertThat(cartItemRepository.existsByUserAndGame(testUser, rpgGame)).isTrue();
        assertThat(cartItemRepository.existsByUserAndGame(testUser, strategyGame)).isFalse();
        assertThat(cartItemRepository.existsByUserAndGame(testUser, expensiveGame)).isFalse();
        assertThat(cartItemRepository.existsByUserAndGame(secondUser, strategyGame)).isTrue();
        assertThat(cartItemRepository.existsByUserAndGame(secondUser, rpgGame)).isFalse();
    }

    @Test
    @DisplayName("Should correctly check belongsToUser and isForGame")
    void shouldCheckBelongsToUserAndIsForGame() {
        CartItem cartItem = CartItem.builder()
                .user(testUser)
                .game(actionGame)
                .build();

        assertThat(cartItem.belongsToUser(testUser)).isTrue();
        assertThat(cartItem.belongsToUser(secondUser)).isFalse();
        assertThat(cartItem.isForGame(actionGame)).isTrue();
        assertThat(cartItem.isForGame(rpgGame)).isFalse();
    }


    @Test
    @DisplayName("Should delete cart item by user and game")
    void shouldDeleteCartItemByUserAndGame() {

        // When
        cartItemRepository.deleteByUserAndGame(testUser, actionGame);
        entityManager.flush();

        // Then
        assertThat(cartItemRepository.existsByUserAndGame(testUser, actionGame)).isFalse();

    }

    @Test
    @DisplayName("Should delete all cart items by user")
    void shouldDeleteAllCartItemsByUser() {
        // Given
        assertThat(cartItemRepository.countByUser(testUser)).isEqualTo(2);

        // When
        cartItemRepository.deleteAllByUser(testUser);
        entityManager.flush();

        // Then
        assertThat(cartItemRepository.countByUser(testUser)).isZero();
        assertThat(cartItemRepository.findByUserWithGame(testUser)).isEmpty();
    }


    @Test
    @DisplayName("Should count cart items by user")
    void shouldCountCartItemsByUser() {

        // When & Then
        assertThat(cartItemRepository.countByUser(testUser)).isEqualTo(2);
        assertThat(cartItemRepository.countByUser(secondUser)).isEqualTo(2);
        assertThat(cartItemRepository.countByUser(unknownUser)).isEqualTo(0);

        // After deletion
        cartItemRepository.deleteByUserAndGame(testUser, actionGame);
        entityManager.flush();
        assertThat(cartItemRepository.countByUser(testUser)).isEqualTo(1);
    }

    @Test
    @DisplayName("Should find cart items with game using fetch join")
    void shouldFindCartItemsWithGameUsingFetchJoin() {

        // When
        List<CartItem> itemsWithGames = cartItemRepository.findByUserWithGame(testUser);

        // Then
        assertThat(itemsWithGames).hasSize(2);

        for (CartItem item : itemsWithGames) {
            assertThat(item.getGame()).isNotNull();
            assertThat(item.getGame().getTitle()).isNotNull();
            assertThat(item.getGame().getPrice()).isNotNull();
            assertThat(item.getGame().getCategory()).isNotNull();
        }

        List<CartItem> emptyResult = cartItemRepository.findByUserWithGame(unknownUser);
        assertThat(emptyResult).isEmpty();
    }


    @Test
    @DisplayName("Should find game IDs by user")
    void shouldFindGameIdsByUser() {

        // When
        List<Long> testUserGameIds = cartItemRepository.findGameIdsByUser(testUser);
        List<Long> secondUserGameIds = cartItemRepository.findGameIdsByUser(secondUser);
        List<Long> thirdUserGameIds = cartItemRepository.findGameIdsByUser(unknownUser);

        // Then
        assertThat(testUserGameIds).hasSize(2);
        assertThat(testUserGameIds).containsExactlyInAnyOrder(
                actionGame.getId(),
                rpgGame.getId()
        );

        assertThat(secondUserGameIds).hasSize(2);
        assertThat(secondUserGameIds).containsExactlyInAnyOrder(
                strategyGame.getId(),
                expensiveGame.getId()
        );
        assertThat(thirdUserGameIds).isEmpty();
    }

    @Test
    @DisplayName("Should verify JPA auditing for addedAt timestamp")
    void shouldVerifyJpaAuditingForAddedAtTimestamp() {
        // Given
        LocalDateTime beforeSave = LocalDateTime.now();

        // Create a new game for this test to avoid unique constraint violation
        Game newGame = Game.builder()
                .title("New Test Game")
                .author(testUser)
                .price(new BigDecimal("19.99"))
                .category(entityManager.find(Category.class, actionGame.getCategory().getId()))
                .build();
        newGame = entityManager.persistAndFlush(newGame);

        CartItem cartItem = CartItem.builder()
                .user(unknownUser)
                .game(newGame)
                .build();

        // When
        CartItem saved = cartItemRepository.save(cartItem);
        entityManager.flush();
        LocalDateTime afterSave = LocalDateTime.now();

        // Then
        assertThat(saved.getAddedAt()).isNotNull();
        assertThat(saved.getAddedAt()).isAfterOrEqualTo(beforeSave);
        assertThat(saved.getAddedAt()).isBeforeOrEqualTo(afterSave);

        // Test with another combination
        Game anotherNewGame = Game.builder()
                .title("Another Test Game")
                .author(testUser)
                .price(new BigDecimal("29.99"))
                .category(entityManager.find(Category.class, actionGame.getCategory().getId()))
                .build();
        anotherNewGame = entityManager.persistAndFlush(anotherNewGame);

        CartItem anotherItem = CartItem.builder()
                .user(unknownUser)
                .game(anotherNewGame)
                .build();
        CartItem savedAnother = cartItemRepository.save(anotherItem);
        entityManager.flush();

        assertThat(savedAnother.getAddedAt()).isNotNull();
    }
}