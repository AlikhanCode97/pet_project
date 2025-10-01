package com.example.Games.game;

import com.example.Games.category.Category;
import com.example.Games.config.TestJpaAuditingConfig;
import com.example.Games.user.auth.User;
import com.example.Games.user.role.Role;
import com.example.Games.user.role.RoleType;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@Import(TestJpaAuditingConfig.class)
@ActiveProfiles("test")
@DisplayName("GameRepository Tests")
class GameRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private GameRepository gameRepository;

    private User testUser;
    private User otherUser;
    private Category actionCategory;
    private Category rpgCategory;
    private Game testGame;

    @BeforeEach
    void setUp() {
        // ROLE
        Role userRole = Role.builder()
                .name(RoleType.DEVELOPER)
                .build();
        userRole = entityManager.persistAndFlush(userRole);

        // USERS
        testUser = User.builder()
                .username("gamedev")
                .email("dev@example.com")
                .password("password123")
                .role(userRole)
                .build();
        testUser = entityManager.persistAndFlush(testUser);

        otherUser = User.builder()
                .username("othergamedev")
                .email("other@example.com")
                .password("password123")
                .role(userRole)
                .build();
        otherUser = entityManager.persistAndFlush(otherUser);

        // CATEGORIES
        actionCategory = Category.builder()
                .name("Action")
                .createdBy(testUser)
                .build();
        actionCategory = entityManager.persistAndFlush(actionCategory);

        rpgCategory = Category.builder()
                .name("RPG")
                .createdBy(testUser)
                .build();
        rpgCategory = entityManager.persistAndFlush(rpgCategory);

        // DEFAULT TEST GAME
        testGame = Game.builder()
                .title("Epic Adventure")
                .author(testUser)
                .price(new BigDecimal("29.99"))
                .category(actionCategory)
                .build();
        testGame = entityManager.persistAndFlush(testGame);
    }

    @Test
    @DisplayName("Should not allow duplicate game titles")
    void shouldNotAllowDuplicateTitles() {
        Game duplicate = Game.builder()
                .title("Epic Adventure") // same as testGame
                .author(otherUser)
                .price(new BigDecimal("49.99"))
                .category(actionCategory)
                .build();

        assertThatThrownBy(() -> {
            entityManager.persistAndFlush(duplicate);
        }).isInstanceOf(ConstraintViolationException.class);
    }


    @Test
    @DisplayName("Should find game by title successfully")
    void shouldFindGameByTitle() {
        // When
        Optional<Game> found = gameRepository.findByTitle("Epic Adventure");

        // Then - Game found
        assertThat(found).isPresent();
        assertThat(found.get().getTitle()).isEqualTo("Epic Adventure");
        assertThat(found.get().getAuthor().getId()).isEqualTo(testUser.getId());
        assertThat(found.get().getCategory().getId()).isEqualTo(actionCategory.getId());
        assertThat(found.get().getPrice()).isEqualByComparingTo(new BigDecimal("29.99"));

        // Then - Game not found
        assertThat(gameRepository.findByTitle("Nonexistent Game")).isEmpty();
    }

    @Test
    @DisplayName("Should find games by title containing ignore case")
    void shouldFindGamesByTitleContainingIgnoreCase() {
        // Given
        Game game1 = createAndSaveGame("Adventure Quest", testUser, "19.99", actionCategory);
        Game game2 = createAndSaveGame("The Great Adventure", testUser, "39.99", rpgCategory);
        Game game3 = createAndSaveGame("Space Shooter", otherUser, "49.99", actionCategory);

        List<Game> adventureGames = gameRepository.findByTitleContainingIgnoreCase("adventure");
        assertThat(adventureGames).hasSize(3);
        assertThat(adventureGames)
                .extracting(Game::getTitle)
                .containsExactlyInAnyOrder("Epic Adventure", "Adventure Quest", "The Great Adventure");

        List<Game> upperCaseSearch = gameRepository.findByTitleContainingIgnoreCase("ADVENTURE");
        assertThat(upperCaseSearch).hasSize(3);

        List<Game> questGames = gameRepository.findByTitleContainingIgnoreCase("quest");
        assertThat(questGames).hasSize(1);
        assertThat(questGames.getFirst().getTitle()).isEqualTo("Adventure Quest");

        List<Game> noMatches = gameRepository.findByTitleContainingIgnoreCase("nonexistent");
        assertThat(noMatches).isEmpty();

        List<Game> singleChar = gameRepository.findByTitleContainingIgnoreCase("Q");
        assertThat(singleChar).hasSize(1);
        assertThat(singleChar.getFirst().getTitle()).isEqualTo("Adventure Quest");
    }

    @Test
    @DisplayName("Should find games by author username")
    void shouldFindGamesByAuthorUsername() {
        // Given
        Game game1 = createAndSaveGame("User Game 1", testUser, "19.99", actionCategory);
        Game game2 = createAndSaveGame("User Game 2", otherUser, "29.99", rpgCategory);

        // When
        List<Game> gameDevGames = gameRepository.findByAuthor_Username("gamedev");
        List<Game> otherGames = gameRepository.findByAuthor_Username("othergamedev");

        // Then
        assertThat(gameDevGames).hasSize(2);
        assertThat(gameDevGames)
                .extracting(Game::getTitle)
                .containsExactlyInAnyOrder("Epic Adventure", "User Game 1");

        assertThat(otherGames).hasSize(1);
        assertThat(otherGames.getFirst().getTitle()).isEqualTo("User Game 2");

        // Test non-existent username
        List<Game> nonExistentGames = gameRepository.findByAuthor_Username("nonexistent");
        assertThat(nonExistentGames).isEmpty();
    }

    @Test
    @DisplayName("Should find games in price range")
    void shouldFindGamesInPriceRange() {
        // Given
        Game cheapGame = createAndSaveGame("Cheap Game", testUser, "9.99", actionCategory);
        Game midGame = createAndSaveGame("Mid Price Game", testUser, "29.99", rpgCategory);
        Game expensiveGame = createAndSaveGame("Expensive Game", otherUser, "59.99", actionCategory);
        Game veryExpensiveGame = createAndSaveGame("Very Expensive Game", otherUser, "79.99", rpgCategory);

        // When & Then
        List<Game> budgetGames = gameRepository.findGamesInPriceRange(
                new BigDecimal("10.00"), new BigDecimal("30.00"));
        assertThat(budgetGames).hasSize(2); // testGame and midGame
        assertThat(budgetGames)
                .extracting(Game::getTitle)
                .containsExactlyInAnyOrder("Epic Adventure", "Mid Price Game");

        List<Game> allGames = gameRepository.findGamesInPriceRange(
                new BigDecimal("0.01"), new BigDecimal("100.00"));
        assertThat(allGames).hasSize(5);

        List<Game> noGamesInRange = gameRepository.findGamesInPriceRange(
                new BigDecimal("100.00"), new BigDecimal("200.00"));
        assertThat(noGamesInRange).isEmpty();

        List<Game> exactBoundary = gameRepository.findGamesInPriceRange(
                new BigDecimal("29.99"), new BigDecimal("29.99"));
        assertThat(exactBoundary).hasSize(2);
        assertThat(exactBoundary)
                .extracting(Game::getTitle)
                .containsExactlyInAnyOrder("Epic Adventure", "Mid Price Game");
    }

    @Test
    @DisplayName("Should sort games by price ascending")
    void shouldSortGamesByPriceAscending() {
        // Given
        Game game1 = createAndSaveGame("Game 1", testUser, "39.99", actionCategory);
        Game game2 = createAndSaveGame("Game 2", testUser, "19.99", rpgCategory);
        Game game3 = createAndSaveGame("Game 3", otherUser, "59.99", actionCategory);

        // When
        List<Game> ascending = gameRepository.findAllByOrderByPriceAsc();

        // Then
        assertThat(ascending).hasSize(4);
        assertThat(ascending)
                .extracting(Game::getPrice)
                .isSortedAccordingTo(BigDecimal::compareTo);
        assertThat(ascending.getFirst().getPrice()).isEqualByComparingTo(new BigDecimal("19.99"));
        assertThat(ascending.getLast().getPrice()).isEqualByComparingTo(new BigDecimal("59.99"));
    }

    @Test
    @DisplayName("Should sort games by price descending")
    void shouldSortGamesByPriceDescending() {
        // Given
        Game game1 = createAndSaveGame("Game 1", testUser, "39.99", actionCategory);
        Game game2 = createAndSaveGame("Game 2", testUser, "19.99", rpgCategory);
        Game game3 = createAndSaveGame("Game 3", otherUser, "59.99", actionCategory);

        // When
        List<Game> descending = gameRepository.findAllByOrderByPriceDesc();

        // Then
        assertThat(descending).hasSize(4);
        assertThat(descending.getFirst().getPrice()).isEqualByComparingTo(new BigDecimal("59.99"));
        assertThat(descending.getLast().getPrice()).isEqualByComparingTo(new BigDecimal("19.99"));
    }

    @Test
    @DisplayName("Should find games by category with pagination")
    void shouldFindGamesByCategoryWithPagination() {
        createAndSaveGame("Game 1", testUser, "39.99", actionCategory);
        createAndSaveGame("Game 2", testUser, "19.99", rpgCategory);
        createAndSaveGame("Game 3", otherUser, "59.99", actionCategory);

        // When & Then - First page
        Pageable firstPage = PageRequest.of(0, 2);
        Page<Game> page1 = gameRepository.findByCategoryId(actionCategory.getId(), firstPage);

        assertThat(page1.getContent()).hasSize(2);
        assertThat(page1.getTotalElements()).isEqualTo(3);
        assertThat(page1.getTotalPages()).isEqualTo(2);
        assertThat(page1.getNumber()).isEqualTo(0);
        assertThat(page1.hasNext()).isTrue();

        //second page
        Pageable secondPage = PageRequest.of(1, 2);
        Page<Game> page2 = gameRepository.findByCategoryId(actionCategory.getId(), secondPage);

        assertThat(page2.getContent()).hasSize(1);
        assertThat(page2.hasNext()).isFalse();
        assertThat(page2.hasPrevious()).isTrue();

        // Test RPG category
        Page<Game> rpgPage = gameRepository.findByCategoryId(rpgCategory.getId(), firstPage);
        assertThat(rpgPage.getContent()).hasSize(1);
        assertThat(rpgPage.getTotalElements()).isEqualTo(1);

        // Test non-existent category
        Page<Game> nonExistent = gameRepository.findByCategoryId(999L, firstPage);
        assertThat(nonExistent.getContent()).isEmpty();
    }

    @Test
    @DisplayName("Should find game by ID with author using fetch join")
    void shouldFindGameByIdWithAuthor() {
        // When
        Optional<Game> found = gameRepository.findByIdWithAuthor(testGame.getId());

        // Then
        assertThat(found).isPresent();
        Game game = found.get();
        assertThat(game.getId()).isEqualTo(testGame.getId());
        assertThat(game.getTitle()).isEqualTo("Epic Adventure");
        
        // Verify author is fetched (not lazy loaded)
        assertThat(game.getAuthor()).isNotNull();
        assertThat(game.getAuthor().getUsername()).isEqualTo("gamedev");
        assertThat(game.getAuthor().getEmail()).isEqualTo("dev@example.com");

        // Test non-existent ID
        Optional<Game> notFound = gameRepository.findByIdWithAuthor(999L);
        assertThat(notFound).isEmpty();
    }

    @Test
    @DisplayName("Should find category ID by game ID")
    void shouldFindCategoryIdByGameId() {

        // When & Then - Game with category
        Optional<Long> categoryId = gameRepository.findCategoryIdByGameId(testGame.getId());
        assertThat(categoryId).isPresent();
        assertThat(categoryId.get()).isEqualTo(actionCategory.getId());

        // Non-existent game
        Optional<Long> nonExistent = gameRepository.findCategoryIdByGameId(999L);
        assertThat(nonExistent).isEmpty();
    }

    @Test
    @DisplayName("Should find all games by IDs with authors using fetch join")
    void shouldFindAllByIdWithAuthor() {
        // Given
        Game game1 = createAndSaveGame("Game 1", testUser, "19.99", actionCategory);
        Game game2 = createAndSaveGame("Game 2", otherUser, "29.99", rpgCategory);
        Game game3 = createAndSaveGame("Game 3", testUser, "39.99", actionCategory);

        List<Long> gameIds = Arrays.asList(testGame.getId(), game1.getId(), game2.getId(), game3.getId());

        // When
        List<Game> games = gameRepository.findAllByIdWithAuthor(gameIds);

        // Then
        assertThat(games).hasSize(4);
        assertThat(games)
                .extracting(Game::getId)
                .containsExactlyInAnyOrderElementsOf(gameIds);

        assertThat(games).allMatch(game -> game.getAuthor() != null);
        assertThat(games).extracting(game -> game.getAuthor().getUsername())
                .containsExactlyInAnyOrder("gamedev", "gamedev", "othergamedev", "gamedev");

        // Test with empty list
        List<Game> emptyResult = gameRepository.findAllByIdWithAuthor(List.of());
        assertThat(emptyResult).isEmpty();

        // Test with non-existent IDs
        List<Game> nonExistent = gameRepository.findAllByIdWithAuthor(Arrays.asList(999L, 1000L));
        assertThat(nonExistent).isEmpty();

    }

    @Test
    @DisplayName("Should verify JPA auditing works correctly")
    void shouldVerifyJpaAuditingWorks() {
        // Given
        LocalDateTime beforeCreation = LocalDateTime.now();
        
        Game newGame = Game.builder()
                .title("Auditing Test Game")
                .author(testUser)
                .price(new BigDecimal("49.99"))
                .category(actionCategory)
                .build();

        // When
        Game savedGame = gameRepository.save(newGame);
        entityManager.flush();
        LocalDateTime afterCreation = LocalDateTime.now();

        // Then
        assertThat(savedGame.getCreatedAt()).isNotNull();
        assertThat(savedGame.getUpdatedAt()).isNotNull();
        assertThat(savedGame.getCreatedAt()).isAfterOrEqualTo(beforeCreation);
        assertThat(savedGame.getCreatedAt()).isBeforeOrEqualTo(afterCreation);
        assertThat(savedGame.getUpdatedAt()).isEqualTo(savedGame.getCreatedAt());

        savedGame.updatePrice(new BigDecimal("39.99"));
        Game updatedGame = gameRepository.save(savedGame);
        entityManager.flush();

        assertThat(updatedGame.getUpdatedAt()).isAfter(updatedGame.getCreatedAt());
    }

    private Game createAndSaveGame(String title, User author, String price, Category category) {
        Game game = Game.builder()
                .title(title)
                .author(author)
                .price(new BigDecimal(price))
                .category(category)
                .build();
        return entityManager.persistAndFlush(game);
    }
}