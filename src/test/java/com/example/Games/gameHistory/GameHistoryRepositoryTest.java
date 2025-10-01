package com.example.Games.gameHistory;

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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@Import(TestJpaAuditingConfig.class)
@ActiveProfiles("test")
@DisplayName("GameHistoryRepository Tests")
class GameHistoryRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private GameHistoryRepository historyRepository;

    private User developerUser;
    private User purchaserUser;
    private User adminUser;
    private Game testGame;
    private Game secondGame;
    private Category actionCategory;
    private Category rpgCategory;

    @BeforeEach
    void setUp() {
        // Setup roles
        Role developerRole = Role.builder()
                .name(RoleType.DEVELOPER)
                .build();
        developerRole = entityManager.persistAndFlush(developerRole);

        Role userRole = Role.builder()
                .name(RoleType.USER)
                .build();
        userRole = entityManager.persistAndFlush(userRole);

        Role adminRole = Role.builder()
                .name(RoleType.ADMIN)
                .build();
        adminRole = entityManager.persistAndFlush(adminRole);

        // Setup users
        developerUser = User.builder()
                .username("gamedev")
                .email("dev@example.com")
                .password("password123")
                .role(developerRole)
                .build();
        developerUser = entityManager.persistAndFlush(developerUser);

        purchaserUser = User.builder()
                .username("purchaser")
                .email("purchaser@example.com")
                .password("password123")
                .role(userRole)
                .build();
        purchaserUser = entityManager.persistAndFlush(purchaserUser);

        adminUser = User.builder()
                .username("admin")
                .email("admin@example.com")
                .password("password123")
                .role(adminRole)
                .build();
        adminUser = entityManager.persistAndFlush(adminUser);

        // Setup categories
        actionCategory = Category.builder()
                .name("Action")
                .createdBy(developerUser)
                .build();
        actionCategory = entityManager.persistAndFlush(actionCategory);

        rpgCategory = Category.builder()
                .name("RPG")
                .createdBy(developerUser)
                .build();
        rpgCategory = entityManager.persistAndFlush(rpgCategory);

        // Setup games
        testGame = Game.builder()
                .title("Epic Adventure")
                .author(developerUser)
                .price(new BigDecimal("29.99"))
                .category(actionCategory)
                .build();
        testGame = entityManager.persistAndFlush(testGame);

        secondGame = Game.builder()
                .title("Space Quest")
                .author(developerUser)
                .price(new BigDecimal("39.99"))
                .category(rpgCategory)
                .build();
        secondGame = entityManager.persistAndFlush(secondGame);
    }

    @Test
    @DisplayName("Should find history with relations using fetch join for game")
    void shouldFindHistoryWithRelationsUsingFetchJoinForGame() {
        // Given
        createHistoryEntry(testGame, ActionType.CREATE, developerUser, "Created");
        createHistoryEntry(testGame, ActionType.UPDATE, developerUser, "Updated");
        createHistoryEntry(secondGame, ActionType.CREATE, developerUser, "Created second");

        // When - Query with fetch join for game
        Pageable pageable = PageRequest.of(0, 10);
        Page<GameHistory> historyWithRelations = historyRepository.findByGameIdWithRelations(
                testGame.getId(), pageable);

        // Then
        assertThat(historyWithRelations.getContent()).hasSize(2);
        assertThat(historyWithRelations.getTotalElements()).isEqualTo(2);

        // Verify relations are eagerly loaded (no lazy loading)
        for (GameHistory history : historyWithRelations.getContent()) {
            // These should not trigger additional queries
            assertThat(history.getGame()).isNotNull();
            assertThat(history.getGame().getTitle()).isNotNull();
            assertThat(history.getChangedBy()).isNotNull();
            assertThat(history.getChangedBy().getUsername()).isNotNull();
        }

        // Verify ordering (newest first)
        List<GameHistory> content = historyWithRelations.getContent();
        for (int i = 0; i < content.size() - 1; i++) {
            assertThat(content.get(i).getChangedAt())
                    .isAfterOrEqualTo(content.get(i + 1).getChangedAt());
        }
    }

    @Test
    @DisplayName("Should find history with relations using fetch join for user")
    void shouldFindHistoryWithRelationsUsingFetchJoinForUser() {
        // Given
        createHistoryEntry(testGame, ActionType.CREATE, developerUser, "Created");
        createHistoryEntry(testGame, ActionType.UPDATE, developerUser, "Updated");
        createHistoryEntry(secondGame, ActionType.CREATE, developerUser, "Created second");
        createHistoryEntry(testGame, ActionType.PURCHASE, purchaserUser, "Purchased");

        // When - Query with fetch join for user
        Pageable pageable = PageRequest.of(0, 10);
        Page<GameHistory> userHistoryWithRelations = historyRepository.findByChangedByIdWithRelations(
                developerUser.getId(), pageable);

        // Then
        assertThat(userHistoryWithRelations.getContent()).hasSize(3);
        assertThat(userHistoryWithRelations.getTotalElements()).isEqualTo(3);
        assertThat(userHistoryWithRelations.getContent())
                .allMatch(h -> h.getGame() != null && h.getChangedBy() != null);
        
        // Verify all belong to the developer
        assertThat(userHistoryWithRelations.getContent())
                .allMatch(h -> h.getChangedBy().getId().equals(developerUser.getId()));
    }

    @Test
    @DisplayName("Should count user actions correctly by action type")
    void shouldCountUserActionsByActionType() {
        // Given - Create various actions
        createHistoryEntry(testGame, ActionType.CREATE, developerUser, "Create 1");
        createHistoryEntry(secondGame, ActionType.CREATE, developerUser, "Create 2");
        createHistoryEntry(testGame, ActionType.UPDATE, developerUser, "Update 1");
        createHistoryEntry(testGame, ActionType.UPDATE, developerUser, "Update 2");
        createHistoryEntry(secondGame, ActionType.UPDATE, developerUser, "Update 3");
        createHistoryEntry(testGame, ActionType.DELETE, developerUser, "Delete 1");
        createHistoryEntry(testGame, ActionType.PURCHASE, purchaserUser, "Purchase 1");

        // When & Then - Count by action type
        long devCreateCount = historyRepository.countByUserAndActionType(
                developerUser.getId(), ActionType.CREATE);
        assertThat(devCreateCount).isEqualTo(2);

        long devUpdateCount = historyRepository.countByUserAndActionType(
                developerUser.getId(), ActionType.UPDATE);
        assertThat(devUpdateCount).isEqualTo(3);

        long devDeleteCount = historyRepository.countByUserAndActionType(
                developerUser.getId(), ActionType.DELETE);
        assertThat(devDeleteCount).isEqualTo(1);

        long devPurchaseCount = historyRepository.countByUserAndActionType(
                developerUser.getId(), ActionType.PURCHASE);
        assertThat(devPurchaseCount).isEqualTo(0);

        long purchaserPurchaseCount = historyRepository.countByUserAndActionType(
                purchaserUser.getId(), ActionType.PURCHASE);
        assertThat(purchaserPurchaseCount).isEqualTo(1);
    }

    @Test
    @DisplayName("Should count total user actions")
    void shouldCountTotalUserActions() {
        // Given
        createHistoryEntry(testGame, ActionType.CREATE, developerUser, "Create 1");
        createHistoryEntry(secondGame, ActionType.CREATE, developerUser, "Create 2");
        createHistoryEntry(testGame, ActionType.UPDATE, developerUser, "Update 1");
        createHistoryEntry(testGame, ActionType.UPDATE, developerUser, "Update 2");
        createHistoryEntry(secondGame, ActionType.UPDATE, developerUser, "Update 3");
        createHistoryEntry(testGame, ActionType.DELETE, developerUser, "Delete 1");
        createHistoryEntry(testGame, ActionType.PURCHASE, purchaserUser, "Purchase 1");

        // When & Then
        long devTotalCount = historyRepository.countByUserId(developerUser.getId());
        assertThat(devTotalCount).isEqualTo(6);

        // Count for purchaser
        long purchaserTotalCount = historyRepository.countByUserId(purchaserUser.getId());
        assertThat(purchaserTotalCount).isEqualTo(1);

        // Count for user with no actions
        long adminCount = historyRepository.countByUserId(adminUser.getId());
        assertThat(adminCount).isEqualTo(0);
    }

    @Test
    @DisplayName("Should find top 5 recent activities by user")
    void shouldFindTop5RecentActivitiesByUser() {
        // Given - Create activities with delays
        List<GameHistory> activities = new ArrayList<>();
        for (int i = 1; i <= 7; i++) {
            GameHistory history = createHistoryEntry(
                    i % 2 == 0 ? testGame : secondGame,
                    ActionType.UPDATE,
                    developerUser,
                    "Activity " + i
            );
            activities.add(history);
            entityManager.flush();

            // Small delay to ensure different timestamps
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                // Ignore
            }
        }

        // When - Get top 5 recent activities
        List<GameHistory> recentActivity = historyRepository.findTop5ByChangedByIdOrderByChangedAtDesc(
                developerUser.getId());

        // Then
        assertThat(recentActivity).hasSize(5); // Only 5 despite having 7 total

        // Verify ordering (newest first)
        for (int i = 0; i < recentActivity.size() - 1; i++) {
            assertThat(recentActivity.get(i).getChangedAt())
                    .isAfterOrEqualTo(recentActivity.get(i + 1).getChangedAt());
        }

        // Verify all belong to the developer
        assertThat(recentActivity)
                .allMatch(h -> h.getChangedBy().getId().equals(developerUser.getId()));

        // Test with user having fewer than 5 activities
        createHistoryEntry(testGame, ActionType.PURCHASE, purchaserUser, "Purchase 1");
        createHistoryEntry(testGame, ActionType.UPDATE, purchaserUser, "Update 1");
        entityManager.flush();
        
        List<GameHistory> purchaserActivity = historyRepository.findTop5ByChangedByIdOrderByChangedAtDesc(
                purchaserUser.getId());
        assertThat(purchaserActivity).hasSize(2); // Only 2 activities
        
        // Test with user having no activities
        List<GameHistory> adminActivity = historyRepository.findTop5ByChangedByIdOrderByChangedAtDesc(
                adminUser.getId());
        assertThat(adminActivity).isEmpty();
    }

    @Test
    @DisplayName("Should find first and last activity timestamps")
    void shouldFindFirstAndLastActivityTimestamps() {
        // Given - Create activities with delays
        GameHistory firstActivity = createHistoryEntry(
                testGame, ActionType.CREATE, developerUser, "First");
        entityManager.flush();
        LocalDateTime firstTime = firstActivity.getChangedAt();

        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            // Ignore
        }

        createHistoryEntry(testGame, ActionType.UPDATE, developerUser, "Middle");
        entityManager.flush();

        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            // Ignore
        }

        GameHistory lastActivity = createHistoryEntry(
                secondGame, ActionType.UPDATE, developerUser, "Last");
        entityManager.flush();
        LocalDateTime lastTime = lastActivity.getChangedAt();

        // When
        LocalDateTime foundFirst = historyRepository.findFirstActivityByUser(developerUser.getId());
        LocalDateTime foundLast = historyRepository.findLastActivityByUser(developerUser.getId());

        // Then
        assertThat(foundFirst).isNotNull();
        assertThat(foundLast).isNotNull();
        assertThat(foundFirst).isCloseTo(firstTime, within(1, ChronoUnit.MILLIS));
        assertThat(foundLast).isCloseTo(lastTime, within(1, ChronoUnit.MILLIS));


        // Test for user with no activity
        LocalDateTime noFirst = historyRepository.findFirstActivityByUser(999L);
        LocalDateTime noLast = historyRepository.findLastActivityByUser(999L);
        assertThat(noFirst).isNull();
        assertThat(noLast).isNull();
    }

    @Test
    @DisplayName("Should handle pagination correctly")
    void shouldHandlePaginationCorrectly() {
        // Given - Create 25 history records
        for (int i = 1; i <= 25; i++) {
            createHistoryEntry(
                    testGame,
                    i % 4 == 0 ? ActionType.PURCHASE : ActionType.UPDATE,
                    i % 3 == 0 ? purchaserUser : developerUser,
                    "Activity " + i
            );
        }
        entityManager.flush();

        // When & Then - Test first page with fetch join
        Pageable firstPage = PageRequest.of(0, 10);
        Page<GameHistory> page1 = historyRepository.findByGameIdWithRelations(testGame.getId(), firstPage);

        assertThat(page1.getContent()).hasSize(10);
        assertThat(page1.getTotalElements()).isEqualTo(25);
        assertThat(page1.getTotalPages()).isEqualTo(3);
        assertThat(page1.getNumber()).isEqualTo(0);
        assertThat(page1.hasNext()).isTrue();
        assertThat(page1.hasPrevious()).isFalse();
        assertThat(page1.isFirst()).isTrue();
        assertThat(page1.isLast()).isFalse();

        // Test second page
        Pageable secondPage = PageRequest.of(1, 10);
        Page<GameHistory> page2 = historyRepository.findByGameIdWithRelations(testGame.getId(), secondPage);

        assertThat(page2.getContent()).hasSize(10);
        assertThat(page2.getNumber()).isEqualTo(1);
        assertThat(page2.hasNext()).isTrue();
        assertThat(page2.hasPrevious()).isTrue();
        assertThat(page2.isFirst()).isFalse();
        assertThat(page2.isLast()).isFalse();

        // Test last page
        Pageable lastPage = PageRequest.of(2, 10);
        Page<GameHistory> page3 = historyRepository.findByGameIdWithRelations(testGame.getId(), lastPage);

        assertThat(page3.getContent()).hasSize(5);
        assertThat(page3.hasNext()).isFalse();
        assertThat(page3.hasPrevious()).isTrue();
        assertThat(page3.isFirst()).isFalse();
        assertThat(page3.isLast()).isTrue();
    }

    @Test
    @DisplayName("Should handle complex field changes")
    void shouldHandleComplexFieldChanges() {
        // Given - History with detailed field changes
        GameHistory priceChange = GameHistory.builder()
                .game(testGame)
                .actionType(ActionType.UPDATE)
                .fieldChanged("price")
                .oldValue("29.99")
                .newValue("24.99")
                .changedBy(developerUser)
                .description("Black Friday sale")
                .build();
        historyRepository.save(priceChange);

        GameHistory titleChange = GameHistory.builder()
                .game(testGame)
                .actionType(ActionType.UPDATE)
                .fieldChanged("title")
                .oldValue("Epic Adventure")
                .newValue("Epic Adventure: Extended Edition")
                .changedBy(developerUser)
                .description("Title updated for extended edition")
                .build();
        historyRepository.save(titleChange);

        GameHistory categoryChange = GameHistory.builder()
                .game(testGame)
                .actionType(ActionType.UPDATE)
                .fieldChanged("category")
                .oldValue(actionCategory.getName())
                .newValue(rpgCategory.getName())
                .changedBy(adminUser)
                .description("Category reclassification")
                .build();
        historyRepository.save(categoryChange);

        entityManager.flush();

        // When - Find by game with relations
        Pageable pageable = PageRequest.of(0, 10);
        Page<GameHistory> updates = historyRepository.findByGameIdWithRelations(
                testGame.getId(), pageable);

        // Then
        assertThat(updates.getContent()).hasSize(3);

        // Verify price change
        GameHistory foundPriceChange = updates.getContent().stream()
                .filter(h -> "price".equals(h.getFieldChanged()))
                .findFirst()
                .orElse(null);
        assertThat(foundPriceChange).isNotNull();
        assertThat(foundPriceChange.getOldValue()).isEqualTo("29.99");
        assertThat(foundPriceChange.getNewValue()).isEqualTo("24.99");
        assertThat(foundPriceChange.getDescription()).contains("Black Friday");

        // Verify title change
        GameHistory foundTitleChange = updates.getContent().stream()
                .filter(h -> "title".equals(h.getFieldChanged()))
                .findFirst()
                .orElse(null);
        assertThat(foundTitleChange).isNotNull();
        assertThat(foundTitleChange.getNewValue()).contains("Extended Edition");

        // Verify category change
        GameHistory foundCategoryChange = updates.getContent().stream()
                .filter(h -> "category".equals(h.getFieldChanged()))
                .findFirst()
                .orElse(null);
        assertThat(foundCategoryChange).isNotNull();
        assertThat(foundCategoryChange.getChangedBy()).isEqualTo(adminUser);
    }

    @Test
    @DisplayName("Should handle edge cases and null values")
    void shouldHandleEdgeCasesAndNullValues() {
        // Test empty results for non-existent entities
        Pageable pageable = PageRequest.of(0, 10);

        Page<GameHistory> noGameHistory = historyRepository.findByGameIdWithRelations(999L, pageable);
        assertThat(noGameHistory.getContent()).isEmpty();
        assertThat(noGameHistory.getTotalElements()).isEqualTo(0);

        Page<GameHistory> noUserHistory = historyRepository.findByChangedByIdWithRelations(999L, pageable);
        assertThat(noUserHistory.getContent()).isEmpty();

        // Test counts for non-existent entities
        long noCount = historyRepository.countByUserId(999L);
        assertThat(noCount).isEqualTo(0);

        long noActionCount = historyRepository.countByUserAndActionType(999L, ActionType.CREATE);
        assertThat(noActionCount).isEqualTo(0);

        // Test with null field values
        GameHistory historyWithNulls = GameHistory.builder()
                .game(testGame)
                .actionType(ActionType.CREATE)
                .fieldChanged(null)  // null field
                .oldValue(null)      // null old value
                .newValue(null)      // null new value
                .changedBy(developerUser)
                .description("Created with null fields")
                .build();
        GameHistory saved = historyRepository.save(historyWithNulls);
        entityManager.flush();

        // Verify it can be retrieved
        Optional<GameHistory> found = historyRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getFieldChanged()).isNull();
        assertThat(found.get().getOldValue()).isNull();
        assertThat(found.get().getNewValue()).isNull();
    }

    @Test
    @DisplayName("Should verify JPA auditing for changed_at timestamp")
    void shouldVerifyJpaAuditingForChangedAtTimestamp() {
        // Given
        LocalDateTime beforeSave = LocalDateTime.now();

        GameHistory history = GameHistory.builder()
                .game(testGame)
                .actionType(ActionType.CREATE)
                .changedBy(developerUser)
                .description("Testing auditing")
                .build();

        // When
        GameHistory saved = historyRepository.save(history);
        entityManager.flush();
        LocalDateTime afterSave = LocalDateTime.now();

        // Then
        assertThat(saved.getChangedAt()).isNotNull();
        assertThat(saved.getChangedAt()).isAfterOrEqualTo(beforeSave);
        assertThat(saved.getChangedAt()).isBeforeOrEqualTo(afterSave);

        // Verify it's set automatically (not manually)
        GameHistory manual = GameHistory.builder()
                .game(testGame)
                .actionType(ActionType.UPDATE)
                .changedBy(developerUser)
                .description("Another test")
                // Not setting changedAt manually
                .build();
        GameHistory savedManual = historyRepository.save(manual);
        entityManager.flush();

        assertThat(savedManual.getChangedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should handle large datasets efficiently")
    void shouldHandleLargeDatasetsEfficiently() {
        // Given - Create 100 history records
        List<GameHistory> largeDataset = new ArrayList<>();
        for (int i = 1; i <= 100; i++) {
            GameHistory history = GameHistory.builder()
                    .game(i % 2 == 0 ? testGame : secondGame)
                    .actionType(ActionType.values()[i % 4])
                    .changedBy(i % 3 == 0 ? purchaserUser : developerUser)
                    .description("Large dataset entry " + i)
                    .build();
            largeDataset.add(history);
        }
        historyRepository.saveAll(largeDataset);
        entityManager.flush();

        // When & Then - Test pagination on large dataset
        Pageable pageable = PageRequest.of(0, 20);
        Page<GameHistory> firstPage = historyRepository.findByChangedByIdWithRelations(
                developerUser.getId(), pageable);

        assertThat(firstPage.getTotalElements()).isGreaterThan(50);
        assertThat(firstPage.getContent()).hasSize(20);
        assertThat(firstPage.getTotalPages()).isGreaterThan(2);

        // Test counting on large dataset
        long devCount = historyRepository.countByUserId(developerUser.getId());
        assertThat(devCount).isGreaterThan(50);

        // Test action type counting on large dataset
        long updateCount = historyRepository.countByUserAndActionType(
                developerUser.getId(), ActionType.UPDATE);
        assertThat(updateCount).isGreaterThan(10);
        
        // Test findTop5 returns only 5 even with 60+ records
        List<GameHistory> top5Dev = historyRepository.findTop5ByChangedByIdOrderByChangedAtDesc(
                developerUser.getId());
        assertThat(top5Dev).hasSize(5);
        
        // Verify they are the most recent ones
        for (int i = 0; i < top5Dev.size() - 1; i++) {
            assertThat(top5Dev.get(i).getChangedAt())
                    .isAfterOrEqualTo(top5Dev.get(i + 1).getChangedAt());
        }
    }

    // Helper methods
    private GameHistory createHistoryEntry(Game game, ActionType actionType,
                                           User changedBy, String description) {
        GameHistory history = GameHistory.builder()
                .game(game)
                .actionType(actionType)
                .changedBy(changedBy)
                .description(description)
                .build();
        return historyRepository.save(history);
    }
}
