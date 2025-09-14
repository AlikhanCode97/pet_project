package com.example.Games.user.balance;

import com.example.Games.user.auth.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.List;

public interface BalanceRepository extends JpaRepository<Balance, Long> {

    Optional<Balance> findByUser(User user);
}
