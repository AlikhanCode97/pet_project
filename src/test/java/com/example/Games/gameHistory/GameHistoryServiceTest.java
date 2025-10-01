package com.example.Games.gameHistory;

import com.example.Games.config.common.service.UserContextService;
import com.example.Games.config.exception.auth.UserNotFoundException;
import com.example.Games.config.exception.game.GameNotFoundException;
import com.example.Games.config.exception.gameHistory.GameHistoryException;
import com.example.Games.game.Game;
import com.example.Games.game.GameRepository;
import com.example.Games.category.Category;
import com.example.Games.gameHistory.dto.DeveloperActivityResponse;
import com.example.Games.gameHistory.dto.FieldChange;
import com.example.Games.gameHistory.dto.GameHistoryResponse;
import com.example.Games.user.auth.User;
import com.example.Games.user.auth.UserRepository;
import com.example.Games.user.role.Role;
import com.example.Games.user.role.RoleType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GameHistoryService Tests")
class GameHistoryServiceTest {

    @Mock
    private GameHistoryRepository historyRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private GameRepository gameRepository;

    @Mock
    private GameHistoryMapStruct gameHistoryMapper;

    @Mock
    private UserContextService userContextService;

    @InjectMocks
    private GameHistoryService historyService;

    @Captor
    private ArgumentCaptor<GameHistory> historyCaptor;

    @Captor
    private ArgumentCaptor<List<GameHistory>> historyListCaptor;

    private User testUser;
    private User otherUser;
    private User developer;
    private Game testGame;
    private Game testGame2;
    private Category testCategory;
    private GameHistory sampleHistory;
    private GameHistoryResponse sampleHistoryResponse;

    @BeforeEach
    void setUp() {
        // Setup roles
        Role developerRole = Role.builder()
                .id(1L)
                .name(RoleType.DEVELOPER)
                .build();

        Role userRole = Role.builder()
                .id(2L)
                .name(RoleType.USER)
                .build();

        // Setup users
        developer = User.builder()
                .id(1L)
                .username("gamedev")
                .email("dev@example.com")
                .password("password123")
                .role(developerRole)
                .build();

        testUser = User.builder()
                .id(2L)
                .username("purchaser")
                .email("purchaser@example.com")
                .password("password123")
                .role(userRole)
                .build();

        otherUser = User.builder()
                .id(3L)
                .username("otheruser")
                .email("other@example.com")
                .password("password123")
                .role(userRole)
                .build();

        // Setup category
        testCategory = Category.builder()
                .id(1L)
                .name("Action")
                .build();

        // Setup games
        testGame = Game.builder()
                .id(1L)
                .title("Test Game")
                .author(developer)
                .price(new BigDecimal("29.99"))
                .category(testCategory)
                .build();

        testGame2 = Game.builder()
                .id(2L)
                .title("Test Game 2")
                .author(developer)
                .price(new BigDecimal("39.99"))
                .category(testCategory)
                .build();

        // Setup sample history
        sampleHistory = GameHistory.builder()
                .id(1L)
                .game(testGame)
                .actionType(ActionType.CREATE)
                .changedBy(developer)
                .changedAt(LocalDateTime.now())
                .description("Game created")
                .build();

        // Setup sample response
        sampleHistoryResponse = new GameHistoryResponse(
                1L,
                1L,
                "Test Game",
                "CREATE",
                "Game Created",
                null,
                null,
                null,
                "gamedev",
                LocalDateTime.now(),
                "Game created"
        );
    }

    @Test
    @DisplayName("Should record game creation successfully")
    void shouldRecordGameCreationSuccessfully() {
        // Given
        when(gameHistoryMapper.createAction(testGame, developer)).thenReturn(sampleHistory);
        when(historyRepository.save(any(GameHistory.class))).thenReturn(sampleHistory);

        // When
        historyService.recordGameCreation(testGame, developer);

        // Then
        verify(gameHistoryMapper).createAction(testGame, developer);
        verify(historyRepository).save(historyCaptor.capture());
        assertThat(historyCaptor.getValue()).isEqualTo(sampleHistory);
    }

    @Test
    @DisplayName("Should record game updates successfully")
    void shouldRecordGameUpdatesSuccessfully() {
        // Given
        List<FieldChange> changes = Arrays.asList(
                FieldChange.of("title", "Old Title", "New Title"),
                FieldChange.of("price", "29.99", "39.99")
        );

        GameHistory titleHistory = GameHistory.builder()
                .game(testGame)
                .actionType(ActionType.UPDATE)
                .fieldChanged("title")
                .oldValue("Old Title")
                .newValue("New Title")
                .changedBy(developer)
                .build();

        GameHistory priceHistory = GameHistory.builder()
                .game(testGame)
                .actionType(ActionType.UPDATE)
                .fieldChanged("price")
                .oldValue("29.99")
                .newValue("39.99")
                .changedBy(developer)
                .build();

        when(gameHistoryMapper.updateAction(testGame, developer, changes.get(0))).thenReturn(titleHistory);
        when(gameHistoryMapper.updateAction(testGame, developer, changes.get(1))).thenReturn(priceHistory);
        when(historyRepository.saveAll(anyList())).thenReturn(Arrays.asList(titleHistory, priceHistory));

        // When
        historyService.recordGameUpdates(testGame, changes, developer);

        // Then
        verify(gameHistoryMapper).updateAction(testGame, developer, changes.get(0));
        verify(gameHistoryMapper).updateAction(testGame, developer, changes.get(1));
        verify(historyRepository).saveAll(historyListCaptor.capture());

        List<GameHistory> savedHistories = historyListCaptor.getValue();
        assertThat(savedHistories).hasSize(2);
        assertThat(savedHistories).containsExactly(titleHistory, priceHistory);
    }

    @Test
    @DisplayName("Should filter out non-actual changes during update")
    void shouldFilterOutNonActualChangesDuringUpdate() {
        // Given
        List<FieldChange> changes = Arrays.asList(
                FieldChange.of("title", "Same Title", "Same Title"), // No actual change
                FieldChange.of("price", "29.99", "39.99") // Actual change
        );

        GameHistory priceHistory = GameHistory.builder()
                .game(testGame)
                .actionType(ActionType.UPDATE)
                .fieldChanged("price")
                .oldValue("29.99")
                .newValue("39.99")
                .changedBy(developer)
                .build();

        when(gameHistoryMapper.updateAction(testGame, developer, changes.get(1))).thenReturn(priceHistory);
        when(historyRepository.saveAll(anyList())).thenReturn(Collections.singletonList(priceHistory));

        // When
        historyService.recordGameUpdates(testGame, changes, developer);

        // Then
        verify(gameHistoryMapper, times(1)).updateAction(any(), any(), any());
        verify(gameHistoryMapper).updateAction(testGame, developer, changes.get(1));
        verify(historyRepository).saveAll(historyListCaptor.capture());

        List<GameHistory> savedHistories = historyListCaptor.getValue();
        assertThat(savedHistories).hasSize(1);
        assertThat(savedHistories.get(0)).isEqualTo(priceHistory);
    }

    @Test
    @DisplayName("Should record game deletion successfully")
    void shouldRecordGameDeletionSuccessfully() {
        // Given
        GameHistory deleteHistory = GameHistory.builder()
                .game(testGame)
                .actionType(ActionType.DELETE)
                .changedBy(developer)
                .description("Game deleted")
                .build();

        when(gameHistoryMapper.deleteAction(testGame, developer)).thenReturn(deleteHistory);
        when(historyRepository.save(any(GameHistory.class))).thenReturn(deleteHistory);

        // When
        historyService.recordGameDeletion(testGame, developer);

        // Then
        verify(gameHistoryMapper).deleteAction(testGame, developer);
        verify(historyRepository).save(historyCaptor.capture());
        assertThat(historyCaptor.getValue()).isEqualTo(deleteHistory);
    }

    @Test
    @DisplayName("Should record single game purchase successfully")
    void shouldRecordSingleGamePurchaseSuccessfully() {
        // Given
        BigDecimal purchasePrice = new BigDecimal("29.99");
        GameHistory purchaseHistory = GameHistory.builder()
                .game(testGame)
                .actionType(ActionType.PURCHASE)
                .changedBy(testUser)
                .newValue("29.99")
                .description("Game purchased")
                .build();

        when(gameHistoryMapper.purchaseAction(testGame, testUser, purchasePrice)).thenReturn(purchaseHistory);
        when(historyRepository.save(any(GameHistory.class))).thenReturn(purchaseHistory);

        // When
        historyService.recordGamePurchase(testGame, testUser, purchasePrice);

        // Then
        verify(gameHistoryMapper).purchaseAction(testGame, testUser, purchasePrice);
        verify(historyRepository).save(purchaseHistory);
    }

    @Test
    @DisplayName("Should record multiple game purchases with explicit prices")
    void shouldRecordMultipleGamePurchasesWithExplicitPrices() {
        // Given
        List<Game> games = Arrays.asList(testGame, testGame2);
        List<BigDecimal> prices = Arrays.asList(
                new BigDecimal("29.99"),
                new BigDecimal("39.99")
        );

        GameHistory purchase1 = GameHistory.builder()
                .game(testGame)
                .actionType(ActionType.PURCHASE)
                .changedBy(testUser)
                .newValue("29.99")
                .build();

        GameHistory purchase2 = GameHistory.builder()
                .game(testGame2)
                .actionType(ActionType.PURCHASE)
                .changedBy(testUser)
                .newValue("39.99")
                .build();

        when(gameHistoryMapper.purchaseAction(testGame, testUser, prices.get(0))).thenReturn(purchase1);
        when(gameHistoryMapper.purchaseAction(testGame2, testUser, prices.get(1))).thenReturn(purchase2);
        when(historyRepository.saveAll(anyList())).thenReturn(Arrays.asList(purchase1, purchase2));

        // When
        historyService.recordGamePurchases(games, testUser, prices);

        // Then
        verify(gameHistoryMapper).purchaseAction(testGame, testUser, prices.get(0));
        verify(gameHistoryMapper).purchaseAction(testGame2, testUser, prices.get(1));
        verify(historyRepository).saveAll(historyListCaptor.capture());

        List<GameHistory> savedPurchases = historyListCaptor.getValue();
        assertThat(savedPurchases).hasSize(2);
    }

    @Test
    @DisplayName("Should throw exception when purchase price list size mismatch")
    void shouldThrowExceptionWhenPurchasePriceListSizeMismatch() {
        // Given
        List<Game> games = Arrays.asList(testGame, testGame2);
        List<BigDecimal> prices = Collections.singletonList(new BigDecimal("29.99")); // Only one price for two games

        // When & Then
        assertThatThrownBy(() -> historyService.recordGamePurchases(games, testUser, prices))
                .isInstanceOf(GameHistoryException.class)
                .hasMessageContaining("2")
                .hasMessageContaining("1");

        verify(historyRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("Should get current developer activity successfully")
    void shouldGetCurrentDeveloperActivitySuccessfully() {
        // Given
        when(userContextService.getAuthorizedUser()).thenReturn(developer);

        long totalGamesCreated = 5L;
        long totalChanges = 15L;
        LocalDateTime firstActivity = LocalDateTime.now().minusDays(30);
        LocalDateTime lastActivity = LocalDateTime.now();
        List<GameHistory> recentHistory = Collections.singletonList(sampleHistory);
        List<GameHistoryResponse> recentActivity = Collections.singletonList(sampleHistoryResponse);

        DeveloperActivityResponse expectedResponse = new DeveloperActivityResponse(
                1L,
                "gamedev",
                "dev@example.com",
                totalGamesCreated,
                totalChanges,
                recentActivity,
                firstActivity,
                lastActivity
        );

        when(historyRepository.countByUserAndActionType(1L, ActionType.CREATE)).thenReturn(totalGamesCreated);
        when(historyRepository.countByUserId(1L)).thenReturn(totalChanges);
        when(historyRepository.findFirstActivityByUser(1L)).thenReturn(firstActivity);
        when(historyRepository.findLastActivityByUser(1L)).thenReturn(lastActivity);
        when(historyRepository.findTop5ByChangedByIdOrderByChangedAtDesc(1L)).thenReturn(recentHistory);
        when(gameHistoryMapper.toDtoList(recentHistory)).thenReturn(recentActivity);
        when(gameHistoryMapper.createDeveloperActivityResponse(
                developer, totalGamesCreated, totalChanges, recentActivity, firstActivity, lastActivity))
                .thenReturn(expectedResponse);

        // When
        DeveloperActivityResponse result = historyService.getMyDeveloperActivity();

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(expectedResponse);
        assertThat(result.developerId()).isEqualTo(1L);
        assertThat(result.totalGamesCreated()).isEqualTo(totalGamesCreated);

        verify(userContextService).getAuthorizedUser();
        verify(historyRepository).findTop5ByChangedByIdOrderByChangedAtDesc(1L);
    }

    @Test
    @DisplayName("Should get developer activity by ID successfully")
    void shouldGetDeveloperActivityByIdSuccessfully() {
        // Given
        Long developerId = 1L;
        when(userContextService.getUserById(developerId)).thenReturn(developer);

        long totalGamesCreated = 10L;
        long totalChanges = 25L;
        LocalDateTime firstActivity = LocalDateTime.now().minusDays(60);
        LocalDateTime lastActivity = LocalDateTime.now();
        List<GameHistory> recentHistory = Collections.singletonList(sampleHistory);
        List<GameHistoryResponse> recentActivity = Collections.singletonList(sampleHistoryResponse);

        DeveloperActivityResponse expectedResponse = new DeveloperActivityResponse(
                1L,
                "gamedev",
                "dev@example.com",
                totalGamesCreated,
                totalChanges,
                recentActivity,
                firstActivity,
                lastActivity
        );

        when(historyRepository.countByUserAndActionType(developerId, ActionType.CREATE)).thenReturn(totalGamesCreated);
        when(historyRepository.countByUserId(developerId)).thenReturn(totalChanges);
        when(historyRepository.findFirstActivityByUser(developerId)).thenReturn(firstActivity);
        when(historyRepository.findLastActivityByUser(developerId)).thenReturn(lastActivity);
        when(historyRepository.findTop5ByChangedByIdOrderByChangedAtDesc(developerId)).thenReturn(recentHistory);
        when(gameHistoryMapper.toDtoList(recentHistory)).thenReturn(recentActivity);
        when(gameHistoryMapper.createDeveloperActivityResponse(
                developer, totalGamesCreated, totalChanges, recentActivity, firstActivity, lastActivity))
                .thenReturn(expectedResponse);

        // When
        DeveloperActivityResponse result = historyService.getDeveloperActivity(developerId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.developerId()).isEqualTo(developerId);

        verify(userContextService).getUserById(developerId);
    }

    @Test
    @DisplayName("Should get my game history successfully")
    void shouldGetMyGameHistorySuccessfully() {
        // Given
        Long gameId = 1L;
        int page = 0;
        int size = 10;

        when(userContextService.getAuthorizedUser()).thenReturn(developer);
        when(gameRepository.findByIdWithAuthor(gameId)).thenReturn(Optional.of(testGame));

        List<GameHistory> historyList = Collections.singletonList(sampleHistory);
        Page<GameHistory> historyPage = new PageImpl<>(historyList, PageRequest.of(page, size), 1);

        when(historyRepository.findByGameIdWithRelations(eq(gameId), any(Pageable.class))).thenReturn(historyPage);
        when(gameHistoryMapper.toDto(sampleHistory)).thenReturn(sampleHistoryResponse);

        // When
        Page<GameHistoryResponse> result = historyService.getMyGameHistory(gameId, page, size);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0)).isEqualTo(sampleHistoryResponse);

        verify(gameRepository).findByIdWithAuthor(gameId);
    }

    @Test
    @DisplayName("Should throw exception when accessing another developer's game history")
    void shouldThrowExceptionWhenAccessingAnotherDevelopersGameHistory() {
        // Given
        Long gameId = 1L;
        when(userContextService.getAuthorizedUser()).thenReturn(otherUser); // Different user
        when(gameRepository.findByIdWithAuthor(gameId)).thenReturn(Optional.of(testGame));

        // When & Then
        assertThatThrownBy(() -> historyService.getMyGameHistory(gameId, 0, 10))
                .isInstanceOf(GameHistoryException.class)
                .hasMessageContaining(otherUser.getUsername());

        verify(historyRepository, never()).findByGameIdWithRelations(any(), any());
    }

    @Test
    @DisplayName("Should throw exception when accessing another developer's game history")
    void shouldThrowExceptionWhenGameNotFoundMyGameHistory() {
        // Given
        Long gameId = 1L;
        when(userContextService.getAuthorizedUser()).thenReturn(otherUser); // Different user
        when(gameRepository.findByIdWithAuthor(anyLong())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> historyService.getMyGameHistory(99L, 0, 10))
                .isInstanceOf(GameNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("Should get game history with pagination")
    void shouldGetGameHistoryWithPagination() {
        // Given
        Long gameId = 1L;
        int page = 0;
        int size = 5;

        List<GameHistory> historyList = Collections.singletonList(sampleHistory);
        Page<GameHistory> historyPage = new PageImpl<>(historyList, PageRequest.of(page, size), 1);

        when(historyRepository.findByGameIdWithRelations(eq(gameId), any(Pageable.class))).thenReturn(historyPage);
        when(gameHistoryMapper.toDto(sampleHistory)).thenReturn(sampleHistoryResponse);

        // When
        Page<GameHistoryResponse> result = historyService.getGameHistory(gameId, page, size);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getPageable().getPageNumber()).isEqualTo(page);
        assertThat(result.getPageable().getPageSize()).isEqualTo(size);

        verify(historyRepository).findByGameIdWithRelations(eq(gameId), any(Pageable.class));
    }

    @Test
    @DisplayName("Should get developer history with pagination")
    void shouldGetDeveloperHistoryWithPagination() {
        // Given
        Long developerId = 1L;
        int page = 1;
        int size = 20;

        List<GameHistory> historyList = Arrays.asList(sampleHistory);
        Page<GameHistory> historyPage = new PageImpl<>(historyList, PageRequest.of(page, size), 21);

        when(historyRepository.findByChangedByIdWithRelations(eq(developerId), any(Pageable.class)))
                .thenReturn(historyPage);
        when(gameHistoryMapper.toDto(sampleHistory)).thenReturn(sampleHistoryResponse);

        // When
        Page<GameHistoryResponse> result = historyService.getDeveloperHistory(developerId, page, size);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(21);
        assertThat(result.getTotalPages()).isEqualTo(2);

        verify(historyRepository).findByChangedByIdWithRelations(eq(developerId), any(Pageable.class));
    }

    @Test
    @DisplayName("Should get current developer's history")
    void shouldGetCurrentDevelopersHistory() {
        // Given
        int page = 0;
        int size = 15;

        when(userContextService.getAuthorizedUser()).thenReturn(developer);

        List<GameHistory> historyList = Arrays.asList(sampleHistory);
        Page<GameHistory> historyPage = new PageImpl<>(historyList, PageRequest.of(page, size), 1);

        when(historyRepository.findByChangedByIdWithRelations(eq(1L), any(Pageable.class)))
                .thenReturn(historyPage);
        when(gameHistoryMapper.toDto(sampleHistory)).thenReturn(sampleHistoryResponse);

        // When
        Page<GameHistoryResponse> result = historyService.getMyDeveloperHistory(page, size);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);

        verify(userContextService).getAuthorizedUser();
        verify(historyRepository).findByChangedByIdWithRelations(eq(1L), any(Pageable.class));
    }

    @Test
    @DisplayName("Should handle empty history results")
    void shouldHandleEmptyHistoryResults() {
        // Given
        Long gameId = 999L;
        Page<GameHistory> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 10), 0);

        when(historyRepository.findByGameIdWithRelations(eq(gameId), any(Pageable.class)))
                .thenReturn(emptyPage);

        // When
        Page<GameHistoryResponse> result = historyService.getGameHistory(gameId, 0, 10);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should handle null activity timestamps")
    void shouldHandleNullActivityTimestamps() {
        // Given
        Long developerId = 1L;
        when(userContextService.getUserById(developerId)).thenReturn(developer);

        when(historyRepository.countByUserAndActionType(developerId, ActionType.CREATE)).thenReturn(0L);
        when(historyRepository.countByUserId(developerId)).thenReturn(0L);
        when(historyRepository.findFirstActivityByUser(developerId)).thenReturn(null);
        when(historyRepository.findLastActivityByUser(developerId)).thenReturn(null);
        when(historyRepository.findTop5ByChangedByIdOrderByChangedAtDesc(developerId))
                .thenReturn(Collections.emptyList());
        when(gameHistoryMapper.toDtoList(Collections.emptyList()))
                .thenReturn(Collections.emptyList());
        when(gameHistoryMapper.createDeveloperActivityResponse(
                any(), anyLong(), anyLong(), any(), any(), any()))
                .thenReturn(new DeveloperActivityResponse(
                        developerId, "gamedev", "dev@example.com",
                        0L, 0L, Collections.emptyList(), null, null
                ));

        // When
        DeveloperActivityResponse result = historyService.getDeveloperActivity(developerId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.totalGamesCreated()).isEqualTo(0L);
        assertThat(result.totalChanges()).isEqualTo(0L);
        assertThat(result.recentActivity()).isEmpty();
        assertThat(result.firstActivity()).isNull();
        assertThat(result.lastActivity()).isNull();
    }
}