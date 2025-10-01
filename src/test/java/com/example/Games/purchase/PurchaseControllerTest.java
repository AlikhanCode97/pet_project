package com.example.Games.purchase;

import com.example.Games.config.common.dto.ApiResponse;
import com.example.Games.config.common.mappers.ResponseMapStruct;
import com.example.Games.config.exception.game.GameNotFoundException;
import com.example.Games.config.exception.purchase.GameAlreadyOwnedException;
import com.example.Games.config.exception.purchase.PurchaseException;
import com.example.Games.config.test.WebMvcTestWithoutSecurity;
import com.example.Games.config.exception.purchase.PurchaseExceptionHandler;
import org.springframework.context.annotation.Import;
import com.example.Games.purchase.dto.PurchaseGamesRequest;
import com.example.Games.purchase.dto.PurchaseResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTestWithoutSecurity(PurchaseController.class)
@Import(PurchaseExceptionHandler.class)
@DisplayName("PurchaseController Tests")
class PurchaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PurchaseService purchaseService;

    @MockitoBean
    private ResponseMapStruct responseMapper;

    private PurchaseResponse purchase1;
    private PurchaseResponse purchase2;

    @BeforeEach
    void setUp() {
        purchase1 = PurchaseResponse.builder()
                .purchaseId(1L)
                .gameId(1L)
                .gameTitle("Test Game")
                .gameAuthor("developer")
                .purchasePrice(new BigDecimal("29.99"))
                .currentGamePrice(new BigDecimal("29.99"))
                .purchasedAt(LocalDateTime.now())
                .priceDifference(BigDecimal.ZERO)
                .build();

        purchase2 = PurchaseResponse.builder()
                .purchaseId(2L)
                .gameId(2L)
                .gameTitle("Test Game 2")
                .gameAuthor("developer")
                .purchasePrice(new BigDecimal("39.99"))
                .currentGamePrice(new BigDecimal("39.99"))
                .purchasedAt(LocalDateTime.now())
                .priceDifference(BigDecimal.ZERO)
                .build();
    }

    @Test
    @DisplayName("Should purchase single game successfully")
    void shouldPurchaseSingleGameSuccessfully() throws Exception {
        when(purchaseService.purchaseGame(1L)).thenReturn(purchase1);
        when(responseMapper.toSuccessResponse("Game purchased successfully", purchase1))
                .thenReturn(ApiResponse.success("Game purchased successfully", purchase1));

        mockMvc.perform(post("/api/v1/purchase/game/{gameId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Game purchased successfully"))
                .andExpect(jsonPath("$.data.purchaseId").value(1))
                .andExpect(jsonPath("$.data.gameTitle").value("Test Game"));

        verify(purchaseService).purchaseGame(1L);
    }

    @Test
    @DisplayName("Should return 404 when game not found")
    void shouldReturn404WhenGameNotFound() throws Exception {
        when(purchaseService.purchaseGame(999L))
                .thenThrow(new GameNotFoundException("Game not found"));
        when(responseMapper.toErrorResponse(anyString()))
                .thenReturn(ApiResponse.error("Game not found"));

        mockMvc.perform(post("/api/v1/purchase/game/{gameId}", 999L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Game not found"));
    }

    @Test
    @DisplayName("Should return 409 when game already owned")
    void shouldReturn409WhenGameAlreadyOwned() throws Exception {
        when(purchaseService.purchaseGame(1L))
                .thenThrow(new GameAlreadyOwnedException("You already own this game"));
        when(responseMapper.toErrorResponse(anyString()))
                .thenReturn(ApiResponse.error("You already own this game"));

        mockMvc.perform(post("/api/v1/purchase/game/{gameId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("You already own this game"));
    }

    @Test
    @DisplayName("Should return 400 when user tries to purchase own game")
    void shouldReturn400WhenSelfPurchase() throws Exception {
        when(purchaseService.purchaseGame(3L))
                .thenThrow(PurchaseException.selfPurchase("My Game"));
        when(responseMapper.toErrorResponse(anyString()))
                .thenReturn(ApiResponse.error("My Game"));

        mockMvc.perform(post("/api/v1/purchase/game/{gameId}", 3L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("My Game"));
    }

    @Test
    @DisplayName("Should return 500 when service throws unexpected exception")
    void shouldReturn500OnUnexpectedException() throws Exception {
        when(purchaseService.purchaseGame(1L))
                .thenThrow(new RuntimeException("Insufficient funds"));
        when(responseMapper.toErrorResponse(anyString()))
                .thenReturn(ApiResponse.error("Insufficient funds"));

        mockMvc.perform(post("/api/v1/purchase/game/{gameId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Insufficient funds"));
    }

    @Test
    @DisplayName("Should return 400 when gameIds list is null")
    void shouldReturn400WhenGameIdsIsNull() throws Exception {
        String jsonWithNull = "{\"gameIds\": null}";

        mockMvc.perform(post("/api/v1/purchase/games")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonWithNull))
                .andExpect(status().isBadRequest());

        verify(purchaseService, never()).purchaseGamesByIds(any());
    }

    @Test
    @DisplayName("Should return 400 when gameIds list is empty")
    void shouldReturn400WhenGameIdsIsEmpty() throws Exception {
        PurchaseGamesRequest request = new PurchaseGamesRequest(Collections.emptyList());

        mockMvc.perform(post("/api/v1/purchase/games")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(purchaseService, never()).purchaseGamesByIds(any());
    }


    @Test
    @DisplayName("Should purchase multiple games successfully")
    void shouldPurchaseMultipleGamesSuccessfully() throws Exception {
        List<Long> ids = Arrays.asList(1L, 2L);
        PurchaseGamesRequest request = new PurchaseGamesRequest(ids);
        List<PurchaseResponse> responses = Arrays.asList(purchase1, purchase2);

        when(purchaseService.purchaseGamesByIds(request)).thenReturn(responses);
        when(responseMapper.toSuccessResponse("2 games purchased successfully", responses))
                .thenReturn(ApiResponse.success("2 games purchased successfully", responses));

        mockMvc.perform(post("/api/v1/purchase/games")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("2 games purchased successfully"))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].gameTitle").value("Test Game"))
                .andExpect(jsonPath("$.data[1].gameTitle").value("Test Game 2"));

        verify(purchaseService).purchaseGamesByIds(request);
    }

    @Test
    @DisplayName("Should get my purchase history")
    void shouldGetMyPurchaseHistory() throws Exception {
        List<PurchaseResponse> history = Collections.singletonList(purchase1);

        when(purchaseService.getMyPurchaseHistory()).thenReturn(history);
        when(responseMapper.toSuccessResponse("Purchase history retrieved", history))
                .thenReturn(ApiResponse.success("Purchase history retrieved", history));

        mockMvc.perform(get("/api/v1/purchase/history")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Purchase history retrieved"))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].purchaseId").value(1));
    }

    @Test
    @DisplayName("Should get paginated purchase history")
    void shouldGetPaginatedPurchaseHistory() throws Exception {
        var page = new org.springframework.data.domain.PageImpl<>(List.of(purchase1, purchase2));
        when(purchaseService.getMyPurchaseHistoryPaged(any()))
                .thenReturn(page);
        when(responseMapper.toSuccessResponse("Paged purchase history retrieved", page))
                .thenReturn(ApiResponse.success("Paged purchase history retrieved", page));

        mockMvc.perform(get("/api/v1/purchase/history/paged")
                        .param("page", "0")
                        .param("size", "2")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Paged purchase history retrieved"))
                .andExpect(jsonPath("$.data.content", hasSize(2)))
                .andExpect(jsonPath("$.data.totalElements").value(2));
    }

    @Test
    @DisplayName("Should get user purchase history as admin")
    void shouldGetUserPurchaseHistoryAsAdmin() throws Exception {
        List<PurchaseResponse> history = List.of(purchase1);
        when(purchaseService.getUserPurchaseHistory(5L)).thenReturn(history);
        when(responseMapper.toSuccessResponse("User purchase history retrieved", history))
                .thenReturn(ApiResponse.success("User purchase history retrieved", history));

        mockMvc.perform(get("/api/v1/purchase/admin/user/{userId}/history", 5L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)));
    }

    @Test
    @DisplayName("Should get game purchases as admin")
    void shouldGetGamePurchasesAsAdmin() throws Exception {
        List<PurchaseResponse> purchases = List.of(purchase1, purchase2);
        when(purchaseService.getGamePurchases(1L)).thenReturn(purchases);
        when(responseMapper.toSuccessResponse("Game purchase history retrieved", purchases))
                .thenReturn(ApiResponse.success("Game purchase history retrieved", purchases));

        mockMvc.perform(get("/api/v1/purchase/admin/game/{gameId}/purchases", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)));
    }

    @Test
    @DisplayName("Should get developer sales")
    void shouldGetDeveloperSales() throws Exception {
        List<PurchaseResponse> sales = List.of(purchase1, purchase2);
        when(purchaseService.getMySales()).thenReturn(sales);
        when(responseMapper.toSuccessResponse("Developer sales retrieved", sales))
                .thenReturn(ApiResponse.success("Developer sales retrieved", sales));

        mockMvc.perform(get("/api/v1/purchase/developer/sales"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)));
    }

    @Test
    @DisplayName("Should get developer revenue")
    void shouldGetDeveloperRevenue() throws Exception {
        BigDecimal revenue = new BigDecimal("1234.56");
        when(purchaseService.getMyTotalRevenue()).thenReturn(revenue);
        when(responseMapper.toSuccessResponse("Total revenue retrieved", revenue))
                .thenReturn(ApiResponse.success("Total revenue retrieved", revenue));

        mockMvc.perform(get("/api/v1/purchase/developer/revenue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(1234.56));
    }
}
