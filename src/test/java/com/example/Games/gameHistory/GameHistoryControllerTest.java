package com.example.Games.gameHistory;

import com.example.Games.config.common.dto.ApiResponse;
import com.example.Games.config.common.mappers.ResponseMapStruct;
import com.example.Games.config.test.WebMvcTestWithoutSecurity;
import com.example.Games.gameHistory.dto.DeveloperActivityResponse;
import com.example.Games.gameHistory.dto.GameHistoryResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTestWithoutSecurity(GameHistoryController.class)
@DisplayName("GameHistoryController Tests")
class GameHistoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private GameHistoryService historyService;

    @MockitoBean
    private ResponseMapStruct responseMapper;

    @Test
    @DisplayName("Should retrieve game history successfully")
    void shouldRetrieveGameHistorySuccessfully() throws Exception {
        // Given
        Long gameId = 1L;
        GameHistoryResponse historyResponse = new GameHistoryResponse(
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

        List<GameHistoryResponse> historyList = List.of(historyResponse);
        Page<GameHistoryResponse> historyPage = new PageImpl<>(historyList, PageRequest.of(0, 10), 1);
        ApiResponse<Page<GameHistoryResponse>> apiResponse = ApiResponse.success("Game history retrieved", historyPage);

        when(historyService.getGameHistory(gameId, 0, 10)).thenReturn(historyPage);
        when(responseMapper.toSuccessResponse("Game history retrieved", historyPage))
                .thenReturn(apiResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/history/game/{gameId}", gameId)
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Game history retrieved"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].id").value(1))
                .andExpect(jsonPath("$.data.content[0].game_id").value(1))
                .andExpect(jsonPath("$.data.content[0].game_title").value("Test Game"))
                .andExpect(jsonPath("$.data.content[0].action_type").value("CREATE"))
                .andExpect(jsonPath("$.data.content[0].changed_by").value("gamedev"))
                .andExpect(jsonPath("$.data.content[0].changed_at").exists())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.totalPages").value(1));

        verify(historyService).getGameHistory(gameId, 0, 10);
        verify(responseMapper).toSuccessResponse("Game history retrieved", historyPage);
    }

    @Test
    @DisplayName("Should retrieve developer activity successfully")
    void shouldRetrieveDeveloperActivitySuccessfully() throws Exception {
        // Given
        Long developerId = 1L;
        GameHistoryResponse recentActivity = new GameHistoryResponse(
                1L, 1L, "Recent Game", "UPDATE", "Game Updated",
                "title", "Old Title", "New Title", "gamedev", LocalDateTime.now(), "Title updated"
        );

        DeveloperActivityResponse activityResponse = new DeveloperActivityResponse(
                1L,
                "gamedev",
                "dev@example.com",
                5L,
                15L,
                List.of(recentActivity),
                LocalDateTime.now().minusDays(30),
                LocalDateTime.now()
        );

        ApiResponse<DeveloperActivityResponse> apiResponse = ApiResponse.success("Developer activity retrieved", activityResponse);

        when(historyService.getDeveloperActivity(developerId)).thenReturn(activityResponse);
        when(responseMapper.toSuccessResponse("Developer activity retrieved", activityResponse))
                .thenReturn(apiResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/history/developer/{developerId}", developerId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Developer activity retrieved"))
                .andExpect(jsonPath("$.data.developerId").value(1))
                .andExpect(jsonPath("$.data.developerUsername").value("gamedev"))
                .andExpect(jsonPath("$.data.developerEmail").value("dev@example.com"))
                .andExpect(jsonPath("$.data.totalGamesCreated").value(5))
                .andExpect(jsonPath("$.data.totalChanges").value(15))
                .andExpect(jsonPath("$.data.recentActivity").isArray())
                .andExpect(jsonPath("$.data.recentActivity.length()").value(1))
                .andExpect(jsonPath("$.data.firstActivity").exists())
                .andExpect(jsonPath("$.data.lastActivity").exists());

        verify(historyService).getDeveloperActivity(developerId);
        verify(responseMapper).toSuccessResponse("Developer activity retrieved", activityResponse);
    }

    @Test
    @DisplayName("Should retrieve developer history with pagination successfully")
    void shouldRetrieveDeveloperHistoryWithPaginationSuccessfully() throws Exception {
        // Given
        Long developerId = 1L;
        int page = 1;
        int size = 5;

        GameHistoryResponse createHistory = new GameHistoryResponse(
                1L, 1L, "Game 1", "CREATE", "Game Created",
                null, null, null, "gamedev", LocalDateTime.now(), "Game created"
        );

        GameHistoryResponse updateHistory = new GameHistoryResponse(
                2L, 1L, "Game 1", "UPDATE", "Game Updated",
                "price", "29.99", "39.99", "gamedev", LocalDateTime.now(), "Price updated"
        );

        List<GameHistoryResponse> historyList = List.of(updateHistory, createHistory);
        Page<GameHistoryResponse> historyPage = new PageImpl<>(historyList, PageRequest.of(page, size), 10);
        ApiResponse<Page<GameHistoryResponse>> apiResponse = ApiResponse.success("Developer history retrieved", historyPage);

        when(historyService.getDeveloperHistory(developerId, page, size)).thenReturn(historyPage);
        when(responseMapper.toSuccessResponse("Developer history retrieved", historyPage))
                .thenReturn(apiResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/history/developer/{developerId}/history", developerId)
                        .param("page", String.valueOf(page))
                        .param("size", String.valueOf(size))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Developer history retrieved"))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.content[0].action_type").value("UPDATE"))
                .andExpect(jsonPath("$.data.content[1].action_type").value("CREATE"))
                .andExpect(jsonPath("$.data.number").value(page))
                .andExpect(jsonPath("$.data.size").value(size))
                .andExpect(jsonPath("$.data.totalElements").value(10))
                .andExpect(jsonPath("$.data.totalPages").value(2));

        verify(historyService).getDeveloperHistory(developerId, page, size);
        verify(responseMapper).toSuccessResponse("Developer history retrieved", historyPage);
    }

    @Test
    @DisplayName("Should retrieve current user activity successfully")
    void shouldRetrieveCurrentUserActivitySuccessfully() throws Exception {
        // Given
        DeveloperActivityResponse activityResponse = new DeveloperActivityResponse(
                1L,
                "currentuser",
                "current@example.com",
                3L,
                10L,
                List.of(),
                LocalDateTime.now().minusDays(15),
                LocalDateTime.now()
        );

        ApiResponse<DeveloperActivityResponse> apiResponse = ApiResponse.success("My developer activity retrieved", activityResponse);

        when(historyService.getMyDeveloperActivity()).thenReturn(activityResponse);
        when(responseMapper.toSuccessResponse("My developer activity retrieved", activityResponse))
                .thenReturn(apiResponse);


        // When & Then
        mockMvc.perform(get("/api/v1/history/my/activity")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("My developer activity retrieved"))
                .andExpect(jsonPath("$.data.developerId").value(1))
                .andExpect(jsonPath("$.data.developerUsername").value("currentuser"))
                .andExpect(jsonPath("$.data.totalGamesCreated").value(3))
                .andExpect(jsonPath("$.data.totalChanges").value(10));

        verify(historyService).getMyDeveloperActivity();
        verify(responseMapper).toSuccessResponse("My developer activity retrieved", activityResponse);
    }

    @Test
    @DisplayName("Should handle various pagination scenarios and edge cases")
    void shouldHandleVariousPaginationScenariosAndEdgeCases() throws Exception {
        // Test 1: Default pagination parameters
        Long gameId = 1L;
        Page<GameHistoryResponse> defaultPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
        ApiResponse<Page<GameHistoryResponse>> defaultResponse = ApiResponse.success("Game history retrieved", defaultPage);

        when(historyService.getGameHistory(gameId, 0, 10)).thenReturn(defaultPage);
        when(responseMapper.toSuccessResponse("Game history retrieved", defaultPage))
                .thenReturn(defaultResponse);

        mockMvc.perform(get("/api/v1/history/game/{gameId}", gameId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(0))
                .andExpect(jsonPath("$.data.number").value(0))
                .andExpect(jsonPath("$.data.size").value(10));

        // Test 2: Custom pagination parameters
        reset(historyService, responseMapper);

        Page<GameHistoryResponse> customPage = new PageImpl<>(List.of(), PageRequest.of(2, 5), 25);
        ApiResponse<Page<GameHistoryResponse>> customResponse = ApiResponse.success("Game history retrieved", customPage);

        when(historyService.getGameHistory(gameId, 2, 5)).thenReturn(customPage);
        when(responseMapper.toSuccessResponse("Game history retrieved", customPage))
                .thenReturn(customResponse);

        mockMvc.perform(get("/api/v1/history/game/{gameId}", gameId)
                        .param("page", "2")
                        .param("size", "5")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.number").value(2))
                .andExpect(jsonPath("$.data.size").value(5))
                .andExpect(jsonPath("$.data.totalElements").value(25))
                .andExpect(jsonPath("$.data.totalPages").value(5));

        verify(historyService).getGameHistory(gameId, 2, 5);
    }

    @Test
    @DisplayName("Should handle different action types in history responses")
    void shouldHandleDifferentActionTypesInHistoryResponses() throws Exception {
        // Given
        Long gameId = 1L;
        List<GameHistoryResponse> mixedHistory = List.of(
                new GameHistoryResponse(1L, 1L, "Test Game", "CREATE", "Game Created",
                        null, null, null, "gamedev", LocalDateTime.now(), "Game created"),
                new GameHistoryResponse(2L, 1L, "Test Game", "UPDATE", "Game Updated",
                        "title", "Old Title", "New Title", "gamedev", LocalDateTime.now(), "Title updated"),
                new GameHistoryResponse(3L, 1L, "Test Game", "PURCHASE", "Game Purchased",
                        null, null, "29.99", "purchaser", LocalDateTime.now(), "Game purchased"),
                new GameHistoryResponse(4L, 1L, "Test Game", "DELETE", "Game Deleted",
                        null, null, null, "gamedev", LocalDateTime.now(), "Game deleted")
        );

        Page<GameHistoryResponse> historyPage = new PageImpl<>(mixedHistory, PageRequest.of(0, 10), 4);
        ApiResponse<Page<GameHistoryResponse>> apiResponse = ApiResponse.success("Game history retrieved", historyPage);

        when(historyService.getGameHistory(gameId, 0, 10)).thenReturn(historyPage);
        when(responseMapper.toSuccessResponse("Game history retrieved", historyPage))
                .thenReturn(apiResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/history/game/{gameId}", gameId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(4))
                .andExpect(jsonPath("$.data.content[0].action_type").value("CREATE"))
                .andExpect(jsonPath("$.data.content[1].action_type").value("UPDATE"))
                .andExpect(jsonPath("$.data.content[1].field_changed").value("title"))
                .andExpect(jsonPath("$.data.content[1].old_value").value("Old Title"))
                .andExpect(jsonPath("$.data.content[1].new_value").value("New Title"))
                .andExpect(jsonPath("$.data.content[2].action_type").value("PURCHASE"))
                .andExpect(jsonPath("$.data.content[2].new_value").value("29.99"))
                .andExpect(jsonPath("$.data.content[2].changed_by").value("purchaser"))
                .andExpect(jsonPath("$.data.content[3].action_type").value("DELETE"));

        verify(historyService).getGameHistory(gameId, 0, 10);
    }

    @Test
    @DisplayName("Should handle service layer exceptions appropriately")
    void shouldHandleServiceLayerExceptionsAppropriately() throws Exception {
        // Test 1: Game not found
        Long nonExistentGameId = 999L;
        when(historyService.getGameHistory(nonExistentGameId, 0, 10))
                .thenThrow(new IllegalArgumentException("Game not found"));

        mockMvc.perform(get("/api/v1/history/game/{gameId}", nonExistentGameId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is5xxServerError());

        // Test 2: Developer not found
        Long nonExistentDeveloperId = 999L;
        when(historyService.getDeveloperActivity(nonExistentDeveloperId))
                .thenThrow(new IllegalArgumentException("Developer not found"));

        mockMvc.perform(get("/api/v1/history/developer/{developerId}", nonExistentDeveloperId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is5xxServerError());

        verify(historyService).getGameHistory(nonExistentGameId, 0, 10);
        verify(historyService).getDeveloperActivity(nonExistentDeveloperId);
        verify(responseMapper, never()).toSuccessResponse(anyString(), any());
    }

    @Test
    @DisplayName("Should retrieve my game history successfully")
    void shouldRetrieveMyGameHistorySuccessfully() throws Exception {
        // Given
        Long gameId = 1L;
        GameHistoryResponse historyResponse = new GameHistoryResponse(
                1L,
                gameId,
                "My Game",
                "UPDATE",
                "Game Updated",
                "price",
                "29.99",
                "39.99",
                "devUser",
                LocalDateTime.now(),
                "Price updated"
        );

        Page<GameHistoryResponse> historyPage = new PageImpl<>(List.of(historyResponse), PageRequest.of(0, 10), 1);
        ApiResponse<Page<GameHistoryResponse>> apiResponse =
                ApiResponse.success("My game history retrieved", historyPage);

        when(historyService.getMyGameHistory(gameId, 0, 10)).thenReturn(historyPage);
        when(responseMapper.toSuccessResponse("My game history retrieved", historyPage))
                .thenReturn(apiResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/history/my/game/{gameId}", gameId)
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("My game history retrieved"))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].id").value(1))
                .andExpect(jsonPath("$.data.content[0].game_id").value(1))
                .andExpect(jsonPath("$.data.content[0].action_type").value("UPDATE"))
                .andExpect(jsonPath("$.data.content[0].field_changed").value("price"))
                .andExpect(jsonPath("$.data.content[0].old_value").value("29.99"))
                .andExpect(jsonPath("$.data.content[0].new_value").value("39.99"));

        verify(historyService).getMyGameHistory(gameId, 0, 10);
        verify(responseMapper).toSuccessResponse("My game history retrieved", historyPage);
    }

    @Test
    @DisplayName("Should retrieve my developer history successfully")
    void shouldRetrieveMyDeveloperHistorySuccessfully() throws Exception {
        // Given
        int page = 0;
        int size = 5;

        GameHistoryResponse historyResponse = new GameHistoryResponse(
                1L,
                1L,
                "Game Title",
                "CREATE",
                "Game Created",
                null,
                null,
                null,
                "devUser",
                LocalDateTime.now(),
                "Game created"
        );

        Page<GameHistoryResponse> historyPage =
                new PageImpl<>(List.of(historyResponse), PageRequest.of(page, size), 1);
        ApiResponse<Page<GameHistoryResponse>> apiResponse =
                ApiResponse.success("My developer history retrieved", historyPage);

        when(historyService.getMyDeveloperHistory(page, size)).thenReturn(historyPage);
        when(responseMapper.toSuccessResponse("My developer history retrieved", historyPage))
                .thenReturn(apiResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/history/my/history")
                        .param("page", String.valueOf(page))
                        .param("size", String.valueOf(size))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("My developer history retrieved"))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].id").value(1))
                .andExpect(jsonPath("$.data.content[0].game_title").value("Game Title"))
                .andExpect(jsonPath("$.data.content[0].action_type").value("CREATE"))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.totalPages").value(1));

        verify(historyService).getMyDeveloperHistory(page, size);
        verify(responseMapper).toSuccessResponse("My developer history retrieved", historyPage);
    }
}