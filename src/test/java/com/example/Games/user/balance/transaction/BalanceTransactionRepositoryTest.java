package com.example.Games.user.balance.transaction;

import com.example.Games.config.TestJpaAuditingConfig;
import com.example.Games.user.auth.User;
import com.example.Games.user.balance.Balance;
import com.example.Games.user.role.Role;
import com.example.Games.user.role.RoleType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@Import(TestJpaAuditingConfig.class)
@ActiveProfiles("test")
@DisplayName("BalanceTransactionRepository Tests")
class BalanceTransactionRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private BalanceTransactionRepository transactionRepository;

    private Balance testBalance;
    private Balance otherBalance;

    @BeforeEach
    void setUp() {
        // Setup roles and users
        Role userRole = Role.builder()
                .name(RoleType.USER)
                .build();
        userRole = entityManager.persistAndFlush(userRole);

        User testUser = User.builder()
                .username("testuser")
                .email("test@example.com")
                .password("password123")
                .role(userRole)
                .build();
        testUser = entityManager.persistAndFlush(testUser);

        User otherUser = User.builder()
                .username("otheruser")
                .email("other@example.com")
                .password("password123")
                .role(userRole)
                .build();
        otherUser = entityManager.persistAndFlush(otherUser);

        testBalance = Balance.builder()
                .user(testUser)
                .amount(new BigDecimal("100.00"))
                .build();
        testBalance = entityManager.persistAndFlush(testBalance);

        otherBalance = Balance.builder()
                .user(otherUser)
                .amount(new BigDecimal("200.00"))
                .build();
        otherBalance = entityManager.persistAndFlush(otherBalance);
    }
    @Test
    @DisplayName("Should not allow transaction without balance")
    void shouldNotAllowTransactionWithoutBalance() {
        BalanceTransaction invalid = BalanceTransaction.builder()
                .type(OperationType.DEPOSIT)
                .amount(new BigDecimal("10.00"))
                .balanceBefore(new BigDecimal("100.00"))
                .balanceAfter(new BigDecimal("110.00"))
                .build();

        assertThatThrownBy(() -> transactionRepository.saveAndFlush(invalid))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Should load balance lazily")
    void shouldLoadBalanceLazily() {
        BalanceTransaction tx = BalanceTransaction.create(
                testBalance,
                OperationType.DEPOSIT,
                new BigDecimal("10.00"),
                new BigDecimal("100.00"),
                new BigDecimal("110.00")
        );
        transactionRepository.saveAndFlush(tx);

        BalanceTransaction found = transactionRepository.findAllByBalanceOrderByTimestampDesc(testBalance).get(0);
        assertThat(found.getBalance().getId()).isEqualTo(testBalance.getId());
    }

    @Test
    @DisplayName("Should fail when transaction amount exceeds column precision")
    void shouldFailWhenTransactionAmountExceedsPrecision() {
        BalanceTransaction tooBigTransaction = BalanceTransaction.create(
                testBalance,
                OperationType.DEPOSIT,
                new BigDecimal("100000000.00"), // exceeds NUMERIC(10,2)
                new BigDecimal("100.00"),
                new BigDecimal("100000100.00")
        );

        assertThatThrownBy(() -> transactionRepository.saveAndFlush(tooBigTransaction))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("Value too long for column"); // check DB message
    }


    @Test
    @DisplayName("Should delete transactions when balance is deleted (cascade + orphanRemoval)")
    void shouldDeleteTransactionsWhenBalanceDeleted() {
        BalanceTransaction transaction = BalanceTransaction.create(
                testBalance,
                OperationType.DEPOSIT,
                new BigDecimal("50.00"),
                new BigDecimal("100.00"),
                new BigDecimal("150.00")
        );

        testBalance.getTransactions().add(transaction); // attach transaction to balance
        entityManager.persistAndFlush(testBalance);

        assertThat(transactionRepository.findAllByBalanceOrderByTimestampDesc(testBalance))
                .hasSize(1);

        entityManager.remove(testBalance);
        entityManager.flush();

        assertThat(transactionRepository.findAllByBalanceOrderByTimestampDesc(testBalance))
                .isEmpty();
    }

}