package com.example.Games.user.balance;

import com.example.Games.config.exception.balance.InvalidAmountException;
import com.example.Games.config.exception.balance.InsufficientFundsException;
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
import java.util.ArrayList;
import java.util.List;
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

    @OneToMany(
            fetch = FetchType.LAZY,
            mappedBy = "balance",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @Builder.Default
    private List<BalanceTransaction> transactions = new ArrayList<>();


    public void deposit(BigDecimal value) {
        validatePositiveValue(value, "Deposit");
        this.amount = this.amount.add(value).setScale(2, RoundingMode.HALF_UP);
    }

    public void withdraw(BigDecimal value) {
        validatePositiveValue(value, "Withdrawal");
        validateSufficientFunds(value);
        this.amount = this.amount.subtract(value).setScale(2, RoundingMode.HALF_UP);
    }

    public BalanceTransaction createTransaction(OperationType type, BigDecimal amount, BigDecimal balanceBefore) {
        return BalanceTransaction.create(this, type, amount, balanceBefore, this.amount);
    }

    public void validateSufficientFunds(BigDecimal amount) {
        if (this.amount.compareTo(amount) < 0) {
            throw new InsufficientFundsException(
                    String.format("Insufficient funds. Current: $%.2f, Requested: $%.2f",
                            this.amount, amount)
            );
        }
    }
    public boolean hasSufficientFunds(BigDecimal amount) {
        return this.amount.compareTo(amount) >= 0;
    }

    private void validatePositiveValue(BigDecimal amount, String operation) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException(operation);
        }
    }

    @PrePersist
    public void onCreate() {
        this.amount = Objects.requireNonNullElse(this.amount, BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }
}

