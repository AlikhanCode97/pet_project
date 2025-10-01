package com.example.Games.game;

import com.example.Games.config.common.dto.ApiResponse;
import com.example.Games.config.common.dto.PagedApiResponse;
import com.example.Games.config.common.mappers.ResponseMapStruct;
import com.example.Games.config.exception.game.GameNotFoundException;
import com.example.Games.config.exception.game.GameTitleAlreadyExistsException;
import com.example.Games.config.test.WebMvcTestWithoutSecurity;
import com.example.Games.game.dto.CreateRequest;
import com.example.Games.game.dto.PagedResponse;
import com.example.Games.game.dto.Response;
import com.example.Games.game.dto.UpdateRequest;
import com.example.Games.category.dto.CategoryResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTestWithoutSecurity(GameController.class)
@DisplayName("GameController Tests")
class GameControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private GameService gameService;

    @MockitoBean
    private ResponseMapStruct responseMapper;

    @Test
    @DisplayName("Should create game successfully")
    void shouldCreateGameSuccessfully() throws Exception {
        // Given
        CreateRequest request = new CreateRequest(
                "New Adventure Game",
                new BigDecimal("39.99"),
                1L
        );

        CategoryResponse categoryResponse = new CategoryResponse(
                1L, "Action", "gamedev", 1L, LocalDateTime.now(), LocalDateTime.now()
        );


        Response gameResponse = new Response(
                1L,
                "New Adventure Game",
                "gamedev",
                new BigDecimal("39.99"),
                categoryResponse,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        ApiResponse<Response> apiResponse = ApiResponse.success("Game created successfully", gameResponse);

        when(gameService.createGame(request)).thenReturn(gameResponse);
        when(responseMapper.toSuccessResponse("Game created successfully", gameResponse))
                .thenReturn(apiResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/games")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Game created successfully"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.title").value("New Adventure Game"))
                .andExpect(jsonPath("$.data.author").value("gamedev"))
                .andExpect(jsonPath("$.data.price").value(39.99))
                .andExpect(jsonPath("$.data.category.name").value("Action"))
                .andExpect(jsonPath("$.data.createdAt").exists())
                .andExpect(jsonPath("$.data.updatedAt").exists());

        verify(gameService).createGame(request);
        verify(responseMapper).toSuccessResponse("Game created successfully", gameResponse);
    }

    @Test
    @DisplayName("Should handle invalid create request")
    void shouldHandleInvalidCreateRequest() throws Exception {
        // Given - Invalid request with null title
        CreateRequest invalidRequest = new CreateRequest(
                null, // Invalid
                new BigDecimal("39.99"),
                1L
        );

        // When & Then
        mockMvc.perform(post("/api/v1/games")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(gameService, never()).createGame(any());
        verify(responseMapper, never()).toSuccessResponse(anyString(), any());
    }

    @Test
    @DisplayName("Should retrieve game by ID successfully")
    void shouldRetrieveGameByIdSuccessfully() throws Exception {
        // Given
        Long gameId = 1L;
        CategoryResponse categoryResponse = new CategoryResponse(
                1L, "Action", "gamedev", 1L, LocalDateTime.now(), LocalDateTime.now()
        );

        Response gameResponse = new Response(
                1L,
                "Test Game",
                "gamedev",
                new BigDecimal("29.99"),
                categoryResponse,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        ApiResponse<Response> apiResponse = ApiResponse.success("Game retrieved successfully", gameResponse);

        when(gameService.getGameById(gameId)).thenReturn(gameResponse);
        when(responseMapper.toSuccessResponse("Game retrieved successfully", gameResponse))
                .thenReturn(apiResponse);


        // When & Then
        mockMvc.perform(get("/api/v1/games/{id}", gameId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Game retrieved successfully"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.title").value("Test Game"))
                .andExpect(jsonPath("$.data.author").value("gamedev"))
                .andExpect(jsonPath("$.data.price").value(29.99))
                .andExpect(jsonPath("$.data.category.name").value("Action"));

        verify(gameService).getGameById(gameId);
        verify(responseMapper).toSuccessResponse("Game retrieved successfully", gameResponse);
    }

    @Test
    @DisplayName("Should retrieve all games successfully")
    void shouldRetrieveAllGamesSuccessfully() throws Exception {
        // Given
        CategoryResponse categoryResponse = new CategoryResponse(
                1L, "Action", "gamedev", 1L, LocalDateTime.now(), LocalDateTime.now()
        );
        Response game1 = new Response(1L, "Game 1", "dev1", new BigDecimal("19.99"),
                categoryResponse, LocalDateTime.now(), LocalDateTime.now());
        Response game2 = new Response(2L, "Game 2", "dev2", new BigDecimal("29.99"),
                categoryResponse, LocalDateTime.now(), LocalDateTime.now());

        List<Response> games = List.of(game1, game2);
        ApiResponse<List<Response>> apiResponse = ApiResponse.success("Games retrieved successfully", games);

        when(gameService.getAllGames()).thenReturn(games);
        when(responseMapper.toSuccessResponse("Games retrieved successfully", games))
                .thenReturn(apiResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/games")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Games retrieved successfully"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].title").value("Game 1"))
                .andExpect(jsonPath("$.data[1].id").value(2))
                .andExpect(jsonPath("$.data[1].title").value("Game 2"));

        verify(gameService).getAllGames();
        verify(responseMapper).toSuccessResponse("Games retrieved successfully", games);
    }
    @Test
    @DisplayName("Should delete game successfully")
    void shouldDeleteGameSuccessfully() throws Exception {
        // Given
        Long gameId = 1L;
        ApiResponse<Object> apiResponse = ApiResponse.success("Game deleted successfully", null);

        doNothing().when(gameService).deleteGame(gameId);
        when(responseMapper.toSuccessResponse("Game deleted successfully"))
                .thenReturn(apiResponse);

        // When & Then
        mockMvc.perform(delete("/api/v1/games/{id}", gameId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Game deleted successfully"));

        verify(gameService).deleteGame(gameId);
        verify(responseMapper).toSuccessResponse("Game deleted successfully");
    }

    @Test
    @DisplayName("Should search games by title successfully")
    void shouldSearchGamesByTitleSuccessfully() throws Exception {
        // Given
        String searchTitle = "Adventure";
        CategoryResponse categoryResponse = new CategoryResponse(
                1L, "Action", "gamedev", 1L, LocalDateTime.now(), LocalDateTime.now()
        );


        Response game1 = new Response(1L, "Adventure Quest", "dev1", new BigDecimal("29.99"),
                categoryResponse, LocalDateTime.now(), LocalDateTime.now());

        List<Response> searchResults = List.of(game1);
        ApiResponse<List<Response>> apiResponse = ApiResponse.success("Search completed", searchResults);

        when(gameService.searchByTitle(searchTitle)).thenReturn(searchResults);
        when(responseMapper.toSuccessResponse("Search completed", searchResults))
                .thenReturn(apiResponse);

        // When & Then - Note: endpoint is /search/title not /search
        mockMvc.perform(get("/api/v1/games/search/title")
                        .param("title", searchTitle)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Search completed"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].title").value("Adventure Quest"));

        verify(gameService).searchByTitle(searchTitle);
        verify(responseMapper).toSuccessResponse("Search completed", searchResults);
    }

    @Test
    @DisplayName("Should search games by author successfully")
    void shouldSearchGamesByAuthorSuccessfully() throws Exception {
        // Given
        String authorName = "gamedev";
        CategoryResponse categoryResponse = new CategoryResponse(
                1L, "Action", "gamedev", 1L, LocalDateTime.now(), LocalDateTime.now()
        );

        Response game1 = new Response(1L, "Dev Game", "gamedev", new BigDecimal("29.99"),
                categoryResponse, LocalDateTime.now(), LocalDateTime.now());

        List<Response> searchResults = List.of(game1);
        ApiResponse<List<Response>> apiResponse = ApiResponse.success("Search completed", searchResults);

        when(gameService.searchGamesByAuthor(authorName)).thenReturn(searchResults);
        when(responseMapper.toSuccessResponse("Search completed", searchResults))
                .thenReturn(apiResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/games/search/author")
                        .param("author", authorName)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Search completed"))
                .andExpect(jsonPath("$.data[0].author").value("gamedev"));

        verify(gameService).searchGamesByAuthor(authorName);
    }

    @Test
    @DisplayName("Should filter games by price range successfully")
    void shouldFilterGamesByPriceRangeSuccessfully() throws Exception {
        // Given
        BigDecimal minPrice = new BigDecimal("20.00");
        BigDecimal maxPrice = new BigDecimal("50.00");

        CategoryResponse categoryResponse = new CategoryResponse(
                1L, "Action", "gamedev", 1L, LocalDateTime.now(), LocalDateTime.now()
        );

        Response game1 = new Response(1L, "Mid Price Game", "dev1", new BigDecimal("35.99"),
                categoryResponse, LocalDateTime.now(), LocalDateTime.now());

        List<Response> filteredGames = List.of(game1);
        ApiResponse<List<Response>> apiResponse = ApiResponse.success("Price filter applied", filteredGames);

        when(gameService.getGamesInPriceRange(minPrice, maxPrice)).thenReturn(filteredGames);
        when(responseMapper.toSuccessResponse("Price filter applied", filteredGames))
                .thenReturn(apiResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/games/filter/price")
                        .param("min", "20.00")
                        .param("max", "50.00")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Price filter applied"))
                .andExpect(jsonPath("$.data[0].price").value(35.99));

        verify(gameService).getGamesInPriceRange(minPrice, maxPrice);
    }

    @Test
    @DisplayName("Should get sorted games by price successfully")
    void shouldGetSortedGamesByPriceSuccessfully() throws Exception {
        // Given
        CategoryResponse categoryResponse = new CategoryResponse(
                1L, "Action", "gamedev", 1L, LocalDateTime.now(), LocalDateTime.now()
        );


        List<Response> sortedGames = List.of(
                new Response(1L, "Cheap Game", "dev1", new BigDecimal("19.99"), categoryResponse, LocalDateTime.now(), LocalDateTime.now()),
                new Response(2L, "Expensive Game", "dev2", new BigDecimal("59.99"), categoryResponse, LocalDateTime.now(), LocalDateTime.now())
        );

        ApiResponse<List<Response>> apiResponse = ApiResponse.success("Games sorted by price ascending", sortedGames);

        when(gameService.getGamesSortedByPrice(true)).thenReturn(sortedGames);
        when(responseMapper.toSuccessResponse("Games sorted by price ascending", sortedGames))
                .thenReturn(apiResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/games/sorted")
                        .param("ascending", "true")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Games sorted by price ascending"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2));

        verify(gameService).getGamesSortedByPrice(true);
    }

    @Test
    @DisplayName("Should update game successfully")
    void shouldUpdateGameSuccessfully() throws Exception {
        Long gameId = 1L;
        UpdateRequest updateRequest = new UpdateRequest("Updated Game", new BigDecimal("49.99"), 1L);

        Response updatedResponse = new Response(
                1L, "Updated Game", "gamedev", new BigDecimal("49.99"),
                new CategoryResponse(1L, "Action", "gamedev", 1L, LocalDateTime.now(), LocalDateTime.now()),
                LocalDateTime.now(), LocalDateTime.now()
        );

        when(gameService.updateGame(eq(gameId), any(UpdateRequest.class))).thenReturn(updatedResponse);
        when(responseMapper.toSuccessResponse(eq("Game updated successfully"), eq(updatedResponse)))
                .thenReturn(ApiResponse.success("Game updated successfully", updatedResponse));

        mockMvc.perform(put("/api/v1/games/{id}", gameId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Game updated successfully"))
                .andExpect(jsonPath("$.data.title").value("Updated Game"))
                .andExpect(jsonPath("$.data.price").value(49.99));
    }

    @Test
    @DisplayName("Should return 404 when game not found by ID")
    void shouldReturn404WhenGameNotFoundById() throws Exception {
        Long gameId = 999L;

        when(gameService.getGameById(gameId))
                .thenThrow(new GameNotFoundException("Game with ID " + gameId + " not found"));
        when(responseMapper.toErrorResponse(anyString()))
                .thenReturn(ApiResponse.error("Game with ID " + gameId + " not found"));

        mockMvc.perform(get("/api/v1/games/{id}", gameId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Game with ID 999 not found"));
    }

    @Test
    @DisplayName("Should return 404 when game not found by title")
    void shouldReturn404WhenGameNotFoundByTitle() throws Exception {
        String title = "Nonexistent";

        when(gameService.getGameByTitle(title))
                .thenThrow(new GameNotFoundException("Game with title " + title + " not found"));
        when(responseMapper.toErrorResponse(anyString()))
                .thenReturn(ApiResponse.error("Game with title " + title + " not found"));

        mockMvc.perform(get("/api/v1/games/title/{title}", title))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Game with title Nonexistent not found"));

        verify(gameService).getGameByTitle(title);
    }


    @Test
    @DisplayName("Should return 409 when creating game with duplicate title")
    void shouldReturn409WhenCreatingGameWithDuplicateTitle() throws Exception {
        CreateRequest request = new CreateRequest(
                "Duplicate Title",
                new BigDecimal("29.99"),
                1L
        );

        when(gameService.createGame(request))
                .thenThrow(new GameTitleAlreadyExistsException("Game with title Duplicate Title already exists"));
        when(responseMapper.toErrorResponse(anyString()))
                .thenReturn(ApiResponse.error("Game with title Duplicate Title already exists"));

        mockMvc.perform(post("/api/v1/games")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Game with title Duplicate Title already exists"));

        verify(gameService).createGame(request);
    }

    @Test
    @DisplayName("Should retrieve games by category with pagination successfully")
    void shouldRetrieveGamesByCategoryWithPaginationSuccessfully() throws Exception {
        // Given
        Long categoryId = 1L;
        int page = 0;
        int size = 2;

        CategoryResponse categoryResponse = new CategoryResponse(
                categoryId, "Action", "gamedev", 1L, LocalDateTime.now(), LocalDateTime.now()
        );

        Response game1 = new Response(
                1L, "Game One", "gamedev", new BigDecimal("19.99"),
                categoryResponse, LocalDateTime.now(), LocalDateTime.now()
        );

        Response game2 = new Response(
                2L, "Game Two", "gamedev", new BigDecimal("29.99"),
                categoryResponse, LocalDateTime.now(), LocalDateTime.now()
        );

        PagedResponse pagedResponse = new PagedResponse(
                List.of(game1, game2),
                page,
                size,
                2, // total elements
                1  // total pages
        );

        ApiResponse<PagedResponse> apiResponse =
                ApiResponse.success("Games retrieved for category", pagedResponse);

        when(gameService.getGamesByCategoryPaged(categoryId, page, size))
                .thenReturn(pagedResponse);
        when(responseMapper.toSuccessResponse("Games retrieved for category", pagedResponse))
                .thenReturn(apiResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/games/category/{categoryId}", categoryId)
                        .param("page", String.valueOf(page))
                        .param("size", String.valueOf(size))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Games retrieved for category"))
                .andExpect(jsonPath("$.data.games").isArray())
                .andExpect(jsonPath("$.data.games.length()").value(2))
                .andExpect(jsonPath("$.data.pageNumber").value(page))
                .andExpect(jsonPath("$.data.pageSize").value(size))
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.totalPages").value(1));

        verify(gameService).getGamesByCategoryPaged(categoryId, page, size);
        verify(responseMapper).toSuccessResponse("Games retrieved for category", pagedResponse);
    }

}