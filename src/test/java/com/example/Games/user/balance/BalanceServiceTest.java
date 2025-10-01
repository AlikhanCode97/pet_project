package com.example.Games.user.balance;

import com.example.Games.config.common.service.UserContextService;
import com.example.Games.config.exception.auth.UserNotFoundException;
import com.example.Games.config.exception.balance.BalanceAlreadyExistsException;
import com.example.Games.config.exception.balance.BalanceNotFoundException;
import com.example.Games.config.exception.balance.InsufficientFundsException;
import com.example.Games.config.exception.balance.InvalidAmountException;
import com.example.Games.user.auth.User;
import com.example.Games.user.balance.dto.*;
import com.example.Games.user.balance.transaction.BalanceTransaction;
import com.example.Games.user.balance.transaction.BalanceTransactionRepository;
import com.example.Games.user.balance.transaction.OperationType;
import com.example.Games.user.role.Role;
import com.example.Games.user.role.RoleType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BalanceService Tests")
class BalanceServiceTest {

    @Mock
    private BalanceRepository balanceRepository;

    @Mock
    private BalanceMapStruct balanceMapper;

    @Mock
    private UserContextService userContextService;

    @Mock
    private BalanceTransactionRepository transactionRepository;

    @InjectMocks
    private BalanceService balanceService;

    private User testUser;
    private Balance testBalance;
    private BalanceResponse balanceResponse;
    private BalanceOperationResponse operationResponse;

    @BeforeEach
    void setUp() {
        Role userRole = Role.builder()
                .id(1L)
                .name(RoleType.USER)
                .build();

        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .password("hashedPassword")
                .role(userRole)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        testBalance = Balance.builder()
                .id(1L)
                .user(testUser)
                .amount(new BigDecimal("100.00"))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        balanceResponse = new BalanceResponse(1L, new BigDecimal("100.00"));
        
        operationResponse = new BalanceOperationResponse(
                new BigDecimal("150.00"),
                1L,
                new BigDecimal("50.00"),
                OperationType.DEPOSIT
        );
    }

    @Test
    @DisplayName("Should create balance successfully")
    void shouldCreateBalanceSuccessfully() {
        // Given
        when(userContextService.getAuthorizedUser()).thenReturn(testUser);
        when(balanceRepository.findByUser(testUser)).thenReturn(Optional.empty());
        when(balanceRepository.save(any(Balance.class))).thenReturn(testBalance);
        when(balanceMapper.toBalanceResponse(eq(1L), any(BigDecimal.class)))
                .thenReturn(new BalanceResponse(1L, BigDecimal.ZERO));

        // When
        BalanceResponse result = balanceService.createBalance();

        // Then
        assertThat(result.userId()).isEqualTo(1L);
        assertThat(result.balance()).isEqualByComparingTo(BigDecimal.ZERO);
        
        ArgumentCaptor<Balance> balanceCaptor = ArgumentCaptor.forClass(Balance.class);
        verify(balanceRepository).save(balanceCaptor.capture());
        
        Balance savedBalance = balanceCaptor.getValue();
        assertThat(savedBalance.getUser()).isEqualTo(testUser);
        assertThat(savedBalance.getAmount()).isEqualByComparingTo(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
    }

    @Test
    @DisplayName("Should throw exception when balance already exists")
    void shouldThrowExceptionWhenBalanceAlreadyExists() {
        // Given
        when(userContextService.getAuthorizedUser()).thenReturn(testUser);
        when(balanceRepository.findByUser(testUser)).thenReturn(Optional.of(testBalance));

        // When & Then
        assertThatThrownBy(() -> balanceService.createBalance())
                .isInstanceOf(BalanceAlreadyExistsException.class)
                .hasMessageContaining("testuser");

        verify(balanceRepository, never()).save(any());
    }

    // GET MY BALANCE TESTS
    @Test
    @DisplayName("Should get my balance successfully")
    void shouldGetMyBalanceSuccessfully() {
        // Given
        when(userContextService.getAuthorizedUser()).thenReturn(testUser);
        when(balanceRepository.findByUser(testUser)).thenReturn(Optional.of(testBalance));
        when(balanceMapper.toBalanceResponse(1L, new BigDecimal("100.00"))).thenReturn(balanceResponse);

        // When
        BalanceResponse result = balanceService.getMyBalance();

        // Then
        assertThat(result).isEqualTo(balanceResponse);
        verify(userContextService).getAuthorizedUser();
        verify(balanceRepository).findByUser(testUser);
    }

    @Test
    @DisplayName("Should throw exception when balance not found for current user")
    void shouldThrowExceptionWhenBalanceNotFoundForCurrentUser() {
        // Given
        when(userContextService.getAuthorizedUser()).thenReturn(testUser);
        when(balanceRepository.findByUser(testUser)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> balanceService.getMyBalance())
                .isInstanceOf(BalanceNotFoundException.class)
                .hasMessageContaining("testuser");

        verify(balanceRepository).findByUser(testUser);
    }

    // GET USER BALANCE TESTS
    @Test
    @DisplayName("Should get user balance by ID")
    void shouldGetUserBalanceById() {
        // Given
        Long userId = 2L;
        User otherUser = User.builder().id(userId).username("otheruser").build();
        Balance otherBalance = Balance.builder()
                .user(otherUser)
                .amount(new BigDecimal("150.00"))
                .build();
        BalanceResponse userBalanceResponse = new BalanceResponse(userId, new BigDecimal("150.00"));

        when(userContextService.getUserById(userId)).thenReturn(otherUser);
        when(balanceRepository.findByUser(otherUser)).thenReturn(Optional.of(otherBalance));
        when(balanceMapper.toBalanceResponse(userId, new BigDecimal("150.00"))).thenReturn(userBalanceResponse);

        // When
        BalanceResponse result = balanceService.getUserBalance(userId);

        // Then
        assertThat(result).isEqualTo(userBalanceResponse);
        verify(userContextService).getUserById(userId);
        verify(balanceRepository).findByUser(otherUser);
    }

    @Test
    @DisplayName("Should throw exception when user not found by ID")
    void shouldThrowExceptionWhenUserNotFoundById() {
        // Given
        Long userId = 999L;
        when(userContextService.getUserById(userId))
                .thenThrow(new UserNotFoundException(userId));

        // When & Then
        assertThatThrownBy(() -> balanceService.getUserBalance(userId))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("999");

        verify(balanceRepository, never()).findByUser(any());
    }

    @Test
    @DisplayName("Should throw exception when balance not found for user by ID")
    void shouldThrowExceptionWhenBalanceNotFoundForUserById() {
        // Given
        Long userId = 2L;
        User otherUser = User.builder().id(userId).username("otheruser").build();
        
        when(userContextService.getUserById(userId)).thenReturn(otherUser);
        when(balanceRepository.findByUser(otherUser)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> balanceService.getUserBalance(userId))
                .isInstanceOf(BalanceNotFoundException.class)
                .hasMessageContaining("otheruser");

        verify(balanceRepository).findByUser(otherUser);
    }

    // DELETE BALANCE TESTS
    @Test
    @DisplayName("Should delete balance successfully")
    void shouldDeleteBalanceSuccessfully() {
        // Given
        when(userContextService.getAuthorizedUser()).thenReturn(testUser);
        when(balanceRepository.findByUser(testUser)).thenReturn(Optional.of(testBalance));
        doNothing().when(balanceRepository).delete(testBalance);

        // When
        balanceService.deleteBalance();

        // Then
        verify(userContextService).getAuthorizedUser();
        verify(balanceRepository).findByUser(testUser);
        verify(balanceRepository).delete(testBalance);
    }

    @Test
    @DisplayName("Should throw exception when deleting non-existent balance")
    void shouldThrowExceptionWhenDeletingNonExistentBalance() {
        // Given
        when(userContextService.getAuthorizedUser()).thenReturn(testUser);
        when(balanceRepository.findByUser(testUser)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> balanceService.deleteBalance())
                .isInstanceOf(BalanceNotFoundException.class)
                .hasMessageContaining("testuser");

        verify(balanceRepository, never()).delete(any());
    }

    // DEPOSIT TESTS
    @Test
    @DisplayName("Should deposit successfully")
    void shouldDepositSuccessfully() {
        // Given
        DepositRequest depositRequest = new DepositRequest(new BigDecimal("50.00"));

        BalanceTransaction transaction = BalanceTransaction.builder()
                .balance(testBalance)
                .type(OperationType.DEPOSIT)
                .amount(new BigDecimal("50.00"))
                .balanceBefore(new BigDecimal("100.00"))
                .balanceAfter(new BigDecimal("150.00"))
                .build();

        when(userContextService.getAuthorizedUser()).thenReturn(testUser);
        when(balanceRepository.findByUser(testUser)).thenReturn(Optional.of(testBalance));
        when(balanceRepository.save(testBalance)).thenReturn(testBalance);
        when(transactionRepository.save(any(BalanceTransaction.class))).thenReturn(transaction);
        when(balanceMapper.toBalanceOperationResponse(
                eq(new BigDecimal("150.00")),
                eq(1L),
                eq(new BigDecimal("50.00")),
                eq(OperationType.DEPOSIT)
        )).thenReturn(operationResponse);

        // When
        BalanceOperationResponse result = balanceService.deposit(depositRequest);

        // Then
        assertThat(result).isEqualTo(operationResponse);
        assertThat(testBalance.getAmount()).isEqualByComparingTo(new BigDecimal("150.00"));

        verify(balanceRepository).save(testBalance);
        verify(transactionRepository).save(any(BalanceTransaction.class));
    }

    @Test
    @DisplayName("Should throw exception when deposit to non-existent balance")
    void shouldThrowExceptionWhenDepositToNonExistentBalance() {
        // Given
        DepositRequest depositRequest = new DepositRequest(new BigDecimal("50.00"));
        
        when(userContextService.getAuthorizedUser()).thenReturn(testUser);
        when(balanceRepository.findByUser(testUser)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> balanceService.deposit(depositRequest))
                .isInstanceOf(BalanceNotFoundException.class)
                .hasMessageContaining("testuser");

        verify(balanceRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should create transaction record for deposit")
    void shouldCreateTransactionRecordForDeposit() {
        // Given
        DepositRequest depositRequest = new DepositRequest(new BigDecimal("75.25"));

        when(userContextService.getAuthorizedUser()).thenReturn(testUser);
        when(balanceRepository.findByUser(testUser)).thenReturn(Optional.of(testBalance));
        when(balanceRepository.save(testBalance)).thenReturn(testBalance);
        when(balanceMapper.toBalanceOperationResponse(any(), any(), any(), any())).thenReturn(operationResponse);

        // When
        balanceService.deposit(depositRequest);

        // Then
        ArgumentCaptor<BalanceTransaction> transactionCaptor = ArgumentCaptor.forClass(BalanceTransaction.class);
        verify(transactionRepository).save(transactionCaptor.capture());

        BalanceTransaction savedTransaction = transactionCaptor.getValue();
        assertThat(savedTransaction.getBalance()).isEqualTo(testBalance);
        assertThat(savedTransaction.getType()).isEqualTo(OperationType.DEPOSIT);
        assertThat(savedTransaction.getAmount()).isEqualByComparingTo(new BigDecimal("75.25"));
        assertThat(savedTransaction.getBalanceBefore()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(savedTransaction.getBalanceAfter()).isEqualByComparingTo(new BigDecimal("175.25"));
    }

    // WITHDRAWAL TESTS
    @Test
    @DisplayName("Should withdraw successfully")
    void shouldWithdrawSuccessfully() {
        // Given
        WithdrawRequest withdrawRequest = new WithdrawRequest(new BigDecimal("30.00"));
        
        BalanceOperationResponse withdrawResponse = new BalanceOperationResponse(
                new BigDecimal("70.00"),
                1L,
                new BigDecimal("30.00"),
                OperationType.WITHDRAWAL
        );

        when(userContextService.getAuthorizedUser()).thenReturn(testUser);
        when(balanceRepository.findByUser(testUser)).thenReturn(Optional.of(testBalance));
        when(balanceRepository.save(testBalance)).thenReturn(testBalance);
        when(balanceMapper.toBalanceOperationResponse(
                eq(new BigDecimal("70.00")),
                eq(1L),
                eq(new BigDecimal("30.00")),
                eq(OperationType.WITHDRAWAL)
        )).thenReturn(withdrawResponse);

        // When
        BalanceOperationResponse result = balanceService.withdraw(withdrawRequest);

        // Then
        assertThat(result).isEqualTo(withdrawResponse);
        assertThat(testBalance.getAmount()).isEqualByComparingTo(new BigDecimal("70.00"));
        
        ArgumentCaptor<BalanceTransaction> transactionCaptor = ArgumentCaptor.forClass(BalanceTransaction.class);
        verify(transactionRepository).save(transactionCaptor.capture());
        
        BalanceTransaction savedTransaction = transactionCaptor.getValue();
        assertThat(savedTransaction.getType()).isEqualTo(OperationType.WITHDRAWAL);
        assertThat(savedTransaction.getAmount()).isEqualByComparingTo(new BigDecimal("30.00"));
    }

    @Test
    @DisplayName("Should throw exception when withdrawing more than balance")
    void shouldThrowExceptionWhenWithdrawingMoreThanBalance() {
        // Given
        WithdrawRequest withdrawRequest = new WithdrawRequest(new BigDecimal("150.00"));

        when(userContextService.getAuthorizedUser()).thenReturn(testUser);
        when(balanceRepository.findByUser(testUser)).thenReturn(Optional.of(testBalance));

        // When & Then
        assertThatThrownBy(() -> balanceService.withdraw(withdrawRequest))
                .isInstanceOf(InsufficientFundsException.class)
                .hasMessageContaining("Current: $100.00")
                .hasMessageContaining("Requested: $150.00");

        verify(balanceRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when withdraw from non-existent balance")
    void shouldThrowExceptionWhenWithdrawFromNonExistentBalance() {
        // Given
        WithdrawRequest withdrawRequest = new WithdrawRequest(new BigDecimal("50.00"));
        
        when(userContextService.getAuthorizedUser()).thenReturn(testUser);
        when(balanceRepository.findByUser(testUser)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> balanceService.withdraw(withdrawRequest))
                .isInstanceOf(BalanceNotFoundException.class)
                .hasMessageContaining("testuser");

        verify(balanceRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when withdrawal amount is invalid")
    void shouldThrowExceptionWhenWithdrawalAmountIsInvalid() {
        // Given
        WithdrawRequest withdrawRequest = new WithdrawRequest(BigDecimal.ZERO);
        
        when(userContextService.getAuthorizedUser()).thenReturn(testUser);
        when(balanceRepository.findByUser(testUser)).thenReturn(Optional.of(testBalance));

        // When & Then
        assertThatThrownBy(() -> balanceService.withdraw(withdrawRequest))
                .isInstanceOf(InvalidAmountException.class)
                .hasMessageContaining("Withdrawal");

        verify(balanceRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should check if user can afford amount")
    void shouldCheckIfUserCanAffordAmount() {
        // Given
        when(userContextService.getAuthorizedUser()).thenReturn(testUser);
        when(balanceRepository.findByUser(testUser)).thenReturn(Optional.of(testBalance));

        // When & Then
        assertThat(balanceService.canAfford(new BigDecimal("50.00"))).isTrue();
        assertThat(balanceService.canAfford(new BigDecimal("100.00"))).isTrue();
        assertThat(balanceService.canAfford(new BigDecimal("100.01"))).isFalse();
        assertThat(balanceService.canAfford(new BigDecimal("150.00"))).isFalse();
    }

    @Test
    @DisplayName("Should throw when balance not found for afford check")
    void shouldThrowWhenBalanceNotFoundForAffordCheck() {
        // Given
        when(userContextService.getAuthorizedUser()).thenReturn(testUser);
        when(balanceRepository.findByUser(testUser)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> balanceService.canAfford(new BigDecimal("50.00")))
                .isInstanceOf(BalanceNotFoundException.class)
                .hasMessageContaining(testUser.getUsername());
    }


    @Test
    @DisplayName("Should create purchase transaction successfully")
    void shouldCreatePurchaseTransactionSuccessfully() {
        // Given
        BigDecimal purchaseAmount = new BigDecimal("25.00");
        
        BalanceTransaction expectedTransaction = BalanceTransaction.builder()
                .balance(testBalance)
                .type(OperationType.PURCHASE)
                .amount(purchaseAmount)
                .balanceBefore(new BigDecimal("100.00"))
                .balanceAfter(new BigDecimal("75.00"))
                .build();

        when(balanceRepository.findByUser(testUser)).thenReturn(Optional.of(testBalance));
        when(balanceRepository.save(testBalance)).thenReturn(testBalance);
        when(transactionRepository.save(any(BalanceTransaction.class))).thenReturn(expectedTransaction);

        // When
        BalanceTransaction result = balanceService.createPurchaseTransaction(purchaseAmount, testUser);

        // Then
        assertThat(result).isEqualTo(expectedTransaction);
        assertThat(testBalance.getAmount()).isEqualByComparingTo(new BigDecimal("75.00"));
        
        verify(balanceRepository).findByUser(testUser);
        verify(balanceRepository).save(testBalance);
        verify(transactionRepository).save(any(BalanceTransaction.class));
    }

    @Test
    @DisplayName("Should throw exception when insufficient funds for purchase")
    void shouldThrowExceptionWhenInsufficientFundsForPurchase() {
        // Given
        BigDecimal purchaseAmount = new BigDecimal("150.00");

        when(balanceRepository.findByUser(testUser)).thenReturn(Optional.of(testBalance));

        // When & Then
        assertThatThrownBy(() -> balanceService.createPurchaseTransaction(purchaseAmount, testUser))
                .isInstanceOf(InsufficientFundsException.class)
                .hasMessageContaining("Current: $100.00")
                .hasMessageContaining("Requested: $150.00");

        verify(balanceRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when balance not found for purchase")
    void shouldThrowExceptionWhenBalanceNotFoundForPurchase() {
        // Given
        BigDecimal purchaseAmount = new BigDecimal("50.00");
        
        when(balanceRepository.findByUser(testUser)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> balanceService.createPurchaseTransaction(purchaseAmount, testUser))
                .isInstanceOf(BalanceNotFoundException.class)
                .hasMessageContaining("testuser");

        verify(balanceRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }
}
