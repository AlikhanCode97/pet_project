package com.example.Games.user.balance;

import com.example.Games.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Balance {

    @Id
    private Long id;

    @OneToOne
    @MapsId // share PK with User
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private BigDecimal amount;

    public void deposit(BigDecimal value) {
        if (value.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Deposit must be positive.");
        this.amount = this.amount.add(value);
    }

    public void withdraw(BigDecimal value) {
        if (value.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Withdraw must be positive.");
        if (this.amount.compareTo(value) < 0)
            throw new IllegalStateException("Insufficient balance");
        this.amount = this.amount.subtract(value);
    }

    @PrePersist
    public void onCreate() {
        if (this.amount == null) this.amount = BigDecimal.ZERO;
    }
}

