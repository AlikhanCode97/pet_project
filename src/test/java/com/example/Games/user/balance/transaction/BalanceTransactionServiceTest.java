package com.example.Games.user.balance.transaction;

import com.example.Games.config.common.service.UserContextService;
import com.example.Games.config.exception.auth.UserNotFoundException;
import com.example.Games.config.exception.balance.BalanceNotFoundException;
import com.example.Games.user.auth.User;
import com.example.Games.user.balance.Balance;
import com.example.Games.user.balance.BalanceRepository;
import com.example.Games.user.balance.transaction.dto.BalanceTransactionDTO;
import com.example.Games.user.role.Role;
import com.example.Games.user.role.RoleType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BalanceTransactionService Tests")
class BalanceTransactionServiceTest {

    @Mock
    private BalanceTransactionRepository transactionRepository;

    @Mock
    private BalanceRepository balanceRepository;

    @Mock
    private UserContextService userContextService;

    @Mock
    private BalanceTransactionMapper transactionMapper;

    @InjectMocks
    private BalanceTransactionService transactionService;

    private User testUser;
    private User otherUser;
    private Balance testBalance;
    private Balance otherBalance;
    private List<BalanceTransaction> sampleTransactions;
    private List<BalanceTransactionDTO> sampleTransactionDTOs;

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
                .password("password123")
                .role(userRole)
                .build();

        otherUser = User.builder()
                .id(2L)
                .username("otheruser")
                .email("other@example.com")
                .password("password123")
                .role(userRole)
                .build();

        testBalance = Balance.builder()
                .id(1L)
                .user(testUser)
                .amount(new BigDecimal("150.00"))
                .build();

        otherBalance = Balance.builder()
                .id(2L)
                .user(otherUser)
                .amount(new BigDecimal("200.00"))
                .build();

        LocalDateTime now = LocalDateTime.now();
        
        BalanceTransaction deposit = BalanceTransaction.builder()
                .id(1L)
                .balance(testBalance)
                .type(OperationType.DEPOSIT)
                .amount(new BigDecimal("100.00"))
                .balanceBefore(new BigDecimal("0.00"))
                .balanceAfter(new BigDecimal("100.00"))
                .timestamp(now.minusHours(3))
                .build();
                
        BalanceTransaction withdrawal = BalanceTransaction.builder()
                .id(2L)
                .balance(testBalance)
                .type(OperationType.WITHDRAWAL)
                .amount(new BigDecimal("30.00"))
                .balanceBefore(new BigDecimal("100.00"))
                .balanceAfter(new BigDecimal("70.00"))
                .timestamp(now.minusHours(2))
                .build();
                
        BalanceTransaction purchase = BalanceTransaction.builder()
                .id(3L)
                .balance(testBalance)
                .type(OperationType.PURCHASE)
                .amount(new BigDecimal("19.99"))
                .balanceBefore(new BigDecimal("70.00"))
                .balanceAfter(new BigDecimal("50.01"))
                .timestamp(now.minusHours(1))
                .build();

        sampleTransactions = Arrays.asList(purchase, withdrawal, deposit);
        
        sampleTransactionDTOs = Arrays.asList(
                new BalanceTransactionDTO(3L, OperationType.PURCHASE, 
                        new BigDecimal("19.99"), new BigDecimal("70.00"), 
                        new BigDecimal("50.01"), purchase.getTimestamp()),
                new BalanceTransactionDTO(2L, OperationType.WITHDRAWAL, 
                        new BigDecimal("30.00"), new BigDecimal("100.00"), 
                        new BigDecimal("70.00"), withdrawal.getTimestamp()),
                new BalanceTransactionDTO(1L, OperationType.DEPOSIT, 
                        new BigDecimal("100.00"), new BigDecimal("0.00"), 
                        new BigDecimal("100.00"), deposit.getTimestamp())
        );
    }

    @Test
    @DisplayName("Should get my transactions successfully")
    void shouldGetMyTransactionsSuccessfully() {
        // Given
        when(userContextService.getAuthorizedUser()).thenReturn(testUser);
        when(balanceRepository.findByUser(testUser)).thenReturn(Optional.of(testBalance));
        when(transactionRepository.findAllByBalanceOrderByTimestampDesc(testBalance))
                .thenReturn(sampleTransactions);
        when(transactionMapper.toDtoList(sampleTransactions)).thenReturn(sampleTransactionDTOs);

        // When
        List<BalanceTransactionDTO> result = transactionService.getMyTransactions();

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(3);
        assertThat(result).isEqualTo(sampleTransactionDTOs);
        
        // Verify order (newest first)
        assertThat(result.get(0).type()).isEqualTo(OperationType.PURCHASE);
        assertThat(result.get(1).type()).isEqualTo(OperationType.WITHDRAWAL);
        assertThat(result.get(2).type()).isEqualTo(OperationType.DEPOSIT);
        
        verify(userContextService).getAuthorizedUser();
        verify(balanceRepository).findByUser(testUser);
        verify(transactionRepository).findAllByBalanceOrderByTimestampDesc(testBalance);
        verify(transactionMapper).toDtoList(sampleTransactions);
    }

    @Test
    @DisplayName("Should return empty list when no transactions exist")
    void shouldReturnEmptyListWhenNoTransactionsExist() {
        // Given
        when(userContextService.getAuthorizedUser()).thenReturn(testUser);
        when(balanceRepository.findByUser(testUser)).thenReturn(Optional.of(testBalance));
        when(transactionRepository.findAllByBalanceOrderByTimestampDesc(testBalance))
                .thenReturn(Collections.emptyList());
        when(transactionMapper.toDtoList(Collections.emptyList())).thenReturn(Collections.emptyList());

        // When
        List<BalanceTransactionDTO> result = transactionService.getMyTransactions();

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
        
        verify(transactionRepository).findAllByBalanceOrderByTimestampDesc(testBalance);
        verify(transactionMapper).toDtoList(Collections.emptyList());
    }

    @Test
    @DisplayName("Should throw exception when balance not found for current user")
    void shouldThrowExceptionWhenBalanceNotFoundForCurrentUser() {
        // Given
        when(userContextService.getAuthorizedUser()).thenReturn(testUser);
        when(balanceRepository.findByUser(testUser)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> transactionService.getMyTransactions())
                .isInstanceOf(BalanceNotFoundException.class)
                .hasMessageContaining("testuser");

        verify(transactionRepository, never()).findAllByBalanceOrderByTimestampDesc(any());
        verify(transactionMapper, never()).toDtoList(any());
    }

    @Test
    @DisplayName("Should get user transactions by ID successfully")
    void shouldGetUserTransactionsByIdSuccessfully() {
        // Given
        Long userId = 2L;
        
        BalanceTransaction otherTransaction = BalanceTransaction.builder()
                .id(4L)
                .balance(otherBalance)
                .type(OperationType.ADMIN_DEPOSIT)
                .amount(new BigDecimal("500.00"))
                .balanceBefore(new BigDecimal("200.00"))
                .balanceAfter(new BigDecimal("700.00"))
                .timestamp(LocalDateTime.now())
                .build();
                
        List<BalanceTransaction> otherTransactions = Collections.singletonList(otherTransaction);
        
        BalanceTransactionDTO otherTransactionDTO = new BalanceTransactionDTO(
                4L, OperationType.ADMIN_DEPOSIT,
                new BigDecimal("500.00"), new BigDecimal("200.00"),
                new BigDecimal("700.00"), otherTransaction.getTimestamp()
        );
        
        List<BalanceTransactionDTO> otherTransactionDTOs = Collections.singletonList(otherTransactionDTO);

        when(userContextService.getUserById(userId)).thenReturn(otherUser);
        when(balanceRepository.findByUser(otherUser)).thenReturn(Optional.of(otherBalance));
        when(transactionRepository.findAllByBalanceOrderByTimestampDesc(otherBalance))
                .thenReturn(otherTransactions);
        when(transactionMapper.toDtoList(otherTransactions)).thenReturn(otherTransactionDTOs);

        // When
        List<BalanceTransactionDTO> result = transactionService.getUserTransactions(userId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).type()).isEqualTo(OperationType.ADMIN_DEPOSIT);
        assertThat(result.get(0).amount()).isEqualByComparingTo(new BigDecimal("500.00"));
        
        verify(userContextService).getUserById(userId);
        verify(balanceRepository).findByUser(otherUser);
        verify(transactionRepository).findAllByBalanceOrderByTimestampDesc(otherBalance);
        verify(transactionMapper).toDtoList(otherTransactions);
    }

    @Test
    @DisplayName("Should throw exception when user not found by ID")
    void shouldThrowExceptionWhenUserNotFoundById() {
        // Given
        Long userId = 999L;
        when(userContextService.getUserById(userId))
                .thenThrow(new UserNotFoundException(userId));

        // When & Then
        assertThatThrownBy(() -> transactionService.getUserTransactions(userId))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("999");

        verify(balanceRepository, never()).findByUser(any());
        verify(transactionRepository, never()).findAllByBalanceOrderByTimestampDesc(any());
    }

    @Test
    @DisplayName("Should throw exception when balance not found for user by ID")
    void shouldThrowExceptionWhenBalanceNotFoundForUserById() {
        // Given
        Long userId = 2L;
        when(userContextService.getUserById(userId)).thenReturn(otherUser);
        when(balanceRepository.findByUser(otherUser)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> transactionService.getUserTransactions(userId))
                .isInstanceOf(BalanceNotFoundException.class)
                .hasMessageContaining("otheruser");

        verify(transactionRepository, never()).findAllByBalanceOrderByTimestampDesc(any());
        verify(transactionMapper, never()).toDtoList(any());
    }
}
