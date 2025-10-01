package com.example.Games.user.balance;

import com.example.Games.config.TestJpaAuditingConfig;
import com.example.Games.user.auth.User;
import com.example.Games.user.role.Role;
import com.example.Games.user.role.RoleRepository;
import com.example.Games.user.role.RoleType;
import org.hibernate.exception.ConstraintViolationException;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@Import(TestJpaAuditingConfig.class)
@ActiveProfiles("test")
@DisplayName("BalanceRepository Tests")
class BalanceRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private BalanceRepository balanceRepository;

    @Autowired
    private RoleRepository roleRepository;

    private Role userRole;
    private User testUser;

    @BeforeEach
    void setUp() {
        userRole = Role.builder()
                .name(RoleType.USER)
                .build();
        userRole = entityManager.persistAndFlush(userRole);

        testUser = User.builder()
                .username("testuser")
                .email("test@example.com")
                .password("password123")
                .role(userRole)
                .build();
        testUser = entityManager.persistAndFlush(testUser);
    }

    @Test
    @DisplayName("Should default amount to zero when not provided")
    void shouldDefaultAmountToZeroWhenNotProvided() {
        Balance balance = Balance.builder()
                .user(testUser)
                .build(); // no amount set

        Balance saved = balanceRepository.saveAndFlush(balance);
        assertThat(saved.getAmount()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("Should not allow balance without user")
    void shouldNotAllowBalanceWithoutUser() {
        Balance invalid = Balance.builder()
                .amount(new BigDecimal("10.00"))
                .build();

        assertThatThrownBy(() -> balanceRepository.saveAndFlush(invalid))
                .isInstanceOf(DataIntegrityViolationException.class);
    }


    @Test
    @DisplayName("Should find balance by user")
    void shouldFindBalanceByUser() {
        // Given
        Balance balance = Balance.builder()
                .user(testUser)
                .amount(new BigDecimal("100.50"))
                .build();
        entityManager.persistAndFlush(balance);

        // When
        Optional<Balance> found = balanceRepository.findByUser(testUser);

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getUser()).isEqualTo(testUser);
        assertThat(found.get().getAmount()).isEqualByComparingTo(new BigDecimal("100.50"));
    }

    @Test
    @DisplayName("Should return empty when user has no balance")
    void shouldReturnEmptyWhenUserHasNoBalance() {
        // Given
        User userWithoutBalance = User.builder()
                .username("nobalanceuser")
                .email("nobalance@example.com")
                .password("password123")
                .role(userRole)
                .build();
        userWithoutBalance = entityManager.persistAndFlush(userWithoutBalance);

        // When
        Optional<Balance> found = balanceRepository.findByUser(userWithoutBalance);

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Should handle maximum allowed amounts correctly")
    void shouldHandleMaximumAllowedAmountsCorrectly() {
        Balance balance = Balance.builder()
                .user(testUser)
                .amount(new BigDecimal("99999999.99")) // max valid
                .build();

        entityManager.persistAndFlush(balance);

        Optional<Balance> found = balanceRepository.findByUser(testUser);

        assertThat(found).isPresent();
        assertThat(found.get().getAmount()).isEqualByComparingTo("99999999.99");
    }


    @Test
    @DisplayName("Should fail when amount exceeds column precision")
    void shouldFailWhenAmountExceedsPrecision() {
        Balance balance = Balance.builder()
                .user(testUser)
                .amount(new BigDecimal("100000000.00"))
                .build();

        assertThatThrownBy(() -> {
            balanceRepository.saveAndFlush(balance);
        }).isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("Value too long for column");
    }

    @Test
    @DisplayName("Should not allow multiple balances for the same user")
    void shouldNotAllowMultipleBalancesForSameUser() {
        Balance balance1 = Balance.builder()
                .user(testUser)
                .amount(new BigDecimal("100.00"))
                .build();
        entityManager.persistAndFlush(balance1);

        Balance balance2 = Balance.builder()
                .user(testUser)
                .amount(new BigDecimal("200.00"))
                .build();

        assertThatThrownBy(() -> {
            entityManager.persistAndFlush(balance2);
        }).isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    @DisplayName("Should delete balance when user is deleted (cascade + orphanRemoval)")
    void shouldDeleteBalanceWhenUserDeleted() {
        Balance balance = Balance.builder()
                .user(testUser)
                .amount(new BigDecimal("100.00"))
                .build();
        balance.setUser(testUser);
        testUser.setBalance(balance);

        entityManager.persistAndFlush(testUser); // cascade persists balance

        assertThat(balanceRepository.findByUser(testUser)).isPresent();

        entityManager.remove(testUser);
        entityManager.flush();

        assertThat(balanceRepository.findById(balance.getId())).isEmpty();
    }

    @Test
    @DisplayName("Should set audit fields on insert and update")
    void shouldSetAuditFieldsOnInsertAndUpdate() throws InterruptedException {
        Balance balance = Balance.builder()
                .user(testUser)
                .amount(new BigDecimal("100.00"))
                .build();

        Balance saved = balanceRepository.save(balance);
        entityManager.flush();
        entityManager.refresh(saved);

        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getCreatedAt()).isBeforeOrEqualTo(saved.getUpdatedAt());

        var originalCreatedAt = saved.getCreatedAt();
        var originalUpdatedAt = saved.getUpdatedAt();


        Thread.sleep(10);
        saved.setAmount(new BigDecimal("200.00"));
        Balance updated = balanceRepository.save(saved);
        entityManager.flush();
        entityManager.refresh(updated);

        assertThat(updated.getCreatedAt()).isEqualTo(originalCreatedAt);
        assertThat(updated.getUpdatedAt()).isAfter(originalUpdatedAt);
        assertThat(updated.getAmount()).isEqualByComparingTo("200.00");
    }
}
