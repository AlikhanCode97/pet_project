package com.example.Games.user.balance;

import com.example.Games.config.common.dto.ApiResponse;
import com.example.Games.config.common.mappers.ResponseMapStruct;
import com.example.Games.config.exception.auth.UserNotFoundException;
import com.example.Games.config.exception.balance.BalanceAlreadyExistsException;
import com.example.Games.config.exception.balance.BalanceNotFoundException;
import com.example.Games.config.exception.balance.InsufficientFundsException;
import com.example.Games.config.exception.balance.InvalidAmountException;
import com.example.Games.config.security.JwtAuthenticationFilter;
import com.example.Games.config.security.SecurityConfig;
import com.example.Games.config.security.TokenBlacklistService;
import com.example.Games.user.balance.dto.*;
import com.example.Games.user.balance.transaction.OperationType;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = BalanceController.class, excludeFilters = {
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {SecurityConfig.class,
                JwtAuthenticationFilter.class, TokenBlacklistService.class})
})
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("BalanceController Tests")
class BalanceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private BalanceService balanceService;

    @MockitoBean
    private ResponseMapStruct responseMapper;

    private BalanceResponse balanceResponse;
    private DepositRequest depositRequest;
    private WithdrawRequest withdrawRequest;
    private BalanceOperationResponse depositOperationResponse;
    private BalanceOperationResponse withdrawOperationResponse;

    @BeforeEach
    void setUp() {
        balanceResponse = new BalanceResponse(1L, new BigDecimal("100.00"));
        depositRequest = new DepositRequest(new BigDecimal("50.00"));
        withdrawRequest = new WithdrawRequest(new BigDecimal("25.00"));
        
        depositOperationResponse = new BalanceOperationResponse(
                new BigDecimal("150.00"),
                1L,
                new BigDecimal("50.00"),
                OperationType.DEPOSIT
        );
        
        withdrawOperationResponse = new BalanceOperationResponse(
                new BigDecimal("75.00"),
                1L,
                new BigDecimal("25.00"),
                OperationType.WITHDRAWAL
        );
    }

    @Test
    @DisplayName("Should get my balance successfully")
    void shouldGetMyBalanceSuccessfully() throws Exception {
        // Given
        when(balanceService.getMyBalance()).thenReturn(balanceResponse);
        when(responseMapper.toSuccessResponse(eq("Balance retrieved"), eq(balanceResponse)))
                .thenReturn(new ApiResponse<>("Balance retrieved", balanceResponse, System.currentTimeMillis()));

        // When & Then
        mockMvc.perform(get("/api/v1/balance/my"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Balance retrieved"))
                .andExpect(jsonPath("$.data.userId").value(1))
                .andExpect(jsonPath("$.data.balance").value(100.00))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(balanceService).getMyBalance();
        verify(responseMapper).toSuccessResponse("Balance retrieved", balanceResponse);
    }

    @Test
    @DisplayName("Should return 404 when balance not found")
    void shouldReturn404WhenMyBalanceNotFound() throws Exception {
        // Given
        when(balanceService.getMyBalance())
                .thenThrow(new BalanceNotFoundException("testuser"));
        when(responseMapper.toErrorResponse(anyString()))
                .thenReturn(new ApiResponse<>("Balance not found for user: testuser", null, System.currentTimeMillis()));

        // When & Then
        mockMvc.perform(get("/api/v1/balance/my"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Balance not found for user: testuser"))
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(balanceService).getMyBalance();
    }

    @Test
    @DisplayName("Should create balance successfully")
    void shouldCreateBalanceSuccessfully() throws Exception {
        // Given
        BalanceResponse newBalanceResponse = new BalanceResponse(1L, BigDecimal.ZERO);
        when(balanceService.createBalance()).thenReturn(newBalanceResponse);
        when(responseMapper.toSuccessResponse(eq("Balance created successfully"), eq(newBalanceResponse)))
                .thenReturn(new ApiResponse<>("Balance created successfully", newBalanceResponse, System.currentTimeMillis()));

        // When & Then
        mockMvc.perform(post("/api/v1/balance/create")
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Balance created successfully"))
                .andExpect(jsonPath("$.data.userId").value(1))
                .andExpect(jsonPath("$.data.balance").value(0));

        verify(balanceService).createBalance();
        verify(responseMapper).toSuccessResponse("Balance created successfully", newBalanceResponse);
    }

    @Test
    @DisplayName("Should return 409 when balance already exists")
    void shouldReturn409WhenBalanceAlreadyExists() throws Exception {
        // Given
        when(balanceService.createBalance())
                .thenThrow(new BalanceAlreadyExistsException("testuser"));
        when(responseMapper.toErrorResponse(anyString()))
                .thenReturn(new ApiResponse<>("Balance already exists for user: testuser", null, System.currentTimeMillis()));

        // When & Then
        mockMvc.perform(post("/api/v1/balance/create")
                        .with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Balance already exists for user: testuser"));

        verify(balanceService).createBalance();
    }

    @Test
    @DisplayName("Should delete balance successfully")
    void shouldDeleteBalanceSuccessfully() throws Exception {
        // Given
        doNothing().when(balanceService).deleteBalance();
        when(responseMapper.toSuccessResponse(eq("Balance deleted successfully")))
                .thenReturn(new ApiResponse<>("Balance deleted successfully", null, System.currentTimeMillis()));

        // When & Then
        mockMvc.perform(delete("/api/v1/balance/delete")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Balance deleted successfully"))
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(balanceService).deleteBalance();
        verify(responseMapper).toSuccessResponse("Balance deleted successfully");
    }

    @Test
    @DisplayName("Should return 404 when deleting non-existent balance")
    void shouldReturn404WhenDeletingNonExistentBalance() throws Exception {
        // Given
        doThrow(new BalanceNotFoundException("testuser"))
                .when(balanceService).deleteBalance();
        when(responseMapper.toErrorResponse(anyString()))
                .thenReturn(new ApiResponse<>("Balance not found for user: testuser", null, System.currentTimeMillis()));

        // When & Then
        mockMvc.perform(delete("/api/v1/balance/delete")
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Balance not found for user: testuser"));

        verify(balanceService).deleteBalance();
    }

    @Test
    @DisplayName("Should deposit successfully")
    void shouldDepositSuccessfully() throws Exception {
        // Given
        when(balanceService.deposit(any(DepositRequest.class))).thenReturn(depositOperationResponse);
        when(responseMapper.toSuccessResponse(eq("Deposit completed successfully"), eq(depositOperationResponse)))
                .thenReturn(new ApiResponse<>("Deposit completed successfully", depositOperationResponse, System.currentTimeMillis()));

        // When & Then
        mockMvc.perform(post("/api/v1/balance/deposit")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(depositRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Deposit completed successfully"))
                .andExpect(jsonPath("$.data.balance").value(150.00))
                .andExpect(jsonPath("$.data.amount").value(50.00))
                .andExpect(jsonPath("$.data.operation").value("DEPOSIT"));

        verify(balanceService).deposit(any(DepositRequest.class));
        verify(responseMapper).toSuccessResponse(eq("Deposit completed successfully"), eq(depositOperationResponse));
    }

    @Test
    @DisplayName("Should return 400 when deposit amount is invalid")
    void shouldReturn400WhenDepositAmountIsSmall() throws Exception {
        // Given - Negative amount
        DepositRequest invalidRequest = new DepositRequest(new BigDecimal("0.00"));
        String json = "{\"amount\": 0.00}";

        // When & Then
        mockMvc.perform(post("/api/v1/balance/deposit")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());

        verify(balanceService, never()).deposit(any());
    }

    @Test
    @DisplayName("Should return 400 when deposit amount is null")
    void shouldReturn400WhenDepositAmountIsNull() throws Exception {
        // Given
        String jsonWithNull = "{\"amount\": null}";

        // When & Then
        mockMvc.perform(post("/api/v1/balance/deposit")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonWithNull))
                .andExpect(status().isBadRequest());

        verify(balanceService, never()).deposit(any());
    }

    @Test
    @DisplayName("Should return 400 when deposit amount is too large")
    void shouldReturn400WhenDepositAmountIsTooLarge() throws Exception {
        String json = "{\"amount\": 1000000.00}";

        // When & Then
        mockMvc.perform(post("/api/v1/balance/deposit")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());

        verify(balanceService, never()).deposit(any());
    }

    @Test
    @DisplayName("Should return 400 when deposit amount has too many decimal places")
    void shouldReturn400WhenDepositAmountHasTooManyDecimalPlaces() throws Exception {
        String json = "{\"amount\": 50.123}";

        // When & Then
        mockMvc.perform(post("/api/v1/balance/deposit")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());

        verify(balanceService, never()).deposit(any());
    }

    @Test
    @DisplayName("Should return 404 when deposit to non-existent balance")
    void shouldReturn404WhenDepositToNonExistentBalance() throws Exception {
        // Given
        when(balanceService.deposit(any(DepositRequest.class)))
                .thenThrow(new BalanceNotFoundException("testuser"));
        when(responseMapper.toErrorResponse(anyString()))
                .thenReturn(new ApiResponse<>("Balance not found for user: testuser", null, System.currentTimeMillis()));

        // When & Then
        mockMvc.perform(post("/api/v1/balance/deposit")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(depositRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Balance not found for user: testuser"));

        verify(balanceService).deposit(any(DepositRequest.class));
    }

    @Test
    @DisplayName("Should withdraw successfully")
    void shouldWithdrawSuccessfully() throws Exception {
        // Given
        when(balanceService.withdraw(any(WithdrawRequest.class))).thenReturn(withdrawOperationResponse);
        when(responseMapper.toSuccessResponse(eq("Withdrawal completed successfully"), eq(withdrawOperationResponse)))
                .thenReturn(new ApiResponse<>("Withdrawal completed successfully", withdrawOperationResponse, System.currentTimeMillis()));

        // When & Then
        mockMvc.perform(post("/api/v1/balance/withdraw")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(withdrawRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Withdrawal completed successfully"))
                .andExpect(jsonPath("$.data.balance").value(75.00))
                .andExpect(jsonPath("$.data.amount").value(25.00))
                .andExpect(jsonPath("$.data.operation").value("WITHDRAWAL"));

        verify(balanceService).withdraw(any(WithdrawRequest.class));
        verify(responseMapper).toSuccessResponse(eq("Withdrawal completed successfully"), eq(withdrawOperationResponse));
    }

    @Test
    @DisplayName("Should return 402 when insufficient funds for withdrawal")
    void shouldReturn402WhenInsufficientFundsForWithdrawal() throws Exception {
        // Given
        when(balanceService.withdraw(any(WithdrawRequest.class)))
                .thenThrow(new InsufficientFundsException("Insufficient funds. Current: $100.00, Requested: $150.00"));
        when(responseMapper.toErrorResponse(anyString()))
                .thenReturn(new ApiResponse<>("Insufficient funds. Current: $100.00, Requested: $150.00", null, System.currentTimeMillis()));

        // When & Then
        mockMvc.perform(post("/api/v1/balance/withdraw")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(withdrawRequest)))
                .andExpect(status().isPaymentRequired())
                .andExpect(jsonPath("$.message").value("Insufficient funds. Current: $100.00, Requested: $150.00"));

        verify(balanceService).withdraw(any(WithdrawRequest.class));
    }

    @Test
    @DisplayName("Should return 404 when withdraw from non-existent balance")
    void shouldReturn404WhenWithdrawFromNonExistentBalance() throws Exception {
        // Given
        when(balanceService.withdraw(any(WithdrawRequest.class)))
                .thenThrow(new BalanceNotFoundException("testuser"));
        when(responseMapper.toErrorResponse(anyString()))
                .thenReturn(new ApiResponse<>("Balance not found for user: testuser", null, System.currentTimeMillis()));

        // When & Then
        mockMvc.perform(post("/api/v1/balance/withdraw")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(withdrawRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Balance not found for user: testuser"));

        verify(balanceService).withdraw(any(WithdrawRequest.class));
    }

    @Test
    @DisplayName("Should get user balance by ID with ADMIN role")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void shouldGetUserBalanceByIdWithAdminRole() throws Exception {
        // Given
        Long userId = 2L;
        BalanceResponse userBalanceResponse = new BalanceResponse(userId, new BigDecimal("150.00"));
        when(balanceService.getUserBalance(userId)).thenReturn(userBalanceResponse);
        when(responseMapper.toSuccessResponse(eq("User balance retrieved"), eq(userBalanceResponse)))
                .thenReturn(new ApiResponse<>("User balance retrieved", userBalanceResponse, System.currentTimeMillis()));

        // When & Then
        mockMvc.perform(get("/api/v1/balance/user/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User balance retrieved"))
                .andExpect(jsonPath("$.data.userId").value(userId))
                .andExpect(jsonPath("$.data.balance").value(150.00));

        verify(balanceService).getUserBalance(userId);
        verify(responseMapper).toSuccessResponse("User balance retrieved", userBalanceResponse);
    }

    @Test
    @DisplayName("Should return 404 when user not found for admin balance check")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void shouldReturn404WhenUserNotFoundForAdminBalanceCheck() throws Exception {
        // Given
        Long userId = 999L;
        when(balanceService.getUserBalance(userId))
                .thenThrow(new UserNotFoundException(userId));
        when(responseMapper.toErrorResponse(anyString()))
                .thenReturn(new ApiResponse<>("User not found with ID: 999", null, System.currentTimeMillis()));

        // When & Then
        mockMvc.perform(get("/api/v1/balance/user/{userId}", userId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found with ID: 999"));

        verify(balanceService).getUserBalance(userId);
    }

    @Test
    @DisplayName("Should return 404 when user has no balance for admin check")
    void shouldReturn404WhenUserHasNoBalanceForAdminCheck() throws Exception {
        // Given
        Long userId = 2L;
        when(balanceService.getUserBalance(userId))
                .thenThrow(new BalanceNotFoundException("otheruser"));
        when(responseMapper.toErrorResponse(anyString()))
                .thenReturn(new ApiResponse<>("Balance not found for user: otheruser", null, System.currentTimeMillis()));

        // When & Then
        mockMvc.perform(get("/api/v1/balance/user/{userId}", userId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Balance not found for user: otheruser"));

        verify(balanceService).getUserBalance(userId);
    }
}
