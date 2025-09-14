package com.example.Games.user.balance;

import com.example.Games.config.exception.purchase.InsufficientFundsException;
import com.example.Games.user.auth.User;
import com.example.Games.user.balance.transaction.BalanceTransaction;
import com.example.Games.user.balance.transaction.OperationType;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
@Table(name = "balances")
public class Balance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public void deposit(BigDecimal value) {
        validatePositiveAmount(value, "Deposit");
        this.amount = this.amount.add(value).setScale(2, RoundingMode.HALF_UP);
    }

    public void withdraw(BigDecimal value) {
        validatePositiveAmount(value, "Withdrawal");
        if (this.amount.compareTo(value) < 0) {
            throw new InsufficientFundsException(
                String.format("Insufficient balance. Current: $%.2f, Requested: $%.2f",
                    this.amount, value)
            );
        }
        this.amount = this.amount.subtract(value).setScale(2, RoundingMode.HALF_UP);
    }

    public BalanceTransaction createTransaction(OperationType type, BigDecimal amount, BigDecimal balanceBefore) {
        return BalanceTransaction.create(this, type, amount, balanceBefore, this.amount);
    }

    public boolean hasSufficientFunds(BigDecimal amount) {
        return this.amount.compareTo(amount) >= 0;
    }

    private void validatePositiveAmount(BigDecimal amount, String operation) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(operation + " amount must be positive");
        }
    }

    @PrePersist
    public void onCreate() {
        this.amount = Objects.requireNonNullElse(this.amount, BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }
}

