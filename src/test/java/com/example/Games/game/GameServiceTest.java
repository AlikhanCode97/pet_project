package com.example.Games.game;

import com.example.Games.category.Category;
import com.example.Games.category.CategoryRepository;
import com.example.Games.category.dto.CategoryResponse;
import com.example.Games.config.common.service.UserContextService;
import com.example.Games.config.exception.category.CategoryNotFoundException;
import com.example.Games.config.exception.game.GameNotFoundException;
import com.example.Games.config.exception.game.GameTitleAlreadyExistsException;
import com.example.Games.config.exception.game.UnauthorizedGameAccessException;
import com.example.Games.game.dto.CreateRequest;
import com.example.Games.game.dto.PagedResponse;
import com.example.Games.game.dto.Response;
import com.example.Games.game.dto.UpdateRequest;
import com.example.Games.gameHistory.GameHistoryService;
import com.example.Games.gameHistory.dto.FieldChange;
import com.example.Games.user.auth.User;
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
import org.springframework.data.domain.Sort;

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
@DisplayName("GameService Tests")
class GameServiceTest {

    @Mock
    private GameRepository gameRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private GameMapStruct gameMapStruct;

    @Mock
    private GameHistoryService historyService;

    @Mock
    private UserContextService userContextService;

    @InjectMocks
    private GameService gameService;

    @Captor
    private ArgumentCaptor<Game> gameCaptor;

    @Captor
    private ArgumentCaptor<List<FieldChange>> changesCaptor;

    private User testUser;
    private User otherUser;
    private Category testCategory;
    private Category otherCategory;
    private Game testGame;
    private Game testGame2;
    private Response testGameResponse;
    private Response testGameResponse2;
    private CategoryResponse categoryResponse;

    @BeforeEach
    void setUp() {
        // Setup roles
        Role developerRole = Role.builder()
                .id(1L)
                .name(RoleType.DEVELOPER)
                .build();

        // Setup users
        testUser = User.builder()
                .id(1L)
                .username("gamedev")
                .email("dev@example.com")
                .password("password123")
                .role(developerRole)
                .build();

        otherUser = User.builder()
                .id(2L)
                .username("othergamedev")
                .email("other@example.com")
                .password("password123")
                .role(developerRole)
                .build();

        // Setup categories
        testCategory = Category.builder()
                .id(1L)
                .name("Action")
                .build();

        otherCategory = Category.builder()
                .id(2L)
                .name("RPG")
                .build();

        // Setup games
        testGame = Game.builder()
                .id(1L)
                .title("Test Game")
                .author(testUser)
                .price(new BigDecimal("29.99"))
                .category(testCategory)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        testGame2 = Game.builder()
                .id(2L)
                .title("Second Game")
                .author(otherUser)
                .price(new BigDecimal("39.99"))
                .category(testCategory)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // Setup responses
        categoryResponse = new CategoryResponse(
                1L,
                "Action",
                "gamedev",
                1L,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        testGameResponse = new Response(
                1L,
                "Test Game",
                "gamedev",
                new BigDecimal("29.99"),
                categoryResponse,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        testGameResponse2 = new Response(
                2L,
                "Second Game",
                "othergamedev",
                new BigDecimal("39.99"),
                categoryResponse,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    @Test
    @DisplayName("Should create game successfully")
    void shouldCreateGameSuccessfully() {
        // Given
        CreateRequest request = new CreateRequest(
                "New Adventure Game",
                new BigDecimal("39.99"),
                1L
        );

        when(userContextService.getAuthorizedUser()).thenReturn(testUser);
        when(gameRepository.findByTitle("New Adventure Game")).thenReturn(Optional.empty());
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(gameMapStruct.toEntity(request, testCategory, testUser)).thenReturn(testGame);
        when(gameRepository.save(any(Game.class))).thenReturn(testGame);
        when(gameMapStruct.toDto(testGame)).thenReturn(testGameResponse);
        doNothing().when(historyService).recordGameCreation(any(Game.class), any(User.class));

        // When
        Response result = gameService.createGame(request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(testGameResponse);

        verify(userContextService).getAuthorizedUser();
        verify(gameRepository).findByTitle("New Adventure Game");
        verify(categoryRepository).findById(1L);
        verify(gameRepository).save(any(Game.class));
        verify(historyService).recordGameCreation(eq(testGame), eq(testUser));
    }

    @Test
    @DisplayName("Should throw exception when title already exists during creation")
    void shouldThrowExceptionWhenTitleAlreadyExistsDuringCreation() {
        // Given
        CreateRequest request = new CreateRequest(
                "Test Game",
                new BigDecimal("39.99"),
                1L
        );

        when(userContextService.getAuthorizedUser()).thenReturn(testUser);
        when(gameRepository.findByTitle("Test Game")).thenReturn(Optional.of(testGame));

        // When & Then
        assertThatThrownBy(() -> gameService.createGame(request))
                .isInstanceOf(GameTitleAlreadyExistsException.class)
                .hasMessageContaining("Test Game");

        verify(gameRepository, never()).save(any());
        verify(historyService, never()).recordGameCreation(any(), any());
    }

    @Test
    @DisplayName("Should throw exception when category not found during creation")
    void shouldThrowExceptionWhenCategoryNotFoundDuringCreation() {
        // Given
        CreateRequest request = new CreateRequest(
                "New Game",
                new BigDecimal("29.99"),
                999L
        );

        when(userContextService.getAuthorizedUser()).thenReturn(testUser);
        when(gameRepository.findByTitle("New Game")).thenReturn(Optional.empty());
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> gameService.createGame(request))
                .isInstanceOf(CategoryNotFoundException.class)
                .hasMessageContaining("999");

        verify(gameRepository, never()).save(any());
        verify(historyService, never()).recordGameCreation(any(), any());
    }


    @Test
    @DisplayName("Should update game successfully")
    void shouldUpdateGameSuccessfully() {
        // Given
        UpdateRequest request = new UpdateRequest(
                "Updated Title",
                new BigDecimal("24.99"),
                2L
        );

        when(userContextService.getAuthorizedUser()).thenReturn(testUser);
        when(gameRepository.findByIdWithAuthor(1L)).thenReturn(Optional.of(testGame));
        when(gameRepository.findByTitle("Updated Title")).thenReturn(Optional.empty());
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(otherCategory));
        when(gameRepository.findCategoryIdByGameId(1L)).thenReturn(Optional.of(1L));
        when(gameRepository.save(any(Game.class))).thenReturn(testGame);
        when(gameMapStruct.toDto(any(Game.class))).thenReturn(testGameResponse);

        // When
        Response result = gameService.updateGame(1L, request);

        // Then
        assertThat(result).isNotNull();

        verify(gameRepository).save(gameCaptor.capture());
        Game savedGame = gameCaptor.getValue();
        assertThat(savedGame).isNotNull();

        verify(historyService).recordGameUpdates(eq(testGame), changesCaptor.capture(), eq(testUser));
        List<FieldChange> changes = changesCaptor.getValue();
        assertThat(changes).hasSize(3);
    }

    @Test
    @DisplayName("Should throw exception when updating non-existent game")
    void shouldThrowWhenUpdatingNonExistentGame() {
        UpdateRequest request = new UpdateRequest("Doesn't Matter", null, null);

        when(userContextService.getAuthorizedUser()).thenReturn(testUser);
        when(gameRepository.findByIdWithAuthor(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> gameService.updateGame(999L, request))
                .isInstanceOf(GameNotFoundException.class)
                .hasMessageContaining("999");

        verify(gameRepository, never()).save(any());
        verify(historyService, never()).recordGameUpdates(any(), any(), any());
    }


    @Test
    @DisplayName("Should throw exception when updating game title that already exists")
    void shouldThrowExceptionWhenUpdatingGameTitleThatAlreadyExists() {
        // Given
        UpdateRequest request = new UpdateRequest(
                "Existing Title",
                null,
                null
        );

        Game existingGame = Game.builder()
                .id(3L)
                .title("Existing Title")
                .build();

        when(userContextService.getAuthorizedUser()).thenReturn(testUser);
        when(gameRepository.findByIdWithAuthor(1L)).thenReturn(Optional.of(testGame));
        when(gameRepository.findByTitle("Existing Title")).thenReturn(Optional.of(existingGame));

        // When & Then
        assertThatThrownBy(() -> gameService.updateGame(1L, request))
                .isInstanceOf(GameTitleAlreadyExistsException.class)
                .hasMessageContaining("Existing Title");

        verify(gameRepository, never()).save(any());
        verify(historyService, never()).recordGameUpdates(any(), any(), any());
    }

    @Test
    @DisplayName("Should throw exception when updating with non-existent category")
    void shouldThrowWhenUpdatingWithNonExistentCategory() {
        UpdateRequest request = new UpdateRequest(null, null, 999L);

        when(userContextService.getAuthorizedUser()).thenReturn(testUser);
        when(gameRepository.findByIdWithAuthor(1L)).thenReturn(Optional.of(testGame));
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> gameService.updateGame(1L, request))
                .isInstanceOf(CategoryNotFoundException.class)
                .hasMessageContaining("999");

        verify(gameRepository, never()).save(any());
    }


    @Test
    @DisplayName("Should throw exception when unauthorized user tries to update game")
    void shouldThrowExceptionWhenUnauthorizedUserTriesToUpdateGame() {
        // Given
        UpdateRequest request = new UpdateRequest(
                "Updated Title",
                null,
                null
        );

        when(userContextService.getAuthorizedUser()).thenReturn(otherUser);
        when(gameRepository.findByIdWithAuthor(1L)).thenReturn(Optional.of(testGame));

        // When & Then
        assertThatThrownBy(() -> gameService.updateGame(1L, request))
                .isInstanceOf(UnauthorizedGameAccessException.class)
                .hasMessageContaining("othergamedev");

        verify(gameRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should retrieve game by ID successfully")
    void shouldRetrieveGameByIdSuccessfully() {
        // Given
        when(gameRepository.findById(1L)).thenReturn(Optional.of(testGame));
        when(gameMapStruct.toDto(testGame)).thenReturn(testGameResponse);

        // When
        Response result = gameService.getGameById(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(testGameResponse);
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.title()).isEqualTo("Test Game");

        verify(gameRepository).findById(1L);
        verify(gameMapStruct).toDto(testGame);
    }

    @Test
    @DisplayName("Should throw exception when game not found by ID")
    void shouldThrowExceptionWhenGameNotFoundById() {
        // Given
        when(gameRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> gameService.getGameById(999L))
                .isInstanceOf(GameNotFoundException.class)
                .hasMessageContaining("999");

        verify(gameMapStruct, never()).toDto(any());
    }

    @Test
    @DisplayName("Should retrieve game by title successfully")
    void shouldRetrieveGameByTitleSuccessfully() {
        // Given
        when(gameRepository.findByTitle("Test Game")).thenReturn(Optional.of(testGame));
        when(gameMapStruct.toDto(testGame)).thenReturn(testGameResponse);

        // When
        Response result = gameService.getGameByTitle("Test Game");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.title()).isEqualTo("Test Game");

        verify(gameRepository).findByTitle("Test Game");
        verify(gameMapStruct).toDto(testGame);
    }

    @Test
    @DisplayName("Should throw exception when game not found by title")
    void shouldThrowExceptionWhenGameNotFoundByTitle() {
        // Given
        when(gameRepository.findByTitle("Nonexistent")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> gameService.getGameByTitle("Nonexistent"))
                .isInstanceOf(GameNotFoundException.class)
                .hasMessageContaining("Nonexistent");
    }

    @Test
    @DisplayName("Should retrieve all games successfully")
    void shouldRetrieveAllGamesSuccessfully() {
        // Given
        List<Game> allGames = Arrays.asList(testGame, testGame2);

        when(gameRepository.findAll()).thenReturn(allGames);
        when(gameMapStruct.toDto(testGame)).thenReturn(testGameResponse);
        when(gameMapStruct.toDto(testGame2)).thenReturn(testGameResponse2);

        // When
        List<Response> result = gameService.getAllGames();

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result).containsExactlyInAnyOrder(testGameResponse, testGameResponse2);

        verify(gameRepository).findAll();
    }

    @Test
    @DisplayName("Should delete game successfully")
    void shouldDeleteGameSuccessfully() {
        // Given
        when(gameRepository.findByIdWithAuthor(1L)).thenReturn(Optional.of(testGame));
        when(userContextService.getAuthorizedUser()).thenReturn(testUser);
        doNothing().when(historyService).recordGameDeletion(any(Game.class), any(User.class));
        doNothing().when(gameRepository).deleteById(1L);

        // When
        gameService.deleteGame(1L);

        // Then
        verify(gameRepository).findByIdWithAuthor(1L);
        verify(userContextService).getAuthorizedUser();
        verify(historyService).recordGameDeletion(eq(testGame), eq(testUser));
        verify(gameRepository).deleteById(1L);
    }

    @Test
    @DisplayName("Should throw exception when deleting non-existent game")
    void shouldThrowWhenDeletingNonExistentGame() {
        when(gameRepository.findByIdWithAuthor(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> gameService.deleteGame(999L))
                .isInstanceOf(GameNotFoundException.class)
                .hasMessageContaining("999");

        verify(gameRepository, never()).deleteById(any());
    }


    @Test
    @DisplayName("Should throw exception when unauthorized user tries to delete game")
    void shouldThrowExceptionWhenUnauthorizedUserTriesToDeleteGame() {
        // Given
        when(gameRepository.findByIdWithAuthor(1L)).thenReturn(Optional.of(testGame));
        when(userContextService.getAuthorizedUser()).thenReturn(otherUser);

        // When & Then
        assertThatThrownBy(() -> gameService.deleteGame(1L))
                .isInstanceOf(UnauthorizedGameAccessException.class)
                .hasMessageContaining("othergamedev");

        verify(historyService, never()).recordGameDeletion(any(), any());
        verify(gameRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("Should search games by title successfully")
    void shouldSearchGamesByTitleSuccessfully() {
        // Given
        List<Game> foundGames = Arrays.asList(testGame);

        when(gameRepository.findByTitleContainingIgnoreCase("Test")).thenReturn(foundGames);
        when(gameMapStruct.toDto(testGame)).thenReturn(testGameResponse);

        // When
        List<Response> result = gameService.searchByTitle("Test");

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result).contains(testGameResponse);

        verify(gameRepository).findByTitleContainingIgnoreCase("Test");
    }

    @Test
    @DisplayName("Should search games by author successfully")
    void shouldSearchGamesByAuthorSuccessfully() {
        // Given
        List<Game> authorGames = Arrays.asList(testGame);

        when(gameRepository.findByAuthor_Username("gamedev")).thenReturn(authorGames);
        when(gameMapStruct.toDto(testGame)).thenReturn(testGameResponse);

        // When
        List<Response> result = gameService.searchGamesByAuthor("gamedev");

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result).contains(testGameResponse);

        verify(gameRepository).findByAuthor_Username("gamedev");
    }

    @Test
    @DisplayName("Should find games in price range successfully")
    void shouldFindGamesInPriceRangeSuccessfully() {
        // Given
        BigDecimal minPrice = new BigDecimal("20.00");
        BigDecimal maxPrice = new BigDecimal("50.00");
        List<Game> gamesInRange = Arrays.asList(testGame);

        when(gameRepository.findGamesInPriceRange(minPrice, maxPrice)).thenReturn(gamesInRange);
        when(gameMapStruct.toDto(testGame)).thenReturn(testGameResponse);

        // When
        List<Response> result = gameService.getGamesInPriceRange(minPrice, maxPrice);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result).contains(testGameResponse);

        verify(gameRepository).findGamesInPriceRange(minPrice, maxPrice);
    }

    @Test
    @DisplayName("Should get games sorted by price ascending")
    void shouldGetGamesSortedByPriceAscending() {
        // Given
        List<Game> sortedGames = Arrays.asList(testGame, testGame2);

        when(gameRepository.findAllByOrderByPriceAsc()).thenReturn(sortedGames);
        when(gameMapStruct.toDto(testGame)).thenReturn(testGameResponse);
        when(gameMapStruct.toDto(testGame2)).thenReturn(testGameResponse2);

        // When
        List<Response> result = gameService.getGamesSortedByPrice(true);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(testGameResponse, testGameResponse2);

        verify(gameRepository).findAllByOrderByPriceAsc();
        verify(gameRepository, never()).findAllByOrderByPriceDesc();
    }

    @Test
    @DisplayName("Should get games sorted by price descending")
    void shouldGetGamesSortedByPriceDescending() {
        // Given
        List<Game> sortedGames = Arrays.asList(testGame2, testGame);

        when(gameRepository.findAllByOrderByPriceDesc()).thenReturn(sortedGames);
        when(gameMapStruct.toDto(testGame2)).thenReturn(testGameResponse2);
        when(gameMapStruct.toDto(testGame)).thenReturn(testGameResponse);

        // When
        List<Response> result = gameService.getGamesSortedByPrice(false);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(testGameResponse2, testGameResponse);

        verify(gameRepository).findAllByOrderByPriceDesc();
        verify(gameRepository, never()).findAllByOrderByPriceAsc();
    }

    @Test
    @DisplayName("Should retrieve games by category with pagination")
    void shouldRetrieveGamesByCategoryWithPagination() {
        // Given
        Long categoryId = 1L;
        int page = 0;
        int size = 2;

        List<Game> categoryGames = Arrays.asList(testGame);
        Pageable pageable = PageRequest.of(page, size, Sort.by("title"));
        Page<Game> gamesPage = new PageImpl<>(categoryGames, pageable, 1);

        when(gameRepository.findByCategoryId(eq(categoryId), any(Pageable.class))).thenReturn(gamesPage);
        when(gameMapStruct.toDto(testGame)).thenReturn(testGameResponse);

        // When
        PagedResponse result = gameService.getGamesByCategoryPaged(categoryId, page, size);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.games()).hasSize(1);
        assertThat(result.games()).contains(testGameResponse);
        assertThat(result.pageNumber()).isEqualTo(0);
        assertThat(result.pageSize()).isEqualTo(2);
        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.totalPages()).isEqualTo(1);

        verify(gameRepository).findByCategoryId(eq(categoryId), any(Pageable.class));
    }

    @Test
    @DisplayName("Should handle empty results appropriately")
    void shouldHandleEmptyResultsAppropriately() {
        // Test empty game list
        when(gameRepository.findAll()).thenReturn(Collections.emptyList());
        List<Response> emptyResult = gameService.getAllGames();
        assertThat(emptyResult).isEmpty();

        // Test empty search results
        when(gameRepository.findByTitleContainingIgnoreCase("NonExistent")).thenReturn(Collections.emptyList());
        List<Response> emptySearch = gameService.searchByTitle("NonExistent");
        assertThat(emptySearch).isEmpty();

        // Test empty author search
        when(gameRepository.findByAuthor_Username("unknown")).thenReturn(Collections.emptyList());
        List<Response> emptyAuthorSearch = gameService.searchGamesByAuthor("unknown");
        assertThat(emptyAuthorSearch).isEmpty();

        // Test empty price range
        when(gameRepository.findGamesInPriceRange(any(), any())).thenReturn(Collections.emptyList());
        List<Response> emptyPriceRange = gameService.getGamesInPriceRange(BigDecimal.ONE, BigDecimal.TEN);
        assertThat(emptyPriceRange).isEmpty();
    }

    @Test
    @DisplayName("Should update only specified fields")
    void shouldUpdateOnlySpecifiedFields() {
        UpdateRequest requestTitleOnly = new UpdateRequest(
                "New Title",
                null,
                null
        );

        when(userContextService.getAuthorizedUser()).thenReturn(testUser);
        when(gameRepository.findByIdWithAuthor(1L)).thenReturn(Optional.of(testGame));
        when(gameRepository.findByTitle("New Title")).thenReturn(Optional.empty());
        when(gameRepository.save(any(Game.class))).thenReturn(testGame);
        when(gameMapStruct.toDto(any(Game.class))).thenReturn(testGameResponse);

        // When
        Response result = gameService.updateGame(1L, requestTitleOnly);

        // Then
        assertThat(result).isNotNull();

        verify(historyService).recordGameUpdates(eq(testGame), changesCaptor.capture(), eq(testUser));
        List<FieldChange> changes = changesCaptor.getValue();
        assertThat(changes).hasSize(1);
        assertThat(changes.get(0).fieldName()).isEqualTo("title");

        verify(categoryRepository, never()).findById(any());
    }
}