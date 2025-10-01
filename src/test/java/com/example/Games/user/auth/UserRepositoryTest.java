package com.example.Games.user.auth;

import com.example.Games.config.TestJpaAuditingConfig;
import com.example.Games.user.balance.Balance;
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
@DisplayName("UserRepository Tests")
class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    private Role userRole;
    private User user;

    @BeforeEach
    void setUp() {
        userRole = Role.builder()
                .name(RoleType.USER)
                .build();
        userRole = entityManager.persistAndFlush(userRole);

        user = User.builder()
                .username("testuser")
                .email("test@example.com")
                .password("password123")
                .role(userRole)
                .build();
        entityManager.persistAndFlush(user);
    }

    @Test
    @DisplayName("Should load role lazily")
    void shouldLoadRoleLazily() {
        User found = userRepository.findById(user.getId()).orElseThrow();
        assertThat(found.getRole()).isNotNull();
        assertThat(found.getRole().getName()).isEqualTo(RoleType.USER);
    }

    @Test
    @DisplayName("Should update last login timestamp")
    void shouldUpdateLastLogin() {
        user.updateLastLogin();
        entityManager.persistAndFlush(user);

        User refreshed = entityManager.find(User.class, user.getId());
        assertThat(refreshed.getLastLoginAt()).isNotNull();
    }

    @Test
    @DisplayName("Should not allow null username")
    void shouldNotAllowNullUsername() {
        User invalid = User.builder()
                .email("invalid@example.com")
                .password("pass")
                .role(userRole)
                .build();

        assertThatThrownBy(() -> entityManager.persistAndFlush(invalid))
                .isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    @DisplayName("Should find user by username")
    void shouldFindUserByUsername() {

        // When
        Optional<User> found = userRepository.findByUsername("testuser");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo("testuser");
        assertThat(found.get().getEmail()).isEqualTo("test@example.com");
        assertThat(found.get().getRole()).isEqualTo(userRole);
    }

    @Test
    @DisplayName("Should return empty when username not found")
    void shouldReturnEmptyWhenUsernameNotFound() {
        // When
        Optional<User> found = userRepository.findByUsername("nonexistent");

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Should check if username exists")
    void shouldCheckIfUsernameExists() {

        // When & Then
        assertThat(userRepository.existsByUsername("testuser")).isTrue();
        assertThat(userRepository.existsByUsername("nonexistent")).isFalse();
    }

    @Test
    @DisplayName("Should check if email exists")
    void shouldCheckIfEmailExists() {
        // When & Then
        assertThat(userRepository.existsByEmail("test@example.com")).isTrue();
        assertThat(userRepository.existsByEmail("nonexistent@example.com")).isFalse();
    }

    @Test
    @DisplayName("Should set audit fields on save and update")
    void shouldSetAuditFieldsOnSaveAndUpdate() {

        // Then
        assertThat(user.getCreatedAt()).isNotNull();
        assertThat(user.getUpdatedAt()).isNotNull();

        // Update
        user.setPassword("newPassword");
        User updated = userRepository.save(user);
        entityManager.flush();
        entityManager.refresh(updated);

        assertThat(updated.getUpdatedAt()).isAfterOrEqualTo(updated.getCreatedAt());
    }

    @Test
    @DisplayName("Should cascade persist balance when saving user")
    void shouldCascadePersistBalance() {
        Balance balance = Balance.builder()
                .amount(BigDecimal.valueOf(100))
                .build();

        User newUser = User.builder()
                .username("withbalance")
                .email("withbalance@example.com")
                .password("secret")
                .role(userRole)
                .balance(balance)
                .build();

        // 🔑 Set the back-reference
        balance.setUser(newUser);

        User saved = userRepository.save(newUser);
        entityManager.flush();
        entityManager.refresh(saved);

        assertThat(saved.getBalance()).isNotNull();
        assertThat(saved.getBalance().getAmount()).isEqualTo("100.00");
        assertThat(saved.getBalance().getUser()).isEqualTo(saved);
    }


    @Test
    @DisplayName("Should not allow duplicate username")
    void shouldNotAllowDuplicateUsername() {
        User duplicate = User.builder()
                .username("testuser") // already exists
                .email("other@example.com")
                .password("password")
                .role(userRole)
                .build();

        assertThatThrownBy(() -> userRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);

    }

    @Test
    @DisplayName("Should not allow duplicate email")
    void shouldNotAllowDuplicateEmail() {
        User duplicate = User.builder()
                .username("anotheruser")
                .email("test@example.com") // already exists
                .password("password")
                .role(userRole)
                .build();

        assertThatThrownBy(() -> userRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);

    }

}
