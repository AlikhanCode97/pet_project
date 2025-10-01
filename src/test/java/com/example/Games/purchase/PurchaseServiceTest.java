package com.example.Games.purchase;

import com.example.Games.config.common.service.UserContextService;
import com.example.Games.config.exception.game.GameNotFoundException;
import com.example.Games.config.exception.purchase.GameAlreadyOwnedException;
import com.example.Games.config.exception.purchase.PurchaseException;
import com.example.Games.game.Game;
import com.example.Games.game.GameRepository;
import com.example.Games.gameHistory.GameHistoryService;
import com.example.Games.purchase.dto.PurchaseGamesRequest;
import com.example.Games.purchase.dto.PurchaseResponse;
import com.example.Games.user.auth.User;
import com.example.Games.user.balance.BalanceService;
import com.example.Games.user.balance.transaction.BalanceTransaction;
import com.example.Games.user.role.Role;
import com.example.Games.user.role.RoleType;
import com.example.Games.category.Category;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PurchaseService Tests")
class PurchaseServiceTest {

    @Mock
    private PurchaseRepository purchaseRepository;

    @Mock
    private GameRepository gameRepository;

    @Mock
    private BalanceService balanceService;

    @Mock
    private GameHistoryService gameHistoryService;

    @Mock
    private PurchaseMapStruct purchaseMapper;

    @Mock
    private UserContextService userContextService;

    @InjectMocks
    private PurchaseService purchaseService;

    @Captor
    private ArgumentCaptor<PurchaseHistory> purchaseCaptor;

    @Captor
    private ArgumentCaptor<List<PurchaseHistory>> purchaseListCaptor;

    private User currentUser;
    private User developer;
    private Game testGame;
    private Game testGame2;
    private PurchaseHistory purchaseHistory;
    private PurchaseResponse purchaseResponse;
    private BalanceTransaction balanceTransaction;

    @BeforeEach
    void setUp() {
        // Setup roles
        Role userRole = Role.builder()
                .name(RoleType.USER)
                .build();

        Role developerRole = Role.builder()
                .name(RoleType.DEVELOPER)
                .build();

        // Setup users
        currentUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .role(userRole)
                .build();

        developer = User.builder()
                .id(2L)
                .username("developer")
                .email("dev@example.com")
                .role(developerRole)
                .build();

        // Setup category
        Category category = Category.builder()
                .id(1L)
                .name("Action")
                .build();

        // Setup games
        testGame = Game.builder()
                .id(1L)
                .title("Test Game")
                .author(developer)
                .price(new BigDecimal("29.99"))
                .category(category)
                .build();

        testGame2 = Game.builder()
                .id(2L)
                .title("Test Game 2")
                .author(developer)
                .price(new BigDecimal("39.99"))
                .category(category)
                .build();

        // Setup purchase history
        purchaseHistory = PurchaseHistory.builder()
                .id(1L)
                .user(currentUser)
                .game(testGame)
                .purchasePrice(new BigDecimal("29.99"))
                .purchasedAt(LocalDateTime.now())
                .build();

        // Setup purchase response
        purchaseResponse = PurchaseResponse.builder()
                .purchaseId(1L)
                .gameId(1L)
                .gameTitle("Test Game")
                .gameAuthor("developer")
                .purchasePrice(new BigDecimal("29.99"))
                .currentGamePrice(new BigDecimal("29.99"))
                .purchasedAt(LocalDateTime.now())
                .priceDifference(BigDecimal.ZERO)
                .build();

        // Setup balance transaction
        balanceTransaction = BalanceTransaction.builder()
                .id(1L)
                .amount(new BigDecimal("29.99"))
                .build();

        // Default mock behavior
        lenient().when(userContextService.getAuthorizedUser())
                .thenReturn(currentUser);

    }

    @Test
    @DisplayName("Should successfully purchase a single game")
    void shouldSuccessfullyPurchaseSingleGame() {
        // Given
        when(gameRepository.findByIdWithAuthor(1L)).thenReturn(Optional.of(testGame));
        when(purchaseRepository.existsByUserIdAndGameId(1L, 1L)).thenReturn(false);
        when(balanceService.createPurchaseTransaction(any(BigDecimal.class), any(User.class)))
                .thenReturn(balanceTransaction);
        when(purchaseMapper.createPurchase(currentUser, testGame)).thenReturn(purchaseHistory);
        when(purchaseRepository.save(any(PurchaseHistory.class))).thenReturn(purchaseHistory);
        when(purchaseMapper.toPurchaseResponse(purchaseHistory)).thenReturn(purchaseResponse);

        // When
        PurchaseResponse result = purchaseService.purchaseGame(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.purchaseId()).isEqualTo(1L);
        assertThat(result.gameTitle()).isEqualTo("Test Game");
        assertThat(result.purchasePrice()).isEqualByComparingTo(new BigDecimal("29.99"));

        verify(purchaseRepository).save(purchaseCaptor.capture());
        assertThat(purchaseCaptor.getValue()).isEqualTo(purchaseHistory);

        verify(balanceService).createPurchaseTransaction(
                eq(new BigDecimal("29.99")),
                eq(currentUser)
        );
        verify(gameHistoryService).recordGamePurchase(
                eq(testGame),
                eq(currentUser),
                eq(new BigDecimal("29.99"))
        );
    }

    @Test
    @DisplayName("Should throw exception when game not found")
    void shouldThrowExceptionWhenGameNotFound() {
        // Given
        when(gameRepository.findByIdWithAuthor(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> purchaseService.purchaseGame(999L))
                .isInstanceOf(GameNotFoundException.class)
                .hasMessage("Game not found");

        verify(purchaseRepository, never()).save(any());
        verify(balanceService, never()).createPurchaseTransaction(any(), any());
    }

    @Test
    @DisplayName("Should throw exception when user already owns the game")
    void shouldThrowExceptionWhenUserAlreadyOwnsGame() {
        // Given
        when(gameRepository.findByIdWithAuthor(1L)).thenReturn(Optional.of(testGame));
        when(purchaseRepository.existsByUserIdAndGameId(1L, 1L)).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> purchaseService.purchaseGame(1L))
                .isInstanceOf(GameAlreadyOwnedException.class)
                .hasMessage("You already own this game: Test Game");

        verify(purchaseRepository, never()).save(any());
        verify(balanceService, never()).createPurchaseTransaction(any(), any());
    }

    @Test
    @DisplayName("Should throw exception when user tries to purchase their own game")
    void shouldThrowExceptionWhenUserTriesToPurchaseOwnGame() {
        // Given
        Game ownGame = Game.builder()
                .id(3L)
                .title("My Game")
                .author(currentUser) // Current user is the author
                .price(new BigDecimal("19.99"))
                .build();

        when(gameRepository.findByIdWithAuthor(3L)).thenReturn(Optional.of(ownGame));
        when(purchaseRepository.existsByUserIdAndGameId(1L, 3L)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> purchaseService.purchaseGame(3L))
                .isInstanceOf(PurchaseException.class)
                .hasMessageContaining("My Game");

        verify(purchaseRepository, never()).save(any());
        verify(balanceService, never()).createPurchaseTransaction(any(), any());
    }

    @Test
    @DisplayName("Should successfully purchase multiple games")
    void shouldSuccessfullyPurchaseMultipleGames() {
        // Given
        List<Long> gameIds = Arrays.asList(1L, 2L);
        List<Game> games = Arrays.asList(testGame, testGame2);
        PurchaseGamesRequest request = new PurchaseGamesRequest(gameIds);

        PurchaseHistory purchase2 = PurchaseHistory.builder()
                .id(2L)
                .user(currentUser)
                .game(testGame2)
                .purchasePrice(new BigDecimal("39.99"))
                .purchasedAt(LocalDateTime.now())
                .build();

        PurchaseResponse response2 = PurchaseResponse.builder()
                .purchaseId(2L)
                .gameId(2L)
                .gameTitle("Test Game 2")
                .gameAuthor("developer")
                .purchasePrice(new BigDecimal("39.99"))
                .currentGamePrice(new BigDecimal("39.99"))
                .purchasedAt(LocalDateTime.now())
                .priceDifference(BigDecimal.ZERO)
                .build();

        when(gameRepository.findAllByIdWithAuthor(gameIds)).thenReturn(games);
        when(purchaseRepository.findOwnedGameIds(1L, gameIds)).thenReturn(Collections.emptyList());
        when(balanceService.createPurchaseTransaction(any(BigDecimal.class), any(User.class)))
                .thenReturn(balanceTransaction);
        when(purchaseMapper.createPurchase(currentUser, testGame)).thenReturn(purchaseHistory);
        when(purchaseMapper.createPurchase(currentUser, testGame2)).thenReturn(purchase2);
        when(purchaseRepository.saveAll(anyList()))
                .thenReturn(Arrays.asList(purchaseHistory, purchase2));
        when(purchaseMapper.toPurchaseResponse(purchaseHistory)).thenReturn(purchaseResponse);
        when(purchaseMapper.toPurchaseResponse(purchase2)).thenReturn(response2);

        // When
        List<PurchaseResponse> results = purchaseService.purchaseGamesByIds(request);

        // Then
        assertThat(results).hasSize(2);
        assertThat(results.get(0).gameTitle()).isEqualTo("Test Game");
        assertThat(results.get(1).gameTitle()).isEqualTo("Test Game 2");

        BigDecimal expectedTotal = new BigDecimal("69.98");
        verify(balanceService).createPurchaseTransaction(
                argThat(amount -> amount.compareTo(expectedTotal) == 0),
                eq(currentUser)
        );
        verify(gameHistoryService).recordGamePurchases(eq(games), eq(currentUser));
    }

    @Test
    @DisplayName("Should throw exception when some games not found in bulk purchase")
    void shouldThrowExceptionWhenSomeGamesNotFoundInBulkPurchase() {
        // Given
        List<Long> gameIds = Arrays.asList(1L, 999L);
        List<Game> games = Arrays.asList(testGame); // Only one game found
        PurchaseGamesRequest request = new PurchaseGamesRequest(gameIds);

        when(gameRepository.findAllByIdWithAuthor(gameIds)).thenReturn(games);

        // When & Then
        assertThatThrownBy(() -> purchaseService.purchaseGamesByIds(request))
                .isInstanceOf(GameNotFoundException.class)
                .hasMessage("One or more games not found");

        verify(purchaseRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("Should throw exception when user already owns some games in bulk purchase")
    void shouldThrowExceptionWhenUserAlreadyOwnsSomeGamesInBulkPurchase() {
        // Given
        List<Long> gameIds = Arrays.asList(1L, 2L);
        List<Game> games = Arrays.asList(testGame, testGame2);
        PurchaseGamesRequest request = new PurchaseGamesRequest(gameIds);

        when(gameRepository.findAllByIdWithAuthor(gameIds)).thenReturn(games);
        when(purchaseRepository.findOwnedGameIds(1L, gameIds))
                .thenReturn(Arrays.asList(1L)); // User already owns game 1

        // When & Then
        assertThatThrownBy(() -> purchaseService.purchaseGamesByIds(request))
                .isInstanceOf(GameAlreadyOwnedException.class)
                .hasMessage("You already own games with IDs: [1]");

        verify(purchaseRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("Should check if user can afford games")
    void shouldCheckIfUserCanAffordGames() {
        // Given
        List<Long> gameIds = Arrays.asList(1L, 2L);
        List<Game> games = Arrays.asList(testGame, testGame2);

        when(gameRepository.findAllByIdWithAuthor(gameIds)).thenReturn(games);
        when(purchaseRepository.findOwnedGameIds(1L, gameIds)).thenReturn(Collections.emptyList());
        when(balanceService.canAfford(any(BigDecimal.class))).thenReturn(true);

        // When
        boolean result = purchaseService.canPurchaseGames(gameIds);

        // Then
        assertThat(result).isTrue();

        BigDecimal expectedTotal = new BigDecimal("69.98");
        verify(balanceService).canAfford(
                argThat(amount -> amount.compareTo(expectedTotal) == 0)
        );
    }

    @Test
    @DisplayName("Should get my purchase history")
    void shouldGetMyPurchaseHistory() {
        // Given
        List<PurchaseHistory> purchases = Arrays.asList(purchaseHistory);
        List<PurchaseResponse> responses = Arrays.asList(purchaseResponse);

        when(purchaseRepository.findByUserWithGameAndAuthor(currentUser))
                .thenReturn(purchases);
        when(purchaseMapper.toPurchaseResponseList(purchases))
                .thenReturn(responses);

        // When
        List<PurchaseResponse> result = purchaseService.getMyPurchaseHistory();

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).gameTitle()).isEqualTo("Test Game");
        verify(purchaseRepository).findByUserWithGameAndAuthor(currentUser);
    }

    @Test
    @DisplayName("Should get my purchase history paginated")
    void shouldGetMyPurchaseHistoryPaginated() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<PurchaseHistory> purchasePage = new PageImpl<>(
                Arrays.asList(purchaseHistory), pageable, 1
        );

        when(purchaseRepository.findByUserWithGameAndAuthor(currentUser, pageable))
                .thenReturn(purchasePage);
        when(purchaseMapper.toPurchaseResponse(purchaseHistory))
                .thenReturn(purchaseResponse);

        // When
        Page<PurchaseResponse> result = purchaseService.getMyPurchaseHistoryPaged(pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).gameTitle()).isEqualTo("Test Game");
    }

    @Test
    @DisplayName("Should get user purchase history by admin")
    void shouldGetUserPurchaseHistoryByAdmin() {
        // Given
        Long userId = 5L;
        List<PurchaseHistory> purchases = Arrays.asList(purchaseHistory);
        List<PurchaseResponse> responses = Arrays.asList(purchaseResponse);

        when(purchaseRepository.findByUserIdWithGameAndAuthor(userId))
                .thenReturn(purchases);
        when(purchaseMapper.toPurchaseResponseList(purchases))
                .thenReturn(responses);

        // When
        List<PurchaseResponse> result = purchaseService.getUserPurchaseHistory(userId);

        // Then
        assertThat(result).hasSize(1);
        verify(purchaseRepository).findByUserIdWithGameAndAuthor(userId);
    }

    @Test
    @DisplayName("Should get game purchases")
    void shouldGetGamePurchases() {
        // Given
        Long gameId = 1L;
        List<PurchaseHistory> purchases = Arrays.asList(purchaseHistory);
        List<PurchaseResponse> responses = Arrays.asList(purchaseResponse);

        when(purchaseRepository.findByGameIdWithGameAndAuthor(gameId))
                .thenReturn(purchases);
        when(purchaseMapper.toPurchaseResponseList(purchases))
                .thenReturn(responses);

        // When
        List<PurchaseResponse> result = purchaseService.getGamePurchases(gameId);

        // Then
        assertThat(result).hasSize(1);
        verify(purchaseRepository).findByGameIdWithGameAndAuthor(gameId);
    }

    @Test
    @DisplayName("Should get my sales as developer")
    void shouldGetMySalesAsDeveloper() {
        // Given
        when(userContextService.getAuthorizedUser()).thenReturn(developer);

        List<PurchaseHistory> sales = Arrays.asList(purchaseHistory);
        List<PurchaseResponse> responses = Arrays.asList(purchaseResponse);

        when(purchaseRepository.findSalesByDeveloperIdWithGame(2L))
                .thenReturn(sales);
        when(purchaseMapper.toPurchaseResponseList(sales))
                .thenReturn(responses);

        // When
        List<PurchaseResponse> result = purchaseService.getMySales();

        // Then
        assertThat(result).hasSize(1);
        verify(purchaseRepository).findSalesByDeveloperIdWithGame(2L);
    }

    @Test
    @DisplayName("Should get my total revenue as developer")
    void shouldGetMyTotalRevenueAsDeveloper() {
        // Given
        when(userContextService.getAuthorizedUser()).thenReturn(developer);
        BigDecimal revenue = new BigDecimal("1234.56");

        when(purchaseRepository.calculateTotalRevenueForDeveloper(2L))
                .thenReturn(Optional.of(revenue));

        // When
        BigDecimal result = purchaseService.getMyTotalRevenue();

        // Then
        assertThat(result).isEqualByComparingTo(revenue);
        verify(purchaseRepository).calculateTotalRevenueForDeveloper(2L);
    }

    @Test
    @DisplayName("Should return zero revenue when developer has no sales")
    void shouldReturnZeroRevenueWhenDeveloperHasNoSales() {
        // Given
        when(userContextService.getAuthorizedUser()).thenReturn(developer);

        when(purchaseRepository.calculateTotalRevenueForDeveloper(2L))
                .thenReturn(Optional.empty());

        // When
        BigDecimal result = purchaseService.getMyTotalRevenue();

        // Then
        assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Should handle empty purchase history")
    void shouldHandleEmptyPurchaseHistory() {
        // Given
        when(purchaseRepository.findByUserWithGameAndAuthor(currentUser))
                .thenReturn(Collections.emptyList());
        when(purchaseMapper.toPurchaseResponseList(Collections.emptyList()))
                .thenReturn(Collections.emptyList());

        // When
        List<PurchaseResponse> result = purchaseService.getMyPurchaseHistory();

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should throw exception when trying to purchase own games in bulk")
    void shouldThrowExceptionWhenTryingToPurchaseOwnGamesInBulk() {
        // Given
        Game ownGame = Game.builder()
                .id(3L)
                .title("My Own Game")
                .author(currentUser)
                .price(new BigDecimal("19.99"))
                .build();

        List<Long> gameIds = Arrays.asList(1L, 3L);
        List<Game> games = Arrays.asList(testGame, ownGame);
        PurchaseGamesRequest request = new PurchaseGamesRequest(gameIds);

        when(gameRepository.findAllByIdWithAuthor(gameIds)).thenReturn(games);
        when(purchaseRepository.findOwnedGameIds(1L, gameIds)).thenReturn(Collections.emptyList());

        // When & Then
        assertThatThrownBy(() -> purchaseService.purchaseGamesByIds(request))
                .isInstanceOf(PurchaseException.class)
                .hasMessageContaining("My Own Game");

        verify(purchaseRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("Should return false when user cannot afford games")
    void shouldReturnFalseWhenUserCannotAffordGames() {
        List<Long> gameIds = Arrays.asList(1L, 2L);
        List<Game> games = Arrays.asList(testGame, testGame2);

        when(gameRepository.findAllByIdWithAuthor(gameIds)).thenReturn(games);
        when(purchaseRepository.findOwnedGameIds(1L, gameIds)).thenReturn(Collections.emptyList());
        when(balanceService.canAfford(any(BigDecimal.class))).thenReturn(false);

        boolean result = purchaseService.canPurchaseGames(gameIds);

        assertThat(result).isFalse();
        verify(balanceService).canAfford(new BigDecimal("69.98"));
    }

    @Test
    @DisplayName("Should purchase multiple games directly")
    void shouldPurchaseMultipleGamesDirectly() {
        List<Game> games = Arrays.asList(testGame, testGame2);
        PurchaseHistory ph1 = purchaseHistory;
        PurchaseHistory ph2 = PurchaseHistory.builder()
                .id(2L).user(currentUser).game(testGame2).purchasePrice(new BigDecimal("39.99")).build();

        PurchaseResponse resp1 = purchaseResponse;
        PurchaseResponse resp2 = PurchaseResponse.builder().purchaseId(2L).gameTitle("Test Game 2").build();

        when(balanceService.createPurchaseTransaction(new BigDecimal("69.98"), currentUser)).thenReturn(balanceTransaction);
        when(purchaseMapper.createPurchase(currentUser, testGame)).thenReturn(ph1);
        when(purchaseMapper.createPurchase(currentUser, testGame2)).thenReturn(ph2);
        when(purchaseRepository.saveAll(anyList())).thenReturn(Arrays.asList(ph1, ph2));
        when(purchaseMapper.toPurchaseResponse(ph1)).thenReturn(resp1);
        when(purchaseMapper.toPurchaseResponse(ph2)).thenReturn(resp2);

        List<PurchaseResponse> results = purchaseService.purchaseGames(games, currentUser);

        assertThat(results).hasSize(2);
        verify(gameHistoryService).recordGamePurchases(games, currentUser);
    }

}