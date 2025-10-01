package com.example.Games.purchase;

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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@Import(TestJpaAuditingConfig.class)
@ActiveProfiles("test")
@DisplayName("PurchaseRepository Tests")
class PurchaseRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private PurchaseRepository purchaseRepository;

    private User buyer1;
    private User buyer2;
    private User developer1;
    private User developer2;
    private Game game1;
    private Game game2;
    private Game game3;
    private Game game4;
    private Category actionCategory;
    private Category rpgCategory;

    @BeforeEach
    void setUp() {
        Role userRole = createRole(RoleType.USER);
        Role developerRole = createRole(RoleType.DEVELOPER);

        developer1 = createUser("dev1", "dev1@example.com", developerRole);
        developer2 = createUser("dev2", "dev2@example.com", developerRole);

        // Setup buyers
        buyer1 = createUser("buyer1", "buyer1@example.com", userRole);
        buyer2 = createUser("buyer2", "buyer2@example.com", userRole);


        // Setup categories
        actionCategory = createCategory("Action",developer1);
        rpgCategory = createCategory("RPG",developer1);

        // Setup games - 2 from developer1, 2 from developer2
        game1 = createGame("Action Game 1", developer1, new BigDecimal("29.99"), actionCategory);
        game2 = createGame("RPG Game 1", developer1, new BigDecimal("39.99"), rpgCategory);
        game3 = createGame("Action Game 2", developer2, new BigDecimal("49.99"), actionCategory);
        game4 = createGame("RPG Game 2", developer2, new BigDecimal("59.99"), rpgCategory);

        // Setup purchase history
        createPurchase(buyer1, game1, new BigDecimal("29.99"));
        createPurchase(buyer1, game2, new BigDecimal("39.99"));
        createPurchase(buyer1, game3, new BigDecimal("49.99"));
        createPurchase(buyer2, game2, new BigDecimal("35.99"));
        createPurchase(buyer2, game4, new BigDecimal("59.99"));
        entityManager.flush();
    }

    @Test
    @DisplayName("Should lazily load game when not using fetch join")
    void shouldLazilyLoadGameWhenNotUsingFetchJoin() {
        PurchaseHistory history = purchaseRepository.findAll().get(0);
        assertThat(history.getGame()).isNotNull(); // triggers lazy load
        assertThat(history.getGame().getTitle()).isNotNull();
    }


    @Test
    @DisplayName("Should check if user owns a game")
    void shouldCheckIfUserOwnsGame() {
        // When & Then - Buyer1's ownership
        assertThat(purchaseRepository.existsByUserIdAndGameId(buyer1.getId(), game1.getId())).isTrue();
        assertThat(purchaseRepository.existsByUserIdAndGameId(buyer1.getId(), game2.getId())).isTrue();
        assertThat(purchaseRepository.existsByUserIdAndGameId(buyer1.getId(), game3.getId())).isTrue();
        assertThat(purchaseRepository.existsByUserIdAndGameId(buyer1.getId(), game4.getId())).isFalse();

        // Buyer2's ownership
        assertThat(purchaseRepository.existsByUserIdAndGameId(buyer2.getId(), game1.getId())).isFalse();
        assertThat(purchaseRepository.existsByUserIdAndGameId(buyer2.getId(), game2.getId())).isTrue();
        assertThat(purchaseRepository.existsByUserIdAndGameId(buyer2.getId(), game3.getId())).isFalse();
        assertThat(purchaseRepository.existsByUserIdAndGameId(buyer2.getId(), game4.getId())).isTrue();

        // Non-existent user
        assertThat(purchaseRepository.existsByUserIdAndGameId(999L, game1.getId())).isFalse();
        
        // Non-existent game
        assertThat(purchaseRepository.existsByUserIdAndGameId(buyer1.getId(), 999L)).isFalse();
    }

    @Test
    @DisplayName("Should find owned game IDs from a list")
    void shouldFindOwnedGameIdsFromList() {
        // Given
        List<Long> gameIdsToCheck = Arrays.asList(
                game1.getId(), game2.getId(), game3.getId(), game4.getId()
        );

        // When - Check for buyer1
        List<Long> buyer1OwnedIds = purchaseRepository.findOwnedGameIds(
                buyer1.getId(), gameIdsToCheck
        );

        // Then
        assertThat(buyer1OwnedIds).hasSize(3);
        assertThat(buyer1OwnedIds).containsExactlyInAnyOrder(
                game1.getId(), game2.getId(), game3.getId()
        );

        // When - Check for buyer2
        List<Long> buyer2OwnedIds = purchaseRepository.findOwnedGameIds(
                buyer2.getId(), gameIdsToCheck
        );

        // Then
        assertThat(buyer2OwnedIds).hasSize(2);
        assertThat(buyer2OwnedIds).containsExactlyInAnyOrder(
                game2.getId(), game4.getId()
        );

        // When - Check with partial list
        List<Long> partialCheck = Arrays.asList(game1.getId(), game4.getId());
        List<Long> buyer1PartialOwned = purchaseRepository.findOwnedGameIds(
                buyer1.getId(), partialCheck
        );

        // Then
        assertThat(buyer1PartialOwned).hasSize(1);
        assertThat(buyer1PartialOwned).contains(game1.getId());

        // When - Check with empty list
        List<Long> emptyCheck = purchaseRepository.findOwnedGameIds(
                buyer1.getId(), Arrays.asList()
        );
        assertThat(emptyCheck).isEmpty();

        // When - User owns none
        List<Long> developerOwned = purchaseRepository.findOwnedGameIds(
                developer1.getId(), gameIdsToCheck
        );
        assertThat(developerOwned).isEmpty();
    }

    @Test
    @DisplayName("Should find purchases by user with game and author (List)")
    void shouldFindPurchasesByUserWithGameAndAuthorList() {
        // When - Get buyer1's purchases
        List<PurchaseHistory> buyer1Purchases = purchaseRepository.findByUserWithGameAndAuthor(buyer1);

        // Then
        assertThat(buyer1Purchases).hasSize(3);
        
        // Verify ordering (newest first)
        for (int i = 0; i < buyer1Purchases.size() - 1; i++) {
            assertThat(buyer1Purchases.get(i).getPurchasedAt())
                    .isAfterOrEqualTo(buyer1Purchases.get(i + 1).getPurchasedAt());
        }

        // Verify relationships are eagerly loaded
        for (PurchaseHistory purchase : buyer1Purchases) {
            assertThat(purchase.getGame()).isNotNull();
            assertThat(purchase.getGame().getTitle()).isNotNull();
            assertThat(purchase.getGame().getAuthor()).isNotNull();
            assertThat(purchase.getGame().getAuthor().getUsername()).isNotNull();
        }

        // Verify correct games
        assertThat(buyer1Purchases)
                .extracting(p -> p.getGame().getId())
                .containsExactlyInAnyOrder(game1.getId(), game2.getId(), game3.getId());

        // When - Get buyer2's purchases
        List<PurchaseHistory> buyer2Purchases = purchaseRepository.findByUserWithGameAndAuthor(buyer2);

        // Then
        assertThat(buyer2Purchases).hasSize(2);
        assertThat(buyer2Purchases)
                .extracting(p -> p.getGame().getId())
                .containsExactlyInAnyOrder(game2.getId(), game4.getId());

        // When - User with no purchases
        List<PurchaseHistory> noPurchases = purchaseRepository.findByUserWithGameAndAuthor(developer1);
        assertThat(noPurchases).isEmpty();
    }

    @Test
    @DisplayName("Should find purchases by user with game and author (Paginated)")
    void shouldFindPurchasesByUserWithGameAndAuthorPaginated() {
        // Given
        Pageable firstPage = PageRequest.of(0, 2);
        Pageable secondPage = PageRequest.of(1, 2);

        // When - Get buyer1's first page
        Page<PurchaseHistory> buyer1Page1 = purchaseRepository.findByUserWithGameAndAuthor(
                buyer1, firstPage
        );

        // Then
        assertThat(buyer1Page1.getContent()).hasSize(2);
        assertThat(buyer1Page1.getTotalElements()).isEqualTo(3);
        assertThat(buyer1Page1.getTotalPages()).isEqualTo(2);
        assertThat(buyer1Page1.hasNext()).isTrue();
        assertThat(buyer1Page1.hasPrevious()).isFalse();

        // Verify relationships are loaded
        for (PurchaseHistory purchase : buyer1Page1.getContent()) {
            assertThat(purchase.getGame()).isNotNull();
            assertThat(purchase.getGame().getAuthor()).isNotNull();
        }

        // When - Get buyer1's second page
        Page<PurchaseHistory> buyer1Page2 = purchaseRepository.findByUserWithGameAndAuthor(
                buyer1, secondPage
        );

        // Then
        assertThat(buyer1Page2.getContent()).hasSize(1);
        assertThat(buyer1Page2.hasNext()).isFalse();
        assertThat(buyer1Page2.hasPrevious()).isTrue();

        // When - Buyer2's purchases with larger page size
        Pageable largePage = PageRequest.of(0, 10);
        Page<PurchaseHistory> buyer2Page = purchaseRepository.findByUserWithGameAndAuthor(
                buyer2, largePage
        );

        // Then
        assertThat(buyer2Page.getContent()).hasSize(2);
        assertThat(buyer2Page.getTotalElements()).isEqualTo(2);
        assertThat(buyer2Page.getTotalPages()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should find purchases by user ID with game and author")
    void shouldFindPurchasesByUserIdWithGameAndAuthor() {
        // When
        List<PurchaseHistory> buyer1Purchases = purchaseRepository.findByUserIdWithGameAndAuthor(
                buyer1.getId()
        );

        // Then
        assertThat(buyer1Purchases).hasSize(3);
        
        // Verify ordering (newest first)
        assertThat(buyer1Purchases).isSortedAccordingTo(
                (p1, p2) -> p2.getPurchasedAt().compareTo(p1.getPurchasedAt())
        );

        // Verify all purchases belong to buyer1
        assertThat(buyer1Purchases)
                .allMatch(p -> p.getUser().getId().equals(buyer1.getId()));

        // When - Non-existent user
        List<PurchaseHistory> noPurchases = purchaseRepository.findByUserIdWithGameAndAuthor(999L);
        assertThat(noPurchases).isEmpty();
    }

    @Test
    @DisplayName("Should find purchases by game ID with game and author")
    void shouldFindPurchasesByGameIdWithGameAndAuthor() {
        // When - Game2 purchased by both buyers
        List<PurchaseHistory> game2Purchases = purchaseRepository.findByGameIdWithGameAndAuthor(
                game2.getId()
        );

        // Then
        assertThat(game2Purchases).hasSize(2);
        assertThat(game2Purchases)
                .extracting(p -> p.getUser().getId())
                .containsExactlyInAnyOrder(buyer1.getId(), buyer2.getId());

        // Verify different purchase prices (one was on sale)
        PurchaseHistory buyer1Purchase = game2Purchases.stream()
                .filter(p -> p.getUser().getId().equals(buyer1.getId()))
                .findFirst().orElse(null);
        PurchaseHistory buyer2Purchase = game2Purchases.stream()
                .filter(p -> p.getUser().getId().equals(buyer2.getId()))
                .findFirst().orElse(null);
        
        assertThat(buyer1Purchase).isNotNull();
        assertThat(buyer1Purchase.getPurchasePrice()).isEqualTo(new BigDecimal("39.99"));
        assertThat(buyer2Purchase).isNotNull();
        assertThat(buyer2Purchase.getPurchasePrice()).isEqualTo(new BigDecimal("35.99"));

        // When - Game1 purchased only by buyer1
        List<PurchaseHistory> game1Purchases = purchaseRepository.findByGameIdWithGameAndAuthor(
                game1.getId()
        );

        // Then
        assertThat(game1Purchases).hasSize(1);
        assertThat(game1Purchases.get(0).getUser().getId()).isEqualTo(buyer1.getId());

        // When - Game with no purchases
        Game unpurchasedGame = Game.builder()
                .title("Unpurchased Game")
                .author(developer1)
                .price(new BigDecimal("99.99"))
                .category(actionCategory)
                .build();
        unpurchasedGame = entityManager.persistAndFlush(unpurchasedGame);

        List<PurchaseHistory> noPurchases = purchaseRepository.findByGameIdWithGameAndAuthor(
                unpurchasedGame.getId()
        );
        assertThat(noPurchases).isEmpty();
    }

    @Test
    @DisplayName("Should find sales by developer ID with game")
    void shouldFindSalesByDeveloperIdWithGame() {
        // When - Developer1's sales (game1 and game2)
        List<PurchaseHistory> dev1Sales = purchaseRepository.findSalesByDeveloperIdWithGame(
                developer1.getId()
        );

        // Then
        assertThat(dev1Sales).hasSize(3); // game1: 1 sale, game2: 2 sales
        
        // Verify all sales are for developer1's games
        assertThat(dev1Sales)
                .allMatch(p -> p.getGame().getAuthor().getId().equals(developer1.getId()));
        
        // Verify game distribution
        long game1Sales = dev1Sales.stream()
                .filter(p -> p.getGame().getId().equals(game1.getId()))
                .count();
        long game2Sales = dev1Sales.stream()
                .filter(p -> p.getGame().getId().equals(game2.getId()))
                .count();
        
        assertThat(game1Sales).isEqualTo(1);
        assertThat(game2Sales).isEqualTo(2);

        // When - Developer2's sales (game3 and game4)
        List<PurchaseHistory> dev2Sales = purchaseRepository.findSalesByDeveloperIdWithGame(
                developer2.getId()
        );

        // Then
        assertThat(dev2Sales).hasSize(2); // game3: 1 sale, game4: 1 sale
        assertThat(dev2Sales)
                .extracting(p -> p.getGame().getId())
                .containsExactlyInAnyOrder(game3.getId(), game4.getId());

        // When - Developer with no sales
        User newDeveloper = User.builder()
                .username("newdev")
                .email("newdev@example.com")
                .password("password123")
                .role(developer1.getRole())
                .build();
        newDeveloper = entityManager.persistAndFlush(newDeveloper);

        List<PurchaseHistory> noSales = purchaseRepository.findSalesByDeveloperIdWithGame(
                newDeveloper.getId()
        );
        assertThat(noSales).isEmpty();
    }

    @Test
    @DisplayName("Should calculate total revenue for developer")
    void shouldCalculateTotalRevenueForDeveloper() {
        // When - Calculate developer1's revenue
        Optional<BigDecimal> dev1Revenue = purchaseRepository.calculateTotalRevenueForDeveloper(
                developer1.getId()
        );

        // Then
        assertThat(dev1Revenue).isPresent();
        // game1: 29.99 + game2: (39.99 + 35.99) = 105.97
        assertThat(dev1Revenue.get()).isEqualByComparingTo(new BigDecimal("105.97"));

        // When - Calculate developer2's revenue
        Optional<BigDecimal> dev2Revenue = purchaseRepository.calculateTotalRevenueForDeveloper(
                developer2.getId()
        );

        // Then
        assertThat(dev2Revenue).isPresent();
        // game3: 49.99 + game4: 59.99 = 109.98
        assertThat(dev2Revenue.get()).isEqualByComparingTo(new BigDecimal("109.98"));

        // When - Developer with no sales
        User newDeveloper = User.builder()
                .username("nosales")
                .email("nosales@example.com")
                .password("password123")
                .role(developer1.getRole())
                .build();
        newDeveloper = entityManager.persistAndFlush(newDeveloper);

        Optional<BigDecimal> noRevenue = purchaseRepository.calculateTotalRevenueForDeveloper(
                newDeveloper.getId()
        );

        // Then - Should return empty Optional (SUM returns null for no rows)
        assertThat(noRevenue).isEmpty();
    }

    @Test
    @DisplayName("Should handle unique constraint on user-game combination")
    void shouldHandleUniqueConstraintOnUserGameCombination() {
        // Given - buyer1 already owns game1
        assertThat(purchaseRepository.existsByUserIdAndGameId(buyer1.getId(), game1.getId())).isTrue();

        // When & Then - Attempting to create duplicate purchase should fail
        PurchaseHistory duplicatePurchase = PurchaseHistory.builder()
                .user(buyer1)
                .game(game1)
                .purchasePrice(new BigDecimal("29.99"))
                .build();

        assertThatThrownBy(() ->
                purchaseRepository.saveAndFlush(duplicatePurchase)
        ).isInstanceOf(DataIntegrityViolationException.class);

    }

    @Test
    @DisplayName("Should verify JPA auditing for purchased_at timestamp")
    void shouldVerifyJpaAuditingForPurchasedAtTimestamp() {
        // Given
        LocalDateTime beforeSave = LocalDateTime.now();
        
        User newBuyer = User.builder()
                .username("newbuyer")
                .email("newbuyer@example.com")
                .password("password123")
                .role(buyer1.getRole())
                .build();
        newBuyer = entityManager.persistAndFlush(newBuyer);

        PurchaseHistory newPurchase = PurchaseHistory.builder()
                .user(newBuyer)
                .game(game4)
                .purchasePrice(new BigDecimal("54.99"))
                .build();

        // When
        PurchaseHistory saved = purchaseRepository.saveAndFlush(newPurchase);
        LocalDateTime afterSave = LocalDateTime.now();

        // Then
        assertThat(saved.getPurchasedAt()).isNotNull();
        assertThat(saved.getPurchasedAt()).isAfterOrEqualTo(beforeSave);
        assertThat(saved.getPurchasedAt()).isBeforeOrEqualTo(afterSave);
    }

    // Helper method
    private void createPurchase(User user, Game game, BigDecimal price) {
        PurchaseHistory purchase = PurchaseHistory.builder()
                .user(user)
                .game(game)
                .purchasePrice(price)
                .build();
        purchaseRepository.saveAndFlush(purchase);
    }
    private Role createRole(RoleType type) {
        Role role = Role.builder().name(type).build();
        return entityManager.persistAndFlush(role);
    }

    private User createUser(String username, String email, Role role) {
        User user = User.builder()
                .username(username)
                .email(email)
                .password("password123") // default test password
                .role(role)
                .build();
        return entityManager.persistAndFlush(user);
    }

    private Category createCategory(String name, User user) {
        Category category = Category.builder().name(name).createdBy(user).build();
        return entityManager.persistAndFlush(category);
    }

    private Game createGame(String title, User author, BigDecimal price, Category category) {
        Game game = Game.builder()
                .title(title)
                .author(author)
                .price(price)
                .category(category)
                .build();
        return entityManager.persistAndFlush(game);
    }
}
