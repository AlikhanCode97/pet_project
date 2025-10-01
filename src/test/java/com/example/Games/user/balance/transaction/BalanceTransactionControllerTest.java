package com.example.Games.user.balance.transaction;

import com.example.Games.config.common.dto.ApiResponse;
import com.example.Games.config.common.mappers.ResponseMapStruct;
import com.example.Games.config.exception.auth.UserNotFoundException;
import com.example.Games.config.exception.balance.BalanceNotFoundException;
import com.example.Games.config.security.JwtAuthenticationFilter;
import com.example.Games.config.security.SecurityConfig;
import com.example.Games.config.security.TokenBlacklistService;
import com.example.Games.user.balance.transaction.dto.BalanceTransactionDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = BalanceTransactionController.class, excludeFilters = {
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {SecurityConfig.class,
                JwtAuthenticationFilter.class, TokenBlacklistService.class})
})
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("BalanceTransactionController Tests")
class BalanceTransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private BalanceTransactionService transactionService;

    @MockitoBean
    private ResponseMapStruct responseMapper;

    private BalanceTransactionDTO depositTransaction;
    private BalanceTransactionDTO withdrawalTransaction;
    private BalanceTransactionDTO purchaseTransaction;
    private List<BalanceTransactionDTO> sampleTransactions;

    @BeforeEach
    void setUp() {
        LocalDateTime now = LocalDateTime.now();
        
        depositTransaction = new BalanceTransactionDTO(
                1L,
                OperationType.DEPOSIT,
                new BigDecimal("100.00"),
                new BigDecimal("0.00"),
                new BigDecimal("100.00"),
                now.minusHours(3)
        );
        
        withdrawalTransaction = new BalanceTransactionDTO(
                2L,
                OperationType.WITHDRAWAL,
                new BigDecimal("30.00"),
                new BigDecimal("100.00"),
                new BigDecimal("70.00"),
                now.minusHours(2)
        );
        
        purchaseTransaction = new BalanceTransactionDTO(
                3L,
                OperationType.PURCHASE,
                new BigDecimal("19.99"),
                new BigDecimal("70.00"),
                new BigDecimal("50.01"),
                now.minusHours(1)
        );

        BalanceTransactionDTO adminDepositTransaction = new BalanceTransactionDTO(
                4L,
                OperationType.ADMIN_DEPOSIT,
                new BigDecimal("500.00"),
                new BigDecimal("50.01"),
                new BigDecimal("550.01"),
                now
        );
        
        // Order: newest first
        sampleTransactions = Arrays.asList(
                adminDepositTransaction,
                purchaseTransaction,
                withdrawalTransaction,
                depositTransaction
        );
    }

    @Test
    @DisplayName("Should get my transactions successfully")
    void shouldGetMyTransactionsSuccessfully() throws Exception {
        // Given
        when(transactionService.getMyTransactions()).thenReturn(sampleTransactions);
        when(responseMapper.toSuccessResponse(eq("My transaction history retrieved"), eq(sampleTransactions)))
                .thenReturn(new ApiResponse<>("My transaction history retrieved", sampleTransactions, System.currentTimeMillis()));

        // When & Then
        mockMvc.perform(get("/api/v1/balance/transactions/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("My transaction history retrieved"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(4))
                .andExpect(jsonPath("$.data[0].type").value("ADMIN_DEPOSIT"))
                .andExpect(jsonPath("$.data[0].amount").value(500.00))
                .andExpect(jsonPath("$.data[1].type").value("PURCHASE"))
                .andExpect(jsonPath("$.data[1].amount").value(19.99))
                .andExpect(jsonPath("$.data[2].type").value("WITHDRAWAL"))
                .andExpect(jsonPath("$.data[3].type").value("DEPOSIT"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(transactionService).getMyTransactions();
        verify(responseMapper).toSuccessResponse("My transaction history retrieved", sampleTransactions);
    }

    @Test
    @DisplayName("Should return empty list when no transactions exist")
    void shouldReturnEmptyListWhenNoTransactionsExist() throws Exception {
        // Given
        List<BalanceTransactionDTO> emptyList = Collections.emptyList();
        when(transactionService.getMyTransactions()).thenReturn(emptyList);
        when(responseMapper.toSuccessResponse(eq("My transaction history retrieved"), eq(emptyList)))
                .thenReturn(new ApiResponse<>("My transaction history retrieved", emptyList, System.currentTimeMillis()));

        // When & Then
        mockMvc.perform(get("/api/v1/balance/transactions/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("My transaction history retrieved"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));

        verify(transactionService).getMyTransactions();
        verify(responseMapper).toSuccessResponse("My transaction history retrieved", emptyList);
    }

    @Test
    @DisplayName("Should return 404 when balance not found for current user")
    void shouldReturn404WhenBalanceNotFoundForCurrentUser() throws Exception {
        // Given
        when(transactionService.getMyTransactions())
                .thenThrow(new BalanceNotFoundException("testuser"));
        when(responseMapper.toErrorResponse(anyString()))
                .thenReturn(new ApiResponse<>("Balance not found for user: testuser", null, System.currentTimeMillis()));

        // When & Then
        mockMvc.perform(get("/api/v1/balance/transactions/me"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Balance not found for user: testuser"))
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(transactionService).getMyTransactions();
        verify(responseMapper).toErrorResponse(anyString());

    }

    @Test
    @DisplayName("Should get user transactions by ID for admin")
    void shouldGetUserTransactionsByIdForAdmin() throws Exception {
        // Given
        Long userId = 2L;
        List<BalanceTransactionDTO> userTransactions = Arrays.asList(depositTransaction, withdrawalTransaction);
        
        when(transactionService.getUserTransactions(userId)).thenReturn(userTransactions);
        when(responseMapper.toSuccessResponse(eq("User transaction history retrieved"), eq(userTransactions)))
                .thenReturn(new ApiResponse<>("User transaction history retrieved", userTransactions, System.currentTimeMillis()));

        // When & Then
        mockMvc.perform(get("/api/v1/balance/transactions/user/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User transaction history retrieved"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].type").value("DEPOSIT"))
                .andExpect(jsonPath("$.data[1].type").value("WITHDRAWAL"));

        verify(transactionService).getUserTransactions(userId);
        verify(responseMapper).toSuccessResponse("User transaction history retrieved", userTransactions);
    }

    @Test
    @DisplayName("Should return empty list when user has no transactions")
    void shouldReturnEmptyListWhenUserHasNoTransactions() throws Exception {
        Long userId = 2L;
        when(transactionService.getUserTransactions(userId)).thenReturn(Collections.emptyList());
        when(responseMapper.toSuccessResponse(eq("User transaction history retrieved"), eq(Collections.emptyList())))
                .thenReturn(new ApiResponse<>("User transaction history retrieved", Collections.emptyList(), System.currentTimeMillis()));

        mockMvc.perform(get("/api/v1/balance/transactions/user/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User transaction history retrieved"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));
    }


    @Test
    @DisplayName("Should return 404 when user not found for admin transaction check")
    void shouldReturn404WhenUserNotFoundForAdminCheck() throws Exception {
        // Given
        Long userId = 999L;
        when(transactionService.getUserTransactions(userId))
                .thenThrow(new UserNotFoundException(userId));
        when(responseMapper.toErrorResponse(anyString()))
                .thenReturn(new ApiResponse<>("User not found with ID: 999", null, System.currentTimeMillis()));

        // When & Then
        mockMvc.perform(get("/api/v1/balance/transactions/user/{userId}", userId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found with ID: 999"));

        verify(transactionService).getUserTransactions(userId);
        verify(responseMapper).toErrorResponse(anyString());

    }

    @Test
    @DisplayName("Should return 404 when balance not found for user")
    void shouldReturn404WhenBalanceNotFoundForUser() throws Exception {
        // Given
        Long userId = 2L;
        when(transactionService.getUserTransactions(userId))
                .thenThrow(new BalanceNotFoundException("otheruser"));
        when(responseMapper.toErrorResponse(anyString()))
                .thenReturn(new ApiResponse<>("Balance not found for user: otheruser", null, System.currentTimeMillis()));

        // When & Then
        mockMvc.perform(get("/api/v1/balance/transactions/user/{userId}", userId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Balance not found for user: otheruser"));

        verify(transactionService).getUserTransactions(userId);
        verify(responseMapper).toErrorResponse(anyString());

    }
}
